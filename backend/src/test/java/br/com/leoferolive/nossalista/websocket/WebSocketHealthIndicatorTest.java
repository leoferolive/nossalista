package br.com.leoferolive.nossalista.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketHealthIndicator tests")
class WebSocketHealthIndicatorTest {

    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private WebSocketHealthIndicator webSocketHealthIndicator;

    @Test
    @DisplayName("deve retornar UP com activeSessions quando PresenceService responde")
    void shouldReturnUpWhenPresenceServiceResponds() {
        when(presenceService.getTotalActiveSessions()).thenReturn(3);

        Health health = webSocketHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("activeSessions", 3);
    }

    @Test
    @DisplayName("deve retornar DOWN quando PresenceService lançar exceção")
    void shouldReturnDownWhenPresenceServiceThrows() {
        when(presenceService.getTotalActiveSessions()).thenThrow(new IllegalStateException("boom"));

        Health health = webSocketHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
