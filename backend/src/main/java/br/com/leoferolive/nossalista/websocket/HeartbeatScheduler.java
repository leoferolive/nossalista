package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.websocket.dto.MemberOfflinePayload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class HeartbeatScheduler {

    private final PresenceService presenceService;
    private final WebSocketEventPublisher eventPublisher;

    public HeartbeatScheduler(PresenceService presenceService, WebSocketEventPublisher eventPublisher) {
        this.presenceService = presenceService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 30_000)
    public void evictExpiredSessions() {
        for (PresenceService.ExpiredPresence expired : presenceService.evictExpiredSessions(Duration.ofSeconds(60))) {
            MemberOfflinePayload payload = new MemberOfflinePayload(
                expired.user().getId().toString(),
                expired.user().getUsername()
            );

            eventPublisher.publishPresenceEvent(expired.listId(), "MEMBER_OFFLINE", payload, expired.user());
        }
    }
}
