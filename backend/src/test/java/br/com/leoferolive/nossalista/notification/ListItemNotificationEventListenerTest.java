package br.com.leoferolive.nossalista.notification;

import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Testa o listener isoladamente (sem Spring): garante que ele delega
 * corretamente para {@link NotificationService} e que exceções do
 * NotificationService são contidas (logadas, não propagadas) — já que este
 * método roda de forma assíncrona, muito depois da requisição original ter
 * retornado.
 *
 * O comportamento de "só dispara após commit" e "roda fora da thread da
 * requisição" (dado pelas anotações {@code @TransactionalEventListener} e
 * {@code @Async}) é coberto pelo teste de integração
 * {@code ListItemNotificationAfterCommitIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListItemNotificationEventListener Tests")
class ListItemNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    private ListItemNotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ListItemNotificationEventListener(notificationService);
    }

    @Test
    @DisplayName("Deve delegar para NotificationService com os dados do evento")
    void shouldDelegateToNotificationServiceWithEventData() {
        UUID listId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        User actor = new User();
        actor.setId(actorId);
        Object payload = new Object();

        ListItemNotificationEvent event = new ListItemNotificationEvent(listId, actorId, "ITEM_ADDED", payload, actor);

        listener.onListItemNotification(event);

        verify(notificationService).notifyListMembers(listId, actorId, "ITEM_ADDED", payload, actor);
    }

    @Test
    @DisplayName("Não deve propagar exceção quando NotificationService falha (best-effort)")
    void shouldNotPropagateExceptionWhenNotificationServiceFails() {
        UUID listId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ListItemNotificationEvent event = new ListItemNotificationEvent(listId, actorId, "ITEM_ADDED", "payload", null);

        doThrow(new RuntimeException("falha simulada"))
                .when(notificationService).notifyListMembers(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> listener.onListItemNotification(event));
    }
}
