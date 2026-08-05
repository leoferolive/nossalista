package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.websocket.dto.PresenceSnapshotPayload;
import br.com.leoferolive.nossalista.websocket.dto.MemberOfflinePayload;
import br.com.leoferolive.nossalista.websocket.dto.MemberOnlinePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenceEventListener Tests")
class PresenceEventListenerTest {

    @Mock
    private PresenceService presenceService;

    @Mock
    private WebSocketEventPublisher eventPublisher;

    private PresenceEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PresenceEventListener(presenceService, eventPublisher);
    }

    @Test
    @DisplayName("SUBSCRIBE em /topic/list/{id}/presence registra sessão e envia snapshot + MEMBER_ONLINE")
    void subscribeListTopicShouldRegisterAndBroadcastOnline() {
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/list/" + listId + "/presence");
        accessor.setSessionId("session-1");
        accessor.setSubscriptionId("sub-1");
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);
        when(presenceService.getOnlineUsers(listId)).thenReturn(List.of(user));

        listener.handleSubscribe(event);

        verify(presenceService).registerSession(listId, "session-1", user);
        verify(presenceService).registerSubscription("sub-1", listId, "session-1");
        verify(presenceService).getOnlineUsers(listId);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<User> actorCaptor = ArgumentCaptor.forClass(User.class);
        verify(eventPublisher).publishPresenceEvent(eq(listId), typeCaptor.capture(), payloadCaptor.capture(), actorCaptor.capture());

        assertThat(typeCaptor.getValue()).isEqualTo("MEMBER_ONLINE");
        assertThat(payloadCaptor.getValue()).isInstanceOf(MemberOnlinePayload.class);
        MemberOnlinePayload payload = (MemberOnlinePayload) payloadCaptor.getValue();
        assertThat(payload.userId()).isEqualTo(user.getId().toString());
        assertThat(payload.username()).isEqualTo("maria");
        assertThat(actorCaptor.getValue()).isEqualTo(user);

        verify(eventPublisher).publishPresenceEvent(eq(listId), eq("PRESENCE_SNAPSHOT"), any(PresenceSnapshotPayload.class));
    }

    @Test
    @DisplayName("DISCONNECT remove de todas as listas e envia MEMBER_OFFLINE")
    void disconnectShouldRemoveSessionAndBroadcastOffline() {
        User user = createUser("maria");
        UUID listId = UUID.randomUUID();
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn("session-1");
        when(presenceService.removeSessionAllLists("session-1"))
            .thenReturn(List.of(new PresenceService.RemovedPresence(listId, user)));

        listener.handleDisconnect(event);

        verify(presenceService).removeSessionAllLists("session-1");
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishPresenceEvent(eq(listId), eq("MEMBER_OFFLINE"), payloadCaptor.capture(), eq(user));
        MemberOfflinePayload payload = (MemberOfflinePayload) payloadCaptor.getValue();
        assertThat(payload.userId()).isEqualTo(user.getId().toString());
    }

    @Test
    @DisplayName("SUBSCRIBE em destino fora de /topic/list/{id} não aciona PresenceService")
    void subscribeOtherDestinationShouldDoNothing() {
        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/notifications");
        accessor.setSessionId("session-1");
        accessor.setSubscriptionId("sub-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);

        listener.handleSubscribe(event);

        verify(presenceService, never()).registerSession(any(), any(), any());
        verify(eventPublisher, never()).publishPresenceEvent(any(), any(String.class), any(), any());
        verify(eventPublisher, never()).publishPresenceEvent(any(), any(String.class), any());
    }

    @Test
    @DisplayName("UNSUBSCRIBE remove sessão e envia MEMBER_OFFLINE")
    void unsubscribeShouldRemoveAndBroadcastOffline() {
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        SessionUnsubscribeEvent event = mock(SessionUnsubscribeEvent.class);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSubscriptionId("sub-1");
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);
        when(presenceService.removeBySubscription("sub-1", "session-1"))
            .thenReturn(new PresenceService.RemovedPresence(listId, user));

        listener.handleUnsubscribe(event);

        verify(presenceService).removeBySubscription("sub-1", "session-1");
        verify(eventPublisher).publishPresenceEvent(eq(listId), eq("MEMBER_OFFLINE"), any(MemberOfflinePayload.class), eq(user));
    }

    @Test
    @DisplayName("SUBSCRIBE em /topic/list/{id}/items (canal de itens) não aciona PresenceService")
    void subscribeItemsTopicShouldDoNothing() {
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/list/" + listId + "/items");
        accessor.setSessionId("session-1");
        accessor.setSubscriptionId("sub-1");
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);

        listener.handleSubscribe(event);

        verify(presenceService, never()).registerSession(any(), any(), any());
    }

    @Test
    @DisplayName("SUBSCRIBE em /topic/list/{id} (tópico raiz, sem sufixo) registra presença")
    void subscribeBareListTopicShouldRegister() {
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/list/" + listId);
        accessor.setSessionId("session-1");
        accessor.setSubscriptionId("sub-1");
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);
        when(presenceService.getOnlineUsers(listId)).thenReturn(List.of(user));

        listener.handleSubscribe(event);

        verify(presenceService).registerSession(listId, "session-1", user);
    }

    @Test
    @DisplayName("SUBSCRIBE com UUID inválido no destino não aciona PresenceService")
    void subscribeInvalidListIdShouldDoNothing() {
        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/list/not-a-valid-uuid/presence");
        accessor.setSessionId("session-1");
        accessor.setSubscriptionId("sub-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);

        listener.handleSubscribe(event);

        verify(presenceService, never()).registerSession(any(), any(), any());
    }

    @Test
    @DisplayName("SUBSCRIBE sem sessionId não aciona PresenceService")
    void subscribeWithoutSessionIdShouldDoNothing() {
        UUID listId = UUID.randomUUID();
        User user = createUser("maria");
        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/list/" + listId + "/presence");
        accessor.setSubscriptionId("sub-1");
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);

        listener.handleSubscribe(event);

        verify(presenceService, never()).registerSession(any(), any(), any());
    }

    @Test
    @DisplayName("SUBSCRIBE sem usuário autenticado não aciona PresenceService")
    void subscribeWithoutAuthenticatedUserShouldDoNothing() {
        UUID listId = UUID.randomUUID();
        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/list/" + listId + "/presence");
        accessor.setSessionId("session-1");
        accessor.setSubscriptionId("sub-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);

        listener.handleSubscribe(event);

        verify(presenceService, never()).registerSession(any(), any(), any());
    }

    @Test
    @DisplayName("UNSUBSCRIBE sem subscriptionId não aciona PresenceService")
    void unsubscribeWithoutSubscriptionIdShouldDoNothing() {
        SessionUnsubscribeEvent event = mock(SessionUnsubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);

        listener.handleUnsubscribe(event);

        verify(presenceService, never()).removeBySubscription(any(), any());
    }

    @Test
    @DisplayName("UNSUBSCRIBE de inscrição desconhecida não publica MEMBER_OFFLINE")
    void unsubscribeWithUnknownSubscriptionShouldNotBroadcast() {
        SessionUnsubscribeEvent event = mock(SessionUnsubscribeEvent.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSubscriptionId("sub-unknown");
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(event.getMessage()).thenReturn(message);
        when(presenceService.removeBySubscription("sub-unknown", "session-1")).thenReturn(null);

        listener.handleUnsubscribe(event);

        verify(eventPublisher, never()).publishPresenceEvent(any(), eq("MEMBER_OFFLINE"), any(), any());
    }

    @Test
    @DisplayName("DISCONNECT sem sessionId não aciona PresenceService")
    void disconnectWithoutSessionIdShouldDoNothing() {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn(null);

        listener.handleDisconnect(event);

        verify(presenceService, never()).removeSessionAllLists(any());
    }

    @Test
    @DisplayName("DISCONNECT sem sessões removidas não publica nenhum evento")
    void disconnectWithNoRemovedSessionsShouldNotBroadcast() {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn("session-1");
        when(presenceService.removeSessionAllLists("session-1")).thenReturn(List.of());

        listener.handleDisconnect(event);

        verify(eventPublisher, never()).publishPresenceEvent(any(), any(String.class), any(), any());
    }

    private User createUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setName(username);
        return user;
    }
}
