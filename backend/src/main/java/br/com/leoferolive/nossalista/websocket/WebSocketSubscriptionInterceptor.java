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
import java.util.UUID;

/**
 * Interceptor de canal STOMP que verifica autorização de subscribe em tópicos de lista.
 * Garante que apenas donos e membros possam se inscrever em "/topic/list/{listId}".
 */
@Component
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    private static final String LIST_TOPIC_PREFIX = "/topic/list/";

    private final ListMemberRepository listMemberRepository;

    public WebSocketSubscriptionInterceptor(ListMemberRepository listMemberRepository) {
        this.listMemberRepository = listMemberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith(LIST_TOPIC_PREFIX)) {
                String listIdStr = destination.substring(LIST_TOPIC_PREFIX.length());

                UUID listId;
                try {
                    listId = UUID.fromString(listIdStr);
                } catch (IllegalArgumentException e) {
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

    private User extractUser(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof User user) {
                return user;
            }
        }
        return null;
    }
}
