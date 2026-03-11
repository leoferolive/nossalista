package br.com.leoferolive.nossalista.push;

import br.com.leoferolive.nossalista.websocket.PresenceService;
import nl.martijndwars.webpush.PushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushNotificationService Tests")
class PushNotificationServiceTest {

    @Mock
    private PushSubscriptionStore subscriptionStore;

    @Mock
    private PresenceService presenceService;

    @Mock
    private VapidConfig vapidConfig;

    @Mock
    private PushService pushService;

    private PushNotificationService service;
    private final UUID userId = UUID.randomUUID();
    private final PushNotificationPayload payload = new PushNotificationPayload(
        "NossaLista", "Teste", "/favicon.ico", "ITEM_ADDED", "/"
    );

    @BeforeEach
    void setUp() {
        when(vapidConfig.isConfigured()).thenReturn(false);
        service = new PushNotificationService(subscriptionStore, presenceService, vapidConfig);
    }

    private void injectMockPushService() throws Exception {
        Field field = PushNotificationService.class.getDeclaredField("pushService");
        field.setAccessible(true);
        field.set(service, pushService);
    }

    @Test
    @DisplayName("Não deve enviar push quando VAPID não está configurado (pushService nulo)")
    void sendToUser_doesNothing_whenPushServiceIsNull() {
        service.sendToUser(userId, payload);

        verify(subscriptionStore, never()).findByUserId(userId);
    }

    @Test
    @DisplayName("Não deve enviar push quando usuário está online (com pushService configurado)")
    void sendToUser_doesNothing_whenUserIsOnline() throws Exception {
        injectMockPushService();
        when(presenceService.isUserConnected(userId)).thenReturn(true);

        service.sendToUser(userId, payload);

        verify(subscriptionStore, never()).findByUserId(userId);
    }

    @Test
    @DisplayName("Deve buscar subscrições quando usuário está offline e lista está vazia")
    void sendToUser_fetchesSubscriptions_whenUserIsOfflineAndNoSubs() throws Exception {
        injectMockPushService();
        when(presenceService.isUserConnected(userId)).thenReturn(false);
        when(subscriptionStore.findByUserId(userId)).thenReturn(List.of());

        service.sendToUser(userId, payload);

        verify(subscriptionStore).findByUserId(userId);
    }

    @Test
    @DisplayName("sendToUser é no-op quando construído com VAPID inválido (pushService=null)")
    void sendToUser_isNoop_whenBuiltWithInvalidVapid() {
        VapidConfig realConfig = new VapidConfig();
        realConfig.setPublicKey("invalid-public-key");
        realConfig.setPrivateKey("invalid-private-key");
        realConfig.setSubject("mailto:test@test.com");

        PushNotificationService svc = new PushNotificationService(subscriptionStore, presenceService, realConfig);
        svc.sendToUser(userId, payload);

        verify(subscriptionStore, never()).findByUserId(userId);
    }

    @Test
    @DisplayName("Deve tratar exceção de subscrição inválida sem remover (erro não-410)")
    void sendToUser_handlesException_andDoesNotRemove_forNon410Error() throws Exception {
        // A chave "p256dh" não é base64url válido no contexto de chave EC
        // O construtor de Notification lança exceção antes de pushService.send()
        // Essa exceção NÃO contém "410" nem "Gone", então remove() não deve ser chamado
        injectMockPushService();
        PushSubscription sub = new PushSubscription("https://push.example.com/sub", "p256dh", "auth");
        when(presenceService.isUserConnected(userId)).thenReturn(false);
        when(subscriptionStore.findByUserId(userId)).thenReturn(List.of(sub));

        service.sendToUser(userId, payload);

        verify(subscriptionStore, never()).remove(userId, sub.endpoint());
    }
}
