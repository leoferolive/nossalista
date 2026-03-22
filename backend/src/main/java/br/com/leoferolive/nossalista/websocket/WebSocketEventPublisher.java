package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public WebSocketEventPublisher(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void publishEvent(UUID listId, String type, Object payload, User actor) {
        publishEvent(listId, type, payload, actor, null);
    }

    public void publishEvent(UUID listId, String type, Object payload, User actor, Long revision) {
        publishListEvent(listId, "items", type, payload, actor, revision);
    }

    public void publishPresenceEvent(UUID listId, String type, Object payload, User actor) {
        publishListEvent(listId, "presence", type, payload, actor, null);
    }

    public void publishPresenceEvent(UUID listId, String type, Object payload) {
        publishListEvent(listId, "presence", type, payload, null, null);
    }

    private void publishListEvent(UUID listId, String channel, String type, Object payload, User actor, Long revision) {
        WebSocketMessage.Builder builder = WebSocketMessage.builder()
            .eventId(UUID.randomUUID().toString())
            .listId(listId)
            .channel(channel)
            .revision(revision)
            .type(type)
            .payload(payload);

        if (actor != null) {
            builder.actor(new WebSocketActor(actor.getId(), actor.getUsername()));
        }

        simpMessagingTemplate.convertAndSend(resolveDestination(listId, channel), builder.build());
    }

    private String resolveDestination(UUID listId, String channel) {
        return switch (channel) {
            case "items" -> WebSocketDestinations.listItems(listId);
            case "presence" -> WebSocketDestinations.listPresence(listId);
            default -> throw new IllegalArgumentException("Unsupported WebSocket channel: " + channel);
        };
    }
}
