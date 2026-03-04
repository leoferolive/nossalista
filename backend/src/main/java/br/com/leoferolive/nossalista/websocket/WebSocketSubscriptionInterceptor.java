package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interceptor de canal STOMP que autentica conexões no CONNECT e
 * verifica autorização de subscribe em tópicos de lista.
 */
@Component
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserService userService;
    private final ListMemberRepository listMemberRepository;

    public WebSocketSubscriptionInterceptor(
        JwtService jwtService,
        UserService userService,
        ListMemberRepository listMemberRepository
    ) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.listMemberRepository = listMemberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor, message);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith(WebSocketDestinations.LIST_TOPIC_PREFIX)) {
                UUID listId = extractListId(destination);
                if (listId == null) {
                    throw new MessageDeliveryException(message,
                        "Destino de subscribe inválido: " + destination);
                }

                User user = extractUser(accessor);
                if (user == null) {
                    throw new MessageDeliveryException(message,
                        "Usuário não autenticado para subscribe em: " + destination);
                }

                boolean isMember = listMemberRepository.existsByListIdAndUserId(listId, user.getId());
                if (!isMember) {
                    throw new MessageDeliveryException(message,
                        "Acesso negado ao tópico: " + destination);
                }
            }
        }

        return message;
    }

    private UUID extractListId(String destination) {
        String listIdStr = destination.substring(WebSocketDestinations.LIST_TOPIC_PREFIX.length());
        int suffixIndex = listIdStr.indexOf('/');
        if (suffixIndex >= 0) {
            listIdStr = listIdStr.substring(0, suffixIndex);
        }

        try {
            return UUID.fromString(listIdStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void authenticate(StompHeaderAccessor accessor, Message<?> message) {
        String token = extractToken(accessor);
        if (token == null || token.isBlank()) {
            throw new MessageDeliveryException(message, "Token JWT ausente no CONNECT STOMP");
        }

        if (!jwtService.validateToken(token)) {
            throw new MessageDeliveryException(message, "Token JWT inválido no CONNECT STOMP");
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userService.findById(userId).orElseThrow(() ->
            new MessageDeliveryException(message, "Usuário do token não encontrado"));

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        accessor.setUser(authentication);
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("user", user);
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
        if (authorizationHeaders != null) {
            for (String header : authorizationHeaders) {
                if (header != null && header.startsWith("Bearer ")) {
                    return header.substring(7);
                }
            }
        }

        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null) {
            for (String token : tokenHeaders) {
                if (token != null && !token.isBlank()) {
                    return token;
                }
            }
        }

        return null;
    }

    User extractUser(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof User user) {
                return user;
            }
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object sessionUser = sessionAttributes.get("user");
            if (sessionUser instanceof User user) {
                return user;
            }
        }

        return null;
    }
}
