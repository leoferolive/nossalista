package br.com.leoferolive.nossalista.listitem.domain;

import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
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
 * Testes de concorrência para o lock otimista ({@code @Version}) de {@link ListItem}.
 *
 * <p>Cada "usuário" carrega o item numa transação própria (persistence context isolado,
 * via {@link TransactionTemplate}), simulando duas requisições HTTP concorrentes que leem
 * o mesmo estado. Em seguida, ambos tentam salvar sua alteração em transações separadas.
 * A primeira escrita avança a versão no banco; a segunda, ainda com a versão antiga (stale),
 * deve falhar com {@link ObjectOptimisticLockingFailureException} — provando que o lock
 * otimista impede a sobrescrita silenciosa (lost update) da alteração alheia.</p>
 *
 * <p>Sem o {@code @Version} em {@link ListItem}, a segunda escrita venceria silenciosamente
 * e este teste falharia (nenhuma exceção seria lançada).</p>
 *
 * <p><b>Nota:</b> roda sobre H2 nesta onda (ver
 * {@code docs/plans/onda1-blindagem-core/T1-lock-otimista.md}). É indicativo; será
 * fortalecido contra PostgreSQL real (Testcontainers) na Onda 2.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ListItemOptimisticLockingTest {

    @Autowired
    private ListItemRepository listItemRepository;

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private UUID userId;
    private UUID listId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            User user = new User();
            user.setUsername("lock_item_user_" + System.nanoTime());
            user.setEmail("lock_item_" + System.nanoTime() + "@example.com");
            user.setAuthProvider(AuthProvider.EMAIL);
            user.setPassword("$2a$10$dummyHashForTesting");
            user.setRole(Role.USER);
            User savedUser = userRepository.saveAndFlush(user);
            userId = savedUser.getId();

            SharedList list = new SharedList();
            list.setName("Lista para teste de lock de item");
            list.setTypeId(1);
            list.setOwner(savedUser);
            SharedList savedList = listRepository.saveAndFlush(list);
            listId = savedList.getId();

            ListItem item = new ListItem();
            item.setName("Item original");
            item.setList(savedList);
            item.setCreatedBy(savedUser);
            ListItem savedItem = listItemRepository.saveAndFlush(item);
            itemId = savedItem.getId();
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            listItemRepository.deleteById(itemId);
            listRepository.deleteById(listId);
            userRepository.deleteById(userId);
        });
    }

    @Test
    void concurrentUpdatesToSameItemMustDetectLostUpdate() {
        // Given: dois "usuários" carregam o mesmo item, cada um em sua própria transação —
        // simulando duas requisições HTTP concorrentes que leem o mesmo estado inicial.
        ListItem itemLoadedByUserA = transactionTemplate.execute(status ->
            listItemRepository.findById(itemId).orElseThrow());
        ListItem itemLoadedByUserB = transactionTemplate.execute(status ->
            listItemRepository.findById(itemId).orElseThrow());

        assertEquals(itemLoadedByUserA.getVersion(), itemLoadedByUserB.getVersion(),
            "Ambas as leituras devem partir da mesma versão");

        // When: usuário A salva sua alteração primeiro — sucesso, a versão avança no banco.
        itemLoadedByUserA.setName("Alterado por A");
        transactionTemplate.executeWithoutResult(status ->
            listItemRepository.saveAndFlush(itemLoadedByUserA));

        // Then: usuário B tenta salvar com a versão desatualizada (stale) — deve falhar,
        // provando que o lock otimista detecta a edição concorrente em vez de sobrescrever
        // silenciosamente a alteração de A.
        itemLoadedByUserB.setName("Alterado por B");
        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
            transactionTemplate.executeWithoutResult(status ->
                listItemRepository.saveAndFlush(itemLoadedByUserB)));
    }
}
