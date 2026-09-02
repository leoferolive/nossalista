package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interceptor de canal STOMP que autentica conexões no CONNECT e
 * verifica autorização de subscribe em tópicos de lista.
 */
@Component
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    private final ListMemberRepository listMemberRepository;

    public WebSocketSubscriptionInterceptor(ListMemberRepository listMemberRepository) {
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
            } else if (destination != null && destination.startsWith(WebSocketDestinations.USER_TOPIC_PREFIX)) {
                User user = extractUser(accessor);
                if (user == null) {
                    throw new MessageDeliveryException(message,
                        "Usuário não autenticado para subscribe em: " + destination);
                }

                UUID targetUserId = extractUserIdFromUserTopic(destination);
                if (targetUserId == null || !targetUserId.equals(user.getId())) {
                    throw new MessageDeliveryException(message,
                        "Acesso negado: " + destination);
                }
            }
        }

        return message;
    }

    private UUID extractUserIdFromUserTopic(String destination) {
        try {
            String after = destination.substring(WebSocketDestinations.USER_TOPIC_PREFIX.length());
            return UUID.fromString(after.split("/")[0]);
        } catch (Exception e) {
            return null;
        }
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
        User user = extractUser(accessor);
        if (user == null) {
            throw new MessageDeliveryException(message, "Sessão ausente no CONNECT STOMP");
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, List.of());
        accessor.setUser(authentication);

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("user", user);
        }
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
