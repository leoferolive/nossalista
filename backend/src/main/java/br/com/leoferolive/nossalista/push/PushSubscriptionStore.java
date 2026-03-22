package br.com.leoferolive.nossalista.push;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class PushSubscriptionStore {

    private static final int MAX_PER_USER = 5;

    private final Map<UUID, List<PushSubscription>> store = new ConcurrentHashMap<>();

    public void add(UUID userId, PushSubscription subscription) {
        store.compute(userId, (id, existing) -> {
            List<PushSubscription> list = existing != null ? existing : new CopyOnWriteArrayList<>();

            // Evitar duplicatas pelo endpoint
            list.removeIf(s -> s.endpoint().equals(subscription.endpoint()));

            list.add(subscription);

            // Manter apenas as MAX_PER_USER mais recentes (remover as mais antigas)
            if (list.size() > MAX_PER_USER) {
                return new CopyOnWriteArrayList<>(list.subList(list.size() - MAX_PER_USER, list.size()));
            }
            return list;
        });
    }

    public void remove(UUID userId, String endpoint) {
        store.computeIfPresent(userId, (id, list) -> {
            list.removeIf(s -> s.endpoint().equals(endpoint));
            return list.isEmpty() ? null : list;
        });
    }

    public void removeAll(UUID userId) {
        store.remove(userId);
    }

    public List<PushSubscription> findByUserId(UUID userId) {
        List<PushSubscription> result = store.get(userId);
        return result != null ? List.copyOf(result) : List.of();
    }
}
