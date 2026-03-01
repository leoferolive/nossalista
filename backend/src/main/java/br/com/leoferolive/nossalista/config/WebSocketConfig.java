package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.websocket.WebSocketAuthInterceptor;
import br.com.leoferolive.nossalista.websocket.WebSocketSubscriptionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuração WebSocket com STOMP sobre SockJS.
 * Registra broker de mensagens, endpoint STOMP e interceptors de auth.
 * Reutiliza as origens CORS definidas no SecurityConfig para manter consistência.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final WebSocketSubscriptionInterceptor webSocketSubscriptionInterceptor;
    private final CorsConfigurationSource corsConfigurationSource;

    public WebSocketConfig(
        WebSocketAuthInterceptor webSocketAuthInterceptor,
        WebSocketSubscriptionInterceptor webSocketSubscriptionInterceptor,
        CorsConfigurationSource corsConfigurationSource
    ) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.webSocketSubscriptionInterceptor = webSocketSubscriptionInterceptor;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins(resolveAllowedOrigins())
            .addInterceptors(webSocketAuthInterceptor)
            .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketSubscriptionInterceptor);
    }

    /**
     * Extrai as origens permitidas do CorsConfigurationSource do SecurityConfig,
     * garantindo que WebSocket e REST usem sempre as mesmas origens.
     */
    private String[] resolveAllowedOrigins() {
        List<String> origins = new ArrayList<>();
        if (corsConfigurationSource instanceof UrlBasedCorsConfigurationSource urlBased) {
            CorsConfiguration corsConfig = urlBased.getCorsConfigurations().get("/**");
            if (corsConfig != null && corsConfig.getAllowedOrigins() != null) {
                origins.addAll(corsConfig.getAllowedOrigins());
            }
        }
        return origins.toArray(String[]::new);
    }
}
