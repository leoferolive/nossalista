package br.com.leoferolive.nossalista.list.domain;

import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.support.AbstractPostgresIT;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes de concorrência para o lock otimista ({@code @Version}) de {@link SharedList}.
 *
 * <p>Mesmo padrão de {@code ListItemOptimisticLockingTest}: dois "usuários" carregam a
 * mesma lista em transações/persistence contexts isolados, ambos tentam salvar sua
 * alteração — a segunda escrita, com versão desatualizada, deve falhar com
 * {@link ObjectOptimisticLockingFailureException}, provando a detecção de lost update.</p>
 *
 * <p>Sem o {@code @Version} em {@link SharedList}, a segunda escrita venceria
 * silenciosamente e este teste falharia (nenhuma exceção seria lançada).</p>
 *
 * <p><b>Nota:</b> introduzido sobre H2 na Onda 1 (ver
 * {@code docs/plans/onda1-blindagem-core/T1-lock-otimista.md}); fortalecido contra
 * PostgreSQL real (Testcontainers, {@link AbstractPostgresIT}) na Onda 2 — ver
 * {@code docs/plans/onda2-honestidade-metrica/T1-testcontainers.md}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SharedListOptimisticLockingTest extends AbstractPostgresIT {

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private UUID userId;
    private UUID listId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            User user = new User();
            user.setUsername("lock_list_user_" + System.nanoTime());
            user.setEmail("lock_list_" + System.nanoTime() + "@example.com");
            user.setAuthProvider(AuthProvider.EMAIL);
            user.setPassword("$2a$10$dummyHashForTesting");
            user.setRole(Role.USER);
            User savedUser = userRepository.saveAndFlush(user);
            userId = savedUser.getId();

            SharedList list = new SharedList();
            list.setName("Lista original");
            list.setTypeId(1);
            list.setOwner(savedUser);
            SharedList savedList = listRepository.saveAndFlush(list);
            listId = savedList.getId();
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            listRepository.deleteById(listId);
            userRepository.deleteById(userId);
        });
    }

    @Test
    void concurrentUpdatesToSameListMustDetectLostUpdate() {
        // Given: dois "usuários" carregam a mesma lista, cada um em sua própria transação —
        // simulando duas requisições HTTP concorrentes que leem o mesmo estado inicial.
        SharedList listLoadedByUserA = transactionTemplate.execute(status ->
            listRepository.findById(listId).orElseThrow());
        SharedList listLoadedByUserB = transactionTemplate.execute(status ->
            listRepository.findById(listId).orElseThrow());

        assertEquals(listLoadedByUserA.getVersion(), listLoadedByUserB.getVersion(),
            "Ambas as leituras devem partir da mesma versão");

        // When: usuário A salva sua alteração primeiro — sucesso, a versão avança no banco.
        listLoadedByUserA.setName("Renomeada por A");
        transactionTemplate.executeWithoutResult(status ->
            listRepository.saveAndFlush(listLoadedByUserA));

        // Then: usuário B tenta salvar com a versão desatualizada (stale) — deve falhar,
        // provando que o lock otimista detecta a edição concorrente em vez de sobrescrever
        // silenciosamente a alteração de A.
        listLoadedByUserB.setName("Renomeada por B");
        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
            transactionTemplate.executeWithoutResult(status ->
                listRepository.saveAndFlush(listLoadedByUserB)));
    }
}
