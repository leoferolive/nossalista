package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.websocket.dto.MemberOfflinePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HeartbeatScheduler Tests")
class HeartbeatSchedulerTest {

    @Mock
    private PresenceService presenceService;

    @Mock
    private WebSocketEventPublisher eventPublisher;

    private HeartbeatScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new HeartbeatScheduler(presenceService, eventPublisher);
    }

    @Test
    @DisplayName("Sessões expiradas disparam MEMBER_OFFLINE para cada sessão expirada")
    void evictExpiredSessionsPublishesOfflineForEachExpiredSession() {
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        when(presenceService.evictExpiredSessions(Duration.ofSeconds(60)))
            .thenReturn(List.of(new PresenceService.ExpiredPresence(listId, user)));

        scheduler.evictExpiredSessions();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishPresenceEvent(eq(listId), eq("MEMBER_OFFLINE"), payloadCaptor.capture(), eq(user));
        MemberOfflinePayload payload = (MemberOfflinePayload) payloadCaptor.getValue();
        assertThat(payload.userId()).isEqualTo(user.getId().toString());
        assertThat(payload.username()).isEqualTo("maria");
    }

    @Test
    @DisplayName("Sem sessões expiradas não publica nenhum evento")
    void evictExpiredSessionsDoesNothingWhenNoneExpired() {
        when(presenceService.evictExpiredSessions(Duration.ofSeconds(60))).thenReturn(List.of());

        scheduler.evictExpiredSessions();

        verify(eventPublisher, never()).publishPresenceEvent(any(), any(), any(), any());
    }

    private User createUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        return user;
    }
}
