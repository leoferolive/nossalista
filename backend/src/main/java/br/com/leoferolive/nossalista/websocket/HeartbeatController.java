package br.com.leoferolive.nossalista.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class HeartbeatController {

    private final PresenceService presenceService;

    public HeartbeatController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @MessageMapping("/list/{listId}/heartbeat")
    public void handleHeartbeat(@DestinationVariable UUID listId, Message<?> message) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        presenceService.updateHeartbeat(listId, sessionId);
    }
}
