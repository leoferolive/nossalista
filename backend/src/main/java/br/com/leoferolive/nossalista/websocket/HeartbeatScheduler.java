package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.websocket.dto.MemberOfflinePayload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class HeartbeatScheduler {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public HeartbeatScheduler(PresenceService presenceService, SimpMessagingTemplate simpMessagingTemplate) {
        this.presenceService = presenceService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Scheduled(fixedDelay = 30_000)
    public void evictExpiredSessions() {
        for (PresenceService.ExpiredPresence expired : presenceService.evictExpiredSessions(Duration.ofSeconds(60))) {
            MemberOfflinePayload payload = new MemberOfflinePayload(
                expired.user().getId().toString(),
                expired.user().getUsername()
            );

            WebSocketMessage message = WebSocketMessage.builder()
                .type("MEMBER_OFFLINE")
                .payload(payload)
                .userId(expired.user().getId())
                .username(expired.user().getUsername())
                .timestamp(Instant.now())
                .build();

            simpMessagingTemplate.convertAndSend("/topic/list/" + expired.listId(), message);
        }
    }
}
