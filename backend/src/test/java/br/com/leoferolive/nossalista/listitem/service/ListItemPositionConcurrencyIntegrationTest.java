package br.com.leoferolive.nossalista.listitem.service;

import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.listitem.dto.CreateItemRequestDTO;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, com Spring real (transações e banco reais, sem simular a colisão),
 * que o retry manual de {@link ListItemService#addItem} (ver
 * {@code insertItemWithPositionRetry}) fecha a race de {@code position}
 * deixada pela Onda 1: N chamadas concorrentes de {@code addItem} na MESMA
 * lista devem resultar em N itens com positions únicas e contíguas
 * (0..N-1), sem propagar exceção a nenhum chamador — mesmo quando duas ou
 * mais threads leem o mesmo {@code maxPosition} antes de qualquer uma
 * commitar (a violação da constraint {@code uq_list_items_list_position},
 * migration V18, é capturada e vira uma nova tentativa transparente).
 *
 * <p>Cada task chama {@code listItemService.addItem(...)} diretamente no
 * bean real injetado pelo Spring — o proxy {@code @Transactional} do método
 * abre sua própria transação de topo por chamada, simulando N requisições
 * HTTP concorrentes de {@code add_items} na mesma lista.
 *
 * <p><b>Nota sobre determinismo:</b> como o teste não instrumenta o código
 * de produção para forçar o entrelaçamento exato das leituras de
 * {@code maxPosition}, não há garantia de que a colisão realmente ocorra em
 * toda execução — depende do agendamento de threads da JVM/SO. As
 * asserções (positions únicas, contíguas, sem erro ao chamador) são,
 * porém, válidas independentemente de a colisão ter sido de fato
 * exercitada nesta execução específica; o alto grau de concorrência
 * (threads = requisições) e o H2 em memória (latência mínima) tornam a
 * colisão bastante provável na prática.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@RegressionTest
class ListItemPositionConcurrencyIntegrationTest {

    private static final int CONCURRENT_ADDS = 10;
    private static final int AWAIT_SECONDS = 30;

    @Autowired
    private ListItemService listItemService;

    @Autowired
    private ListService listService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListItemRepository listItemRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private User owner;
    private SharedList list;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        // Setup roda em transação própria, comitada de verdade, para que a
        // lista exista para as transações independentes de cada addItem concorrente.
        transactionTemplate.executeWithoutResult(status -> {
            String unique = UUID.randomUUID().toString();
            owner = userService.createUser(
                    "race-owner-" + unique,
                    "race-owner-" + unique + "@example.com",
                    "hashedPassword",
                    "Race Owner",
                    AuthProvider.EMAIL
            );
            list = listService.createList(new CreateListRequest("Lista Race T3", 1), owner);
        });
    }

    @AfterEach
    void tearDown() {
        // Deletar o owner comita e faz CASCADE (lists -> list_items/list_members/
        // activity_logs, ver V2/V3/V4/V6__*.sql), limpando o que este teste
        // comitou no H2 compartilhado pela suíte (Surefire com reuseForks=true).
        transactionTemplate.executeWithoutResult(status -> userRepository.deleteById(owner.getId()));
    }

    @Test
    @DisplayName("Adds concorrentes na mesma lista geram positions únicas e contíguas, sem propagar erro ao chamador")
    void concurrentAddsResultInUniqueContiguousPositionsWithoutPropagatingErrors() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ADDS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_ADDS);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<UUID>> tasks = IntStream.range(0, CONCURRENT_ADDS)
                .mapToObj(i -> (Callable<UUID>) () -> {
                    CreateItemRequestDTO dto =
                            new CreateItemRequestDTO("Item concorrente " + i, null, null, null, null);
                    // Sincroniza o início: todas as threads já prontas disparam
                    // addItem "ao mesmo tempo", maximizando a chance de duas ou
                    // mais lerem o mesmo maxPosition antes de qualquer commit.
                    readyLatch.countDown();
                    startLatch.await();
                    return listItemService.addItem(list.getId(), dto, owner).id();
                })
                .collect(Collectors.toList());

        List<Future<UUID>> futures = new ArrayList<>();
        try {
            for (Callable<UUID> task : tasks) {
                futures.add(executor.submit(task));
            }
            assertThat(readyLatch.await(AWAIT_SECONDS, TimeUnit.SECONDS))
                    .as("todas as threads devem ficar prontas antes do disparo sincronizado")
                    .isTrue();
            startLatch.countDown();

            // Future.get() relança qualquer exceção da task via ExecutionException —
            // se addItem propagasse erro ao chamador (retry esgotado ou qualquer
            // outra falha), este loop capturaria e o teste falharia aqui.
            List<UUID> createdItemIds = new ArrayList<>();
            for (Future<UUID> future : futures) {
                createdItemIds.add(future.get(AWAIT_SECONDS, TimeUnit.SECONDS));
            }

            assertThat(createdItemIds)
                    .as("todas as %d chamadas de addItem devem retornar com sucesso, sem exceção ao chamador",
                            CONCURRENT_ADDS)
                    .hasSize(CONCURRENT_ADDS)
                    .doesNotHaveDuplicates();
        } catch (ExecutionException | TimeoutException e) {
            throw new AssertionError(
                    "addItem concorrente propagou erro ao chamador (retry deveria ter absorvido a colisão): "
                            + e.getCause(), e);
        } finally {
            executor.shutdownNow();
        }

        List<ListItem> items = listItemRepository.findByListIdOrderByPositionAsc(list.getId());
        List<Integer> positions = items.stream().map(ListItem::getPosition).collect(Collectors.toList());
        List<Integer> expectedPositions =
                IntStream.range(0, CONCURRENT_ADDS).boxed().collect(Collectors.toList());

        assertThat(items).as("todos os itens devem ter sido persistidos").hasSize(CONCURRENT_ADDS);
        assertThat(positions).as("positions devem ser únicas (constraint V18 nunca deve ser violada ao final)")
                .doesNotHaveDuplicates();
        assertThat(positions).as("positions devem ser contíguas, cobrindo exatamente 0..N-1")
                .containsExactlyInAnyOrderElementsOf(expectedPositions);
    }
}
