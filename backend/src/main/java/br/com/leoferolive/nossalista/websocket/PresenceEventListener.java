package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.websocket.dto.MemberOfflinePayload;
import br.com.leoferolive.nossalista.websocket.dto.MemberOnlinePayload;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class PresenceEventListener {

    private static final String LIST_TOPIC_PREFIX = "/topic/list/";

    private final PresenceService presenceService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public PresenceEventListener(PresenceService presenceService,
                                 SimpMessagingTemplate simpMessagingTemplate) {
        this.presenceService = presenceService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        UUID listId = parseListId(destination);
        if (listId == null) {
            return;
        }

        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        User user = extractUser(accessor);
        if (user == null) {
            return;
        }

        presenceService.registerSession(listId, sessionId, user);
        presenceService.registerSubscription(accessor.getSubscriptionId(), listId, sessionId);
        broadcastOnline(listId, user);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String subscriptionId = accessor.getSubscriptionId();
        String sessionId = accessor.getSessionId();
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }

        PresenceService.RemovedPresence removed = presenceService.removeBySubscription(subscriptionId, sessionId);
        if (removed != null) {
            broadcastOffline(removed.listId(), removed.user());
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        List<PresenceService.RemovedPresence> removedEntries = presenceService.removeSessionAllLists(sessionId);
        for (PresenceService.RemovedPresence removed : removedEntries) {
            broadcastOffline(removed.listId(), removed.user());
        }
    }

    private void broadcastOnline(UUID listId, User user) {
        MemberOnlinePayload payload = new MemberOnlinePayload(
            user.getId().toString(),
            user.getUsername(),
            user.getName(),
            user.getAvatarUrl()
        );

        WebSocketMessage message = WebSocketMessage.builder()
            .type("MEMBER_ONLINE")
            .payload(payload)
            .userId(user.getId())
            .username(user.getUsername())
            .timestamp(Instant.now())
            .build();

        simpMessagingTemplate.convertAndSend(LIST_TOPIC_PREFIX + listId, message);
    }

    private void broadcastOffline(UUID listId, User user) {
        MemberOfflinePayload payload = new MemberOfflinePayload(
            user.getId().toString(),
            user.getUsername()
        );

        WebSocketMessage message = WebSocketMessage.builder()
            .type("MEMBER_OFFLINE")
            .payload(payload)
            .userId(user.getId())
            .username(user.getUsername())
            .timestamp(Instant.now())
            .build();

        simpMessagingTemplate.convertAndSend(LIST_TOPIC_PREFIX + listId, message);
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

    private UUID parseListId(String destination) {
        if (destination == null || !destination.startsWith(LIST_TOPIC_PREFIX)) {
            return null;
        }

        String rawListId = destination.substring(LIST_TOPIC_PREFIX.length());
        try {
            return UUID.fromString(rawListId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
