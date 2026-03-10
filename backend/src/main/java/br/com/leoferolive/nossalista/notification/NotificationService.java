package br.com.leoferolive.nossalista.notification;

import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.push.PushNotificationPayload;
import br.com.leoferolive.nossalista.push.PushNotificationService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.websocket.WebSocketActor;
import br.com.leoferolive.nossalista.websocket.WebSocketDestinations;
import br.com.leoferolive.nossalista.websocket.WebSocketMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private final ListMemberRepository listMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    public NotificationService(ListMemberRepository listMemberRepository,
                               SimpMessagingTemplate messagingTemplate,
                               PushNotificationService pushNotificationService) {
        this.listMemberRepository = listMemberRepository;
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
    }

    public void notifyListMembers(UUID listId, UUID actorId, String type, Object payload, User actor) {
        String actorName = actor != null ? actor.getUsername() : "Alguém";

        listMemberRepository.findByListId(listId).forEach(member -> {
            UUID memberId = member.getUser().getId();
            if (memberId.equals(actorId)) return;

            WebSocketMessage msg = WebSocketMessage.builder()
                .listId(listId)
                .channel("notifications")
                .type(type)
                .payload(payload)
                .actor(actor != null ? new WebSocketActor(actor.getId(), actor.getUsername()) : null)
                .build();

            messagingTemplate.convertAndSend(
                WebSocketDestinations.userNotifications(memberId), msg);

            pushNotificationService.sendToUser(memberId,
                new PushNotificationPayload(
                    "NossaLista",
                    buildPushBody(type, actorName),
                    "/favicon.ico",
                    type,
                    "/"
                )
            );
        });
    }

    private String buildPushBody(String type, String actorName) {
        return switch (type) {
            case "ITEM_ADDED" -> actorName + " adicionou um item";
            case "ITEM_UPDATED" -> actorName + " editou um item";
            case "ITEM_REMOVED" -> actorName + " removeu um item";
            case "ITEM_CHECKED" -> actorName + " marcou um item";
            case "LIST_NAME_UPDATED" -> actorName + " renomeou a lista";
            case "MEMBER_JOINED" -> "Um novo membro entrou na lista";
            case "MEMBER_LEFT" -> "Um membro saiu da lista";
            case "MEMBER_REMOVED" -> "Um membro foi removido da lista";
            default -> "Nova atividade na lista";
        };
    }
}
