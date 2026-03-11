package br.com.leoferolive.nossalista.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PushSubscriptionStore Tests")
class PushSubscriptionStoreTest {

    @Test
    @DisplayName("add deve armazenar subscrição para o usuário")
    void addShouldStoreSubscription() {
        PushSubscriptionStore store = new PushSubscriptionStore();
        UUID userId = UUID.randomUUID();
        PushSubscription sub = new PushSubscription("https://endpoint.com", "p256dh", "auth");

        store.add(userId, sub);

        assertThat(store.findByUserId(userId)).containsExactly(sub);
    }

    @Test
    @DisplayName("add deve respeitar limite máximo de 5 subscrições por usuário (FIFO)")
    void addShouldRespectMaxPerUser() {
        PushSubscriptionStore store = new PushSubscriptionStore();
        UUID userId = UUID.randomUUID();

        for (int i = 0; i < 6; i++) {
            store.add(userId, new PushSubscription("https://endpoint-" + i + ".com", "p256dh", "auth"));
        }

        List<PushSubscription> subs = store.findByUserId(userId);
        assertThat(subs).hasSize(5);
        // O mais antigo (endpoint-0) deve ter sido removido
        assertThat(subs).noneMatch(s -> s.endpoint().equals("https://endpoint-0.com"));
    }

    @Test
    @DisplayName("remove deve remover subscrição pelo endpoint")
    void removeShouldRemoveByEndpoint() {
        PushSubscriptionStore store = new PushSubscriptionStore();
        UUID userId = UUID.randomUUID();
        PushSubscription sub = new PushSubscription("https://endpoint.com", "p256dh", "auth");

        store.add(userId, sub);
        store.remove(userId, sub.endpoint());

        assertThat(store.findByUserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("findByUserId deve retornar lista vazia para usuário desconhecido")
    void findByUserIdShouldReturnEmptyForUnknownUser() {
        PushSubscriptionStore store = new PushSubscriptionStore();
        UUID unknownId = UUID.randomUUID();

        assertThat(store.findByUserId(unknownId)).isEmpty();
    }
}
