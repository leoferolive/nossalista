package br.com.leoferolive.nossalista.notification;

import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.push.PushNotificationService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.websocket.WebSocketDestinations;
import br.com.leoferolive.nossalista.websocket.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private ListMemberRepository listMemberRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private PushNotificationService pushNotificationService;

    private NotificationService notificationService;

    private User actor;
    private User member1;
    private User member2;
    private UUID listId;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(listMemberRepository, messagingTemplate, pushNotificationService);

        listId = UUID.randomUUID();

        actor = new User();
        actor.setId(UUID.randomUUID());
        actor.setUsername("actor");

        member1 = new User();
        member1.setId(UUID.randomUUID());
        member1.setUsername("member1");

        member2 = new User();
        member2.setId(UUID.randomUUID());
        member2.setUsername("member2");
    }

    @Test
    @DisplayName("Deve publicar notificação para todos os membros exceto o ator")
    void shouldPublishToAllMembersExceptActor() {
        ListMember actorMembership = createMembership(actor, MemberRole.OWNER);
        ListMember m1Membership = createMembership(member1, MemberRole.MEMBER);
        ListMember m2Membership = createMembership(member2, MemberRole.MEMBER);

        when(listMemberRepository.findByListId(listId))
            .thenReturn(List.of(actorMembership, m1Membership, m2Membership));

        notificationService.notifyListMembers(listId, actor.getId(), "ITEM_ADDED", "payload", actor);

        // Deve enviar para member1 e member2, mas não para o ator
        verify(messagingTemplate, times(2)).convertAndSend(any(String.class), any(Object.class));
        verify(messagingTemplate).convertAndSend(
            eq(WebSocketDestinations.userNotifications(member1.getId())), any(Object.class));
        verify(messagingTemplate).convertAndSend(
            eq(WebSocketDestinations.userNotifications(member2.getId())), any(Object.class));
    }

    @Test
    @DisplayName("Não deve publicar notificação para o ator")
    void shouldNotPublishToActor() {
        ListMember actorMembership = createMembership(actor, MemberRole.OWNER);
        when(listMemberRepository.findByListId(listId)).thenReturn(List.of(actorMembership));

        notificationService.notifyListMembers(listId, actor.getId(), "ITEM_ADDED", "payload", actor);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("Mensagem deve conter tipo e canal notifications")
    void messageShouldContainTypeAndNotificationsChannel() {
        ListMember m1Membership = createMembership(member1, MemberRole.MEMBER);
        when(listMemberRepository.findByListId(listId)).thenReturn(List.of(m1Membership));

        notificationService.notifyListMembers(listId, actor.getId(), "ITEM_ADDED", "payload", actor);

        ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());

        WebSocketMessage msg = captor.getValue();
        assertThat(msg.getType()).isEqualTo("ITEM_ADDED");
        assertThat(msg.getChannel()).isEqualTo("notifications");
        assertThat(msg.getListId()).isEqualTo(listId);
    }

    private ListMember createMembership(User user, MemberRole role) {
        ListMember m = new ListMember();
        m.setUser(user);
        m.setRole(role);
        return m;
    }
}
