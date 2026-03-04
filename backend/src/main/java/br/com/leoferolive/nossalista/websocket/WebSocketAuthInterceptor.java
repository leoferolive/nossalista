package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interceptor de handshake WebSocket que autentica o cliente via JWT.
 * Extrai o token do query param "token" ou do header "Authorization: Bearer {token}".
 * Rejeita conexões sem token válido retornando false (HTTP 401).
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    public WebSocketAuthInterceptor(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) throws Exception {
        String token = extractToken(request);

        // SockJS/STOMP clients often send auth only in the CONNECT frame,
        // so the HTTP handshake must remain permissive when no token is present.
        if (token == null || token.isBlank()) {
            return true;
        }

        if (!jwtService.validateToken(token)) {
            return false;
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userService.findById(userId).orElse(null);

        if (user == null) {
            return false;
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        attributes.put("user", user);

        return true;
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {
        // Limpar o SecurityContextHolder após o handshake para evitar vazamento
        // de contexto em threads reutilizadas do pool
        SecurityContextHolder.clearContext();
    }

    /**
     * Extrai o JWT do query param "token" ou do header "Authorization: Bearer {token}".
     *
     * @param request requisição HTTP do handshake
     * @return token JWT ou null se não encontrado
     */
    private String extractToken(ServerHttpRequest request) {
        // 1. Tentar extrair do query param "token"
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }

        // 2. Tentar extrair do header "Authorization: Bearer {token}"
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        return null;
    }
}
