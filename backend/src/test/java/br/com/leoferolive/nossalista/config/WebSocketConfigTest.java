package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("WebSocketConfig Tests")
class WebSocketConfigTest {

    @Autowired
    private WebSocketConfig webSocketConfig;

    @Test
    @DisplayName("WebSocketConfig bean deve ser carregado no contexto Spring")
    void webSocketConfigBeanShouldLoad() {
        assertThat(webSocketConfig).isNotNull();
    }

    @Test
    @DisplayName("WebSocketConfig deve ter anotação @EnableWebSocketMessageBroker")
    void webSocketConfigShouldHaveEnableWebSocketMessageBrokerAnnotation() {
        assertThat(WebSocketConfig.class.isAnnotationPresent(EnableWebSocketMessageBroker.class))
            .isTrue();
    }
}
