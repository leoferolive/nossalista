package br.com.leoferolive.nossalista.notification;

import br.com.leoferolive.nossalista.user.domain.User;

import java.util.UUID;

/**
 * Evento de domínio publicado por operações de escrita em itens de lista
 * (adicionar, marcar/desmarcar, atualizar, remover).
 *
 * <p>É publicado <b>dentro</b> da transação de escrita, mas só é consumido
 * <b>após o commit</b> (ver {@link ListItemNotificationEventListener}), para
 * que nenhuma notificação seja disparada para uma mudança que sofreu
 * rollback.
 *
 * @param listId  lista afetada
 * @param actorId usuário que realizou a ação (excluído da notificação)
 * @param type    tipo do evento (ex.: {@code ITEM_ADDED}, {@code ITEM_CHECKED})
 * @param payload corpo da notificação (normalmente um DTO já serializável)
 * @param actor   usuário que realizou a ação, usado para compor a mensagem
 */
public record ListItemNotificationEvent(UUID listId, UUID actorId, String type, Object payload, User actor) {
}
