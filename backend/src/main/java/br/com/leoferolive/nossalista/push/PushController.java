package br.com.leoferolive.nossalista.push;

import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushSubscriptionStore subscriptionStore;
    private final VapidConfig vapidConfig;

    public PushController(PushSubscriptionStore subscriptionStore, VapidConfig vapidConfig) {
        this.subscriptionStore = subscriptionStore;
        this.vapidConfig = vapidConfig;
    }

    @GetMapping("/vapid-public-key")
    public ResponseEntity<String> getVapidPublicKey() {
        if (!vapidConfig.isConfigured()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(vapidConfig.getPublicKey());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(
        @RequestBody PushSubscription subscription,
        @AuthenticationPrincipal User user
    ) {
        subscriptionStore.add(user.getId(), subscription);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(
        @RequestBody UnsubscribeRequest request,
        @AuthenticationPrincipal User user
    ) {
        subscriptionStore.remove(user.getId(), request.endpoint());
        return ResponseEntity.ok().build();
    }

    public record UnsubscribeRequest(String endpoint) {}
}
