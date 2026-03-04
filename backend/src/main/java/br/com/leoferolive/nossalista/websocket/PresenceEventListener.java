package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.websocket.dto.PresenceSnapshotPayload;
import br.com.leoferolive.nossalista.websocket.dto.MemberOnlinePayload;
import br.com.leoferolive.nossalista.websocket.dto.MemberOfflinePayload;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PresenceEventListener {

    private final PresenceService presenceService;
    private final WebSocketEventPublisher eventPublisher;

    public PresenceEventListener(PresenceService presenceService,
                                 WebSocketEventPublisher eventPublisher) {
        this.presenceService = presenceService;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (!isPresenceDestination(destination)) {
            return;
        }
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
        broadcastPresenceSnapshot(listId);
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

        eventPublisher.publishPresenceEvent(listId, "MEMBER_ONLINE", payload, user);
    }

    private void broadcastOffline(UUID listId, User user) {
        MemberOfflinePayload payload = new MemberOfflinePayload(
            user.getId().toString(),
            user.getUsername()
        );

        eventPublisher.publishPresenceEvent(listId, "MEMBER_OFFLINE", payload, user);
    }

    private void broadcastPresenceSnapshot(UUID listId) {
        Collection<User> onlineUsers = presenceService.getOnlineUsers(listId);
        List<MemberOnlinePayload> members = onlineUsers.stream()
            .sorted(Comparator.comparing(User::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)))
            .map(user -> new MemberOnlinePayload(
                user.getId().toString(),
                user.getUsername(),
                user.getName(),
                user.getAvatarUrl()
            ))
            .collect(Collectors.toList());

        eventPublisher.publishPresenceEvent(listId, "PRESENCE_SNAPSHOT", new PresenceSnapshotPayload(members));
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
        if (destination == null || !destination.startsWith(WebSocketDestinations.LIST_TOPIC_PREFIX)) {
            return null;
        }

        String rawListId = destination.substring(WebSocketDestinations.LIST_TOPIC_PREFIX.length());
        int suffixIndex = rawListId.indexOf('/');
        if (suffixIndex >= 0) {
            rawListId = rawListId.substring(0, suffixIndex);
        }
        try {
            return UUID.fromString(rawListId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isPresenceDestination(String destination) {
        return destination != null
            && destination.startsWith(WebSocketDestinations.LIST_TOPIC_PREFIX)
            && (
                destination.endsWith(WebSocketDestinations.PRESENCE_SUFFIX)
                    || !destination.substring(WebSocketDestinations.LIST_TOPIC_PREFIX.length()).contains("/")
            );
    }
}
