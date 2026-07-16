package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PresenceService Tests")
class PresenceServiceTest {

    @Test
    @DisplayName("registerSession adiciona sessão corretamente")
    void registerSessionAddsEntryForList() {
        PresenceService service = new PresenceService();
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");

        service.registerSession(listId, "session-1", user);

        assertThat(service.getOnlineUsers(listId)).containsExactly(user);
    }

    @Test
    @DisplayName("removeSession retorna usuário e limpa entrada")
    void removeSessionReturnsUserAndClearsEntry() {
        PresenceService service = new PresenceService();
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        service.registerSession(listId, "session-1", user);

        Optional<User> removed = service.removeSession(listId, "session-1");

        assertThat(removed).contains(user);
        assertThat(service.getOnlineUsers(listId)).isEmpty();
    }

    @Test
    @DisplayName("removeSessionAllLists remove sessão de todas as listas")
    void removeSessionAllListsRemovesSessionFromAllLists() {
        PresenceService service = new PresenceService();
        UUID listA = UUID.randomUUID();
        UUID listB = UUID.randomUUID();
        User user = createUser("maria");
        service.registerSession(listA, "session-1", user);
        service.registerSession(listB, "session-1", user);

        List<PresenceService.RemovedPresence> removed = service.removeSessionAllLists("session-1");

        assertThat(removed).hasSize(2);
        assertThat(removed).extracting(PresenceService.RemovedPresence::listId).containsExactlyInAnyOrder(listA, listB);
        assertThat(service.getOnlineUsers(listA)).isEmpty();
        assertThat(service.getOnlineUsers(listB)).isEmpty();
    }

    @Test
    @DisplayName("evictExpiredSessions remove só sessões expiradas, preserva as válidas")
    void evictExpiredSessionsRemovesOnlyExpired() {
        Instant[] now = {Instant.now()};
        PresenceService service = new PresenceService(() -> now[0]);
        UUID listId = UUID.randomUUID();
        User expiredUser = createUser("old-user");
        User validUser = createUser("new-user");

        now[0] = Instant.now().minusSeconds(120);
        service.registerSession(listId, "expired-session", expiredUser);

        now[0] = Instant.now();
        service.registerSession(listId, "valid-session", validUser);

        List<PresenceService.ExpiredPresence> evicted = service.evictExpiredSessions(Duration.ofSeconds(60));

        assertThat(evicted).hasSize(1);
        assertThat(evicted.getFirst().user()).isEqualTo(expiredUser);
        assertThat(service.getOnlineUsers(listId)).containsExactly(validUser);
    }

    @Test
    @DisplayName("updateHeartbeat atualiza timestamp e evita expiração da sessão")
    void updateHeartbeatPreventsSessionEviction() {
        Instant[] now = {Instant.now()};
        PresenceService service = new PresenceService(() -> now[0]);
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");

        now[0] = Instant.now().minusSeconds(120);
        service.registerSession(listId, "session-1", user);

        now[0] = Instant.now();
        service.updateHeartbeat(listId, "session-1");

        List<PresenceService.ExpiredPresence> evicted = service.evictExpiredSessions(Duration.ofSeconds(60));
        assertThat(evicted).isEmpty();
        assertThat(service.getOnlineUsers(listId)).containsExactly(user);
    }

    @Test
    @DisplayName("getOnlineUsers retorna apenas usuários com sessão ativa")
    void getOnlineUsersReturnsOnlyUsersWithActiveSessions() {
        PresenceService service = new PresenceService();
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");

        service.registerSession(listId, "session-1", user);
        service.removeSession(listId, "session-1");

        assertThat(service.getOnlineUsers(listId)).isEmpty();
    }

    @Test
    @DisplayName("getTotalActiveSessions soma sessões ativas entre listas")
    void getTotalActiveSessionsSumsAllSessionsAcrossLists() {
        PresenceService service = new PresenceService();
        UUID listA = UUID.randomUUID();
        UUID listB = UUID.randomUUID();
        User userA = createUser("maria");
        User userB = createUser("joao");

        service.registerSession(listA, "session-a1", userA);
        service.registerSession(listA, "session-a2", userA);
        service.registerSession(listB, "session-b1", userB);

        assertThat(service.getTotalActiveSessions()).isEqualTo(3);
    }

    @Test
    @DisplayName("isUserConnected retorna true quando usuário tem sessão ativa em qualquer lista")
    void isUserConnectedReturnsTrueWhenUserHasActiveSessions() {
        PresenceService service = new PresenceService();
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        service.registerSession(listId, "session-1", user);

        assertThat(service.isUserConnected(user.getId())).isTrue();
    }

    @Test
    @DisplayName("isUserConnected retorna false quando usuário não tem sessões")
    void isUserConnectedReturnsFalseWhenUserHasNoSessions() {
        PresenceService service = new PresenceService();
        UUID unknownUserId = UUID.randomUUID();

        assertThat(service.isUserConnected(unknownUserId)).isFalse();
    }

    @Test
    @DisplayName("isUserConnected retorna false após usuário sair de todas as listas")
    void isUserConnectedReturnsFalseAfterUserLeavesAllLists() {
        PresenceService service = new PresenceService();
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        service.registerSession(listId, "session-1", user);
        service.removeSessionAllLists("session-1");

        assertThat(service.isUserConnected(user.getId())).isFalse();
    }

    @Test
    @DisplayName("registerSubscription com id em branco não registra a inscrição")
    void registerSubscriptionWithBlankIdDoesNothing() {
        PresenceService service = new PresenceService();
        UUID listId = UUID.randomUUID();

        service.registerSubscription("   ", listId, "session-1");

        assertThat(service.removeBySubscription("   ", "session-1")).isNull();
    }

    @Test
    @DisplayName("removeBySubscription com subscriptionId desconhecido retorna null")
    void removeBySubscriptionWithUnknownIdReturnsNull() {
        PresenceService service = new PresenceService();

        assertThat(service.removeBySubscription("unknown-sub", "session-1")).isNull();
    }

    @Test
    @DisplayName("removeBySubscription com sessionId nulo usa a sessão registrada na inscrição")
    void removeBySubscriptionWithNullSessionIdUsesRegisteredSession() {
        PresenceService service = new PresenceService();
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        service.registerSession(listId, "session-1", user);
        service.registerSubscription("sub-1", listId, "session-1");

        PresenceService.RemovedPresence removed = service.removeBySubscription("sub-1", null);

        assertThat(removed).isNotNull();
        assertThat(removed.user()).isEqualTo(user);
        assertThat(removed.listId()).isEqualTo(listId);
        assertThat(service.getOnlineUsers(listId)).isEmpty();
    }

    @Test
    @DisplayName("removeSession em lista sem sessões retorna vazio")
    void removeSessionOnListWithoutSessionsReturnsEmpty() {
        PresenceService service = new PresenceService();
        UUID listId = UUID.randomUUID();

        Optional<User> removed = service.removeSession(listId, "session-1");

        assertThat(removed).isEmpty();
    }

    private User createUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setName(username);
        return user;
    }
}
