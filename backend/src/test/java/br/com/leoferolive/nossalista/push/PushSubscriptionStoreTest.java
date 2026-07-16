package br.com.leoferolive.nossalista.push;

import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do {@link PushSubscriptionStore} agora PERSISTIDO no banco (T2 — Onda 1).
 *
 * <p>O store passou de in-memory ({@code ConcurrentHashMap}, por instância) para banco
 * compartilhado: como cada deploy substitui o pod ({@code replicas: 1}), toda inscrição de
 * push era apagada a cada release. Por isso o teste é de integração — exercita o round-trip
 * real save/find/delete/upsert contra o H2 (MODE=PostgreSQL). Mesmo padrão de
 * {@code OAuthCodeStoreTest} (D-011).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("PushSubscriptionStore Tests (persistido)")
class PushSubscriptionStoreTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    private PushSubscriptionRepository repository;

    @Autowired
    private UserRepository userRepository;

    private PushSubscriptionStore store() {
        return new PushSubscriptionStore(repository);
    }

    /** push_subscriptions.user_id tem FK para users(id): cada teste precisa de um usuário real. */
    private UUID persistUser() {
        int n = COUNTER.incrementAndGet();
        User user = new User();
        user.setUsername("push-user-" + n);
        user.setEmail("push-user-" + n + "@example.com");
        user.setPassword("$2a$10$dummyHashForTesting");
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        return userRepository.saveAndFlush(user).getId();
    }

    @Test
    @DisplayName("add deve armazenar subscrição para o usuário")
    void addShouldStoreSubscription() {
        PushSubscriptionStore store = store();
        UUID userId = persistUser();
        PushSubscription sub = new PushSubscription("https://endpoint.com", "p256dh", "auth");

        store.add(userId, sub);

        assertThat(store.findByUserId(userId)).containsExactly(sub);
    }

    @Test
    @DisplayName("add deve respeitar limite máximo de 5 subscrições por usuário (FIFO)")
    void addShouldRespectMaxPerUser() {
        PushSubscriptionStore store = store();
        UUID userId = persistUser();

        for (int i = 0; i < 6; i++) {
            store.add(userId, new PushSubscription("https://endpoint-" + i + ".com", "p256dh", "auth"));
        }

        List<PushSubscription> subs = store.findByUserId(userId);
        assertThat(subs).hasSize(5);
        // O mais antigo (endpoint-0) deve ter sido removido
        assertThat(subs).noneMatch(s -> s.endpoint().equals("https://endpoint-0.com"));
    }

    @Test
    @DisplayName("add deve substituir (upsert) a inscrição existente com o mesmo endpoint")
    void addShouldUpsertByEndpoint() {
        PushSubscriptionStore store = store();
        UUID userId = persistUser();
        PushSubscription original = new PushSubscription("https://endpoint.com", "p256dh-old", "auth-old");
        PushSubscription renovada = new PushSubscription("https://endpoint.com", "p256dh-new", "auth-new");

        store.add(userId, original);
        store.add(userId, renovada);

        List<PushSubscription> subs = store.findByUserId(userId);
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0)).isEqualTo(renovada);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("add deve transferir a inscrição para o novo usuário quando o endpoint já pertence a outro")
    void addShouldReassignEndpointToNewOwner() {
        PushSubscriptionStore store = store();
        UUID userA = persistUser();
        UUID userB = persistUser();
        PushSubscription sub = new PushSubscription("https://endpoint-shared.com", "p256dh", "auth");

        store.add(userA, sub);
        store.add(userB, sub);

        assertThat(store.findByUserId(userA)).isEmpty();
        assertThat(store.findByUserId(userB)).containsExactly(sub);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("remove deve remover subscrição pelo endpoint")
    void removeShouldRemoveByEndpoint() {
        PushSubscriptionStore store = store();
        UUID userId = persistUser();
        PushSubscription sub = new PushSubscription("https://endpoint.com", "p256dh", "auth");

        store.add(userId, sub);
        store.remove(userId, sub.endpoint());

        assertThat(store.findByUserId(userId)).isEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("removeAll deve remover todas as subscrições do usuário")
    void removeAllShouldRemoveEverySubscriptionForUser() {
        PushSubscriptionStore store = store();
        UUID userId = persistUser();
        store.add(userId, new PushSubscription("https://endpoint-1.com", "p256dh", "auth"));
        store.add(userId, new PushSubscription("https://endpoint-2.com", "p256dh", "auth"));

        store.removeAll(userId);

        assertThat(store.findByUserId(userId)).isEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("findByUserId deve retornar lista vazia para usuário desconhecido")
    void findByUserIdShouldReturnEmptyForUnknownUser() {
        PushSubscriptionStore store = store();
        UUID unknownId = UUID.randomUUID();

        assertThat(store.findByUserId(unknownId)).isEmpty();
    }

    @Test
    @DisplayName("subscrição salva sobrevive a uma nova instância do store (persistência real)")
    void subscriptionSurvivesNewStoreInstance() {
        UUID userId = persistUser();
        PushSubscription sub = new PushSubscription("https://endpoint-persist.com", "p256dh", "auth");

        store().add(userId, sub);

        // Simula um novo "processo"/pod lendo do mesmo banco compartilhado: nova instância
        // do store, mesmo repository (mesma conexão/schema).
        PushSubscriptionStore novaInstancia = store();
        assertThat(novaInstancia.findByUserId(userId)).containsExactly(sub);
    }
}
