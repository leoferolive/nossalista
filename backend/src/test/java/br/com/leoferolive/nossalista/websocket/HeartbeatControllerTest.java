package br.com.leoferolive.nossalista.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("HeartbeatController Tests")
class HeartbeatControllerTest {

    @Mock
    private PresenceService presenceService;

    private HeartbeatController controller;

    @BeforeEach
    void setUp() {
        controller = new HeartbeatController(presenceService);
    }

    @Test
    @DisplayName("Heartbeat com sessionId válido atualiza a presença")
    void handleHeartbeatWithValidSessionUpdatesPresence() {
        UUID listId = UUID.randomUUID();
        Message<byte[]> message = buildMessage("session-1");

        controller.handleHeartbeat(listId, message);

        verify(presenceService).updateHeartbeat(listId, "session-1");
    }

    @Test
    @DisplayName("Heartbeat sem sessionId não aciona o PresenceService")
    void handleHeartbeatWithoutSessionIdDoesNothing() {
        UUID listId = UUID.randomUUID();
        Message<byte[]> message = buildMessage(null);

        controller.handleHeartbeat(listId, message);

        verify(presenceService, never()).updateHeartbeat(any(), any());
    }

    @Test
    @DisplayName("Heartbeat com sessionId em branco não aciona o PresenceService")
    void handleHeartbeatWithBlankSessionIdDoesNothing() {
        UUID listId = UUID.randomUUID();
        Message<byte[]> message = buildMessage("   ");

        controller.handleHeartbeat(listId, message);

        verify(presenceService, never()).updateHeartbeat(any(), any());
    }

    private Message<byte[]> buildMessage(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        if (sessionId != null) {
            accessor.setSessionId(sessionId);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
