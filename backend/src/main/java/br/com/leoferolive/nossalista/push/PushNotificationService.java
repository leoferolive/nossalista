package br.com.leoferolive.nossalista.push;

import br.com.leoferolive.nossalista.websocket.PresenceService;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.security.Security;
import java.util.List;
import java.util.UUID;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final PushSubscriptionStore subscriptionStore;
    private final PresenceService presenceService;
    private final VapidConfig vapidConfig;
    private final JsonMapper jsonMapper;
    private PushService pushService;

    public PushNotificationService(
        PushSubscriptionStore subscriptionStore,
        PresenceService presenceService,
        VapidConfig vapidConfig,
        JsonMapper jsonMapper
    ) {
        this.subscriptionStore = subscriptionStore;
        this.presenceService = presenceService;
        this.vapidConfig = vapidConfig;
        this.jsonMapper = jsonMapper;
        this.pushService = buildPushService();
    }

    private PushService buildPushService() {
        if (!vapidConfig.isConfigured()) {
            return null;
        }
        try {
            return new PushService(
                vapidConfig.getPublicKey(),
                vapidConfig.getPrivateKey(),
                vapidConfig.getSubject()
            );
        } catch (Exception e) {
            log.warn("Não foi possível inicializar PushService — VAPID inválido: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Envia push notification para um usuário caso ele esteja offline.
     * Se o usuário estiver online (sessão WebSocket ativa), o push é omitido
     * pois ele já receberá a notificação pelo WebSocket.
     */
    public void sendToUser(UUID userId, PushNotificationPayload payload) {
        if (pushService == null) {
            return;
        }

        boolean isOnline = presenceService.isUserConnected(userId);
        if (isOnline) {
            return;
        }

        List<PushSubscription> subscriptions = subscriptionStore.findByUserId(userId);
        String body = serialize(payload);

        for (PushSubscription sub : subscriptions) {
            try {
                Subscription webPushSub = new Subscription(sub.endpoint(), new Subscription.Keys(sub.p256dh(), sub.auth()));
                Notification notification = new Notification(webPushSub, body);
                pushService.send(notification);
            } catch (Exception e) {
                if (isGoneError(e)) {
                    log.info("Push subscription expirada para usuário {}, removendo: {}", userId, sub.endpoint());
                    subscriptionStore.remove(userId, sub.endpoint());
                } else {
                    log.warn("Falha ao enviar push para usuário {}: {}", userId, e.getMessage());
                }
            }
        }
    }

    private String serialize(PushNotificationPayload payload) {
        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            log.error("Falha ao serializar push notification payload: {}", e.getMessage());
            throw new RuntimeException("Falha ao serializar push notification payload", e);
        }
    }

    private boolean isGoneError(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("410") || msg.contains("Gone"));
    }
}
