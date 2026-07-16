package br.com.leoferolive.nossalista.notification;

import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.listitem.dto.CreateItemRequestDTO;
import br.com.leoferolive.nossalista.listitem.service.ListItemService;
import br.com.leoferolive.nossalista.support.RegressionTest;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Prova, com Spring real (transação e evento reais — sem simular commit/rollback
 * com mocks), o contrato mais sensível da T3: notificação de item só é
 * disparada APÓS o commit da transação e sempre FORA da thread da requisição.
 *
 * <p>Deliberadamente NÃO usa {@code @Transactional} de classe de teste: essa
 * anotação nunca comita de verdade (Spring sempre reverte ao final do
 * método), o que tornaria "dispara após commit" um falso-positivo — o
 * listener {@code AFTER_COMMIT} nunca dispararia e o teste passaria por
 * motivo errado. Em vez disso, cada teste controla explicitamente commit ou
 * rollback via {@link TransactionTemplate}.
 *
 * <p>Como os commits são reais, os dados de cada teste são explicitamente
 * removidos em {@link #tearDown()} — o H2 em memória usado nos testes é
 * compartilhado (mesma URL nomeada) pela suíte inteira dentro da mesma JVM
 * (Surefire roda com {@code reuseForks=true}), e outras classes (ex.:
 * {@code ListItemRepositoryTest}) fazem asserções de contagem absoluta que
 * quebrariam se dados comitados aqui vazassem para elas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@RegressionTest
class ListItemNotificationAfterCommitIntegrationTest {

    @Autowired
    private ListItemService listItemService;

    @Autowired
    private ListService listService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private NotificationService notificationService;

    private TransactionTemplate transactionTemplate;
    private User owner;
    private SharedList list;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        // Setup roda em transação própria, comitada de verdade, para que os
        // dados existam nas transações independentes controladas por cada teste.
        transactionTemplate.executeWithoutResult(status -> {
            String unique = UUID.randomUUID().toString();
            owner = userService.createUser(
                    "notif-owner-" + unique,
                    "notif-owner-" + unique + "@example.com",
                    "hashedPassword",
                    "Notif Owner",
                    AuthProvider.EMAIL
            );
            list = listService.createList(new CreateListRequest("Lista Notificação T3", 1), owner);
        });
    }

    @AfterEach
    void tearDown() {
        // Deletar o owner comita e faz CASCADE (lists -> list_items/list_members/
        // activity_logs, ver V2/V3/V4/V6__*.sql), removendo tudo que este teste
        // comitou e mantendo o H2 compartilhado limpo para o resto da suíte.
        transactionTemplate.executeWithoutResult(status -> userRepository.deleteById(owner.getId()));
    }

    @Test
    @DisplayName("Não notifica quando a transação de escrita sofre rollback")
    void doesNotNotifyOnRollback() {
        CreateItemRequestDTO dto = new CreateItemRequestDTO("Item que será desfeito", null, null, null, null);

        transactionTemplate.executeWithoutResult(status -> {
            listItemService.addItem(list.getId(), dto, owner);
            status.setRollbackOnly();
        });

        // after(): espera a janela inteira e SÓ ENTÃO confirma ausência de
        // interação — diferente de verify(..., never()) isolado, que poderia
        // passar mesmo que o listener ainda fosse disparar um instante depois.
        verify(notificationService, after(500).never())
                .notifyListMembers(eq(list.getId()), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Notifica após o commit, de forma assíncrona e fora da thread da requisição")
    void notifiesAfterCommitAsynchronously() {
        CreateItemRequestDTO dto = new CreateItemRequestDTO("Item confirmado", null, null, null, null);
        AtomicReference<String> notifyingThreadName = new AtomicReference<>();
        AtomicReference<String> callingThreadName = new AtomicReference<>();

        doAnswer(invocation -> {
            notifyingThreadName.set(Thread.currentThread().getName());
            return null;
        }).when(notificationService).notifyListMembers(any(), any(), any(), any(), any());

        transactionTemplate.executeWithoutResult(status -> {
            callingThreadName.set(Thread.currentThread().getName());
            listItemService.addItem(list.getId(), dto, owner);
        });

        // timeout(): tolera a natureza assíncrona — espera até 2s pelo
        // listener rodar no executor dedicado.
        verify(notificationService, timeout(2000))
                .notifyListMembers(eq(list.getId()), eq(owner.getId()), eq("ITEM_ADDED"), any(), eq(owner));

        assertThat(notifyingThreadName.get())
                .as("a notificação deve rodar fora da thread da requisição/transação, no executor dedicado")
                .isNotNull()
                .isNotEqualTo(callingThreadName.get())
                .startsWith("async-notif-");
    }
}
