package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class PresenceService {

    private final Map<UUID, Map<String, PresenceEntry>> sessionsByList = new ConcurrentHashMap<>();
    private final Map<String, SubscriptionEntry> subscriptions = new ConcurrentHashMap<>();
    private final Supplier<Instant> clock;

    public PresenceService() {
        this(Instant::now);
    }

    PresenceService(Supplier<Instant> clock) {
        this.clock = clock;
    }

    public record PresenceEntry(User user, Instant lastHeartbeat) {
    }

    public record RemovedPresence(UUID listId, User user) {
    }

    public record ExpiredPresence(UUID listId, User user) {
    }

    private record SubscriptionEntry(UUID listId, String sessionId) {
    }

    public void registerSession(UUID listId, String sessionId, User user) {
        sessionsByList
            .computeIfAbsent(listId, ignored -> new ConcurrentHashMap<>())
            .put(sessionId, new PresenceEntry(user, clock.get()));
    }

    public void registerSubscription(String subscriptionId, UUID listId, String sessionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }
        subscriptions.put(subscriptionId, new SubscriptionEntry(listId, sessionId));
    }

    public Optional<User> removeSession(UUID listId, String sessionId) {
        PresenceEntry[] removedHolder = {null};

        sessionsByList.compute(listId, (key, sessions) -> {
            if (sessions == null) return null;
            removedHolder[0] = sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });

        if (removedHolder[0] != null) {
            subscriptions.entrySet().removeIf(entry ->
                entry.getValue().listId().equals(listId) && entry.getValue().sessionId().equals(sessionId)
            );
        }

        return Optional.ofNullable(removedHolder[0]).map(PresenceEntry::user);
    }

    public RemovedPresence removeBySubscription(String subscriptionId, String sessionId) {
        SubscriptionEntry entry = subscriptions.remove(subscriptionId);
        if (entry == null) {
            return null;
        }

        String targetSessionId = sessionId;
        if (targetSessionId == null || targetSessionId.isBlank()) {
            targetSessionId = entry.sessionId();
        }

        Optional<User> removed = removeSession(entry.listId(), targetSessionId);
        return removed.map(user -> new RemovedPresence(entry.listId(), user)).orElse(null);
    }

    public List<RemovedPresence> removeSessionAllLists(String sessionId) {
        List<RemovedPresence> removed = new ArrayList<>();

        List<UUID> listIds = new ArrayList<>(sessionsByList.keySet());
        for (UUID listId : listIds) {
            Optional<User> maybeUser = removeSession(listId, sessionId);
            maybeUser.ifPresent(user -> removed.add(new RemovedPresence(listId, user)));
        }

        subscriptions.entrySet().removeIf(entry -> entry.getValue().sessionId().equals(sessionId));
        return removed;
    }

    public Collection<User> getOnlineUsers(UUID listId) {
        Map<String, PresenceEntry> sessions = sessionsByList.get(listId);
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }

        Map<UUID, User> uniqueUsers = new LinkedHashMap<>();
        for (PresenceEntry entry : sessions.values()) {
            uniqueUsers.put(entry.user().getId(), entry.user());
        }

        return uniqueUsers.values();
    }

    public void updateHeartbeat(UUID listId, String sessionId) {
        Map<String, PresenceEntry> sessions = sessionsByList.get(listId);
        if (sessions == null) {
            return;
        }

        sessions.computeIfPresent(sessionId, (ignored, previous) -> new PresenceEntry(previous.user(), clock.get()));
    }

    public List<ExpiredPresence> evictExpiredSessions(Duration timeout) {
        Instant threshold = clock.get().minus(timeout);
        List<ExpiredPresence> expired = new ArrayList<>();

        for (Map.Entry<UUID, Map<String, PresenceEntry>> listEntry : sessionsByList.entrySet()) {
            UUID listId = listEntry.getKey();
            Map<String, PresenceEntry> sessions = listEntry.getValue();

            List<String> sessionIdsToRemove = new ArrayList<>();
            for (Map.Entry<String, PresenceEntry> sessionEntry : sessions.entrySet()) {
                PresenceEntry presence = sessionEntry.getValue();
                if (presence.lastHeartbeat().isBefore(threshold)) {
                    sessionIdsToRemove.add(sessionEntry.getKey());
                }
            }

            for (String expiredSessionId : sessionIdsToRemove) {
                Optional<User> maybeUser = removeSession(listId, expiredSessionId);
                maybeUser.ifPresent(user -> expired.add(new ExpiredPresence(listId, user)));
            }
        }

        return expired;
    }

    public int getTotalActiveSessions() {
        return sessionsByList.values().stream()
            .mapToInt(Map::size)
            .sum();
    }
}
