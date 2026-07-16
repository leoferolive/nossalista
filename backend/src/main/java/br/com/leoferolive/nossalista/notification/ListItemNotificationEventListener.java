package br.com.leoferolive.nossalista.notification;

import br.com.leoferolive.nossalista.config.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consome {@link ListItemNotificationEvent} somente <b>após o commit</b> da
 * transação que o publicou ({@link TransactionPhase#AFTER_COMMIT}) e
 * <b>fora</b> da thread da requisição ({@code @Async}).
 *
 * <p>Isso garante duas coisas ao mesmo tempo: (1) nenhum membro é notificado
 * sobre uma mudança que sofreu rollback; (2) o I/O externo de notificação
 * (WebSocket por usuário + push) não bloqueia nem prolonga a transação de
 * escrita nem a resposta HTTP da requisição original.
 */
@Component
public class ListItemNotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ListItemNotificationEventListener.class);

    private final NotificationService notificationService;

    public ListItemNotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async(AsyncConfig.ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onListItemNotification(ListItemNotificationEvent event) {
        try {
            notificationService.notifyListMembers(
                    event.listId(), event.actorId(), event.type(), event.payload(), event.actor());
        } catch (RuntimeException e) {
            // Notificação é best-effort: uma falha aqui não deve derrubar nada
            // (a request já retornou há muito tempo) — apenas logamos.
            log.error("Falha ao notificar membros da lista {} (type={}): {}",
                    event.listId(), event.type(), e.getMessage(), e);
        }
    }
}
