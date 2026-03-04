package br.com.leoferolive.nossalista.websocket;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WebSocketHealthIndicator implements HealthIndicator {

    private final PresenceService presenceService;

    public WebSocketHealthIndicator(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    public Health health() {
        try {
            int totalSessions = presenceService.getTotalActiveSessions();
            return Health.up()
                .withDetail("activeSessions", totalSessions)
                .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
