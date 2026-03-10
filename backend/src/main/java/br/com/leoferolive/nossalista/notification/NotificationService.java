package br.com.leoferolive.nossalista.notification;

import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
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

    public NotificationService(ListMemberRepository listMemberRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.listMemberRepository = listMemberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyListMembers(UUID listId, UUID actorId, String type, Object payload, User actor) {
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
        });
    }
}
