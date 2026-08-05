package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.HashMap;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSubscriptionInterceptor Tests")
class WebSocketSubscriptionInterceptorTest {

    @Mock
    private ListMemberRepository listMemberRepository;

    private WebSocketSubscriptionInterceptor interceptor;

    private static final UUID VALID_LIST_ID = UUID.randomUUID();
    private static final UUID VALID_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketSubscriptionInterceptor(listMemberRepository);
    }

    @Test
    @DisplayName("Membro da lista deve conseguir se inscrever no tópico")
    void shouldAllowSubscribeForListMember() {
        User user = createUser(VALID_USER_ID);
        Message<?> message = buildSubscribeMessage("/topic/list/" + VALID_LIST_ID + "/items", user);

        when(listMemberRepository.existsByListIdAndUserId(VALID_LIST_ID, VALID_USER_ID))
            .thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Não-membro da lista deve ter subscribe rejeitado com MessageDeliveryException")
    void shouldRejectSubscribeForNonMember() {
        User user = createUser(VALID_USER_ID);
        Message<?> message = buildSubscribeMessage("/topic/list/" + VALID_LIST_ID + "/presence", user);

        when(listMemberRepository.existsByListIdAndUserId(VALID_LIST_ID, VALID_USER_ID))
            .thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("Acesso negado ao tópico");
    }

    @Test
    @DisplayName("Usuário não autenticado deve ter subscribe rejeitado com MessageDeliveryException")
    void shouldRejectSubscribeForUnauthenticatedUser() {
        Message<?> message = buildSubscribeMessage("/topic/list/" + VALID_LIST_ID + "/items", null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("Usuário não autenticado");
    }

    @Test
    @DisplayName("UUID inválido no destino deve lançar MessageDeliveryException")
    void shouldRejectSubscribeWithInvalidUuidInDestination() {
        User user = createUser(VALID_USER_ID);
        Message<?> message = buildSubscribeMessage("/topic/list/not-a-valid-uuid/items", user);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("Destino de subscribe inválido");
    }

    @Test
    @DisplayName("Usuário deve conseguir subscrever ao próprio tópico de notificações")
    void shouldAllowSubscribeToOwnNotificationsTopic() {
        User user = createUser(VALID_USER_ID);
        Message<?> message = buildSubscribeMessage(
            "/topic/user/" + VALID_USER_ID + "/notifications", user);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Usuário não deve conseguir subscrever ao tópico de notificações de outro usuário")
    void shouldRejectSubscribeToOtherUserNotificationsTopic() {
        UUID otherUserId = UUID.randomUUID();
        User user = createUser(VALID_USER_ID);
        Message<?> message = buildSubscribeMessage(
            "/topic/user/" + otherUserId + "/notifications", user);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("Acesso negado");
    }

    @Test
    @DisplayName("Subscribe em tópico não-lista deve passar sem verificação de membership")
    void shouldAllowSubscribeForNonListTopic() {
        User user = createUser(VALID_USER_ID);
        Message<?> message = buildSubscribeMessage("/topic/other-topic", user);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("CONNECT com usuário do handshake deve preservar a sessão STOMP")
    void shouldAuthenticateConnectWithHandshakeUser() {
        User user = createUser(VALID_USER_ID);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>(Collections.singletonMap("user", user)));
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getSessionAttributes()).containsEntry("user", user);
        assertThat(interceptor.extractUser(resultAccessor)).isEqualTo(user);
    }

    @Test
    @DisplayName("SUBSCRIBE deve aceitar usuário autenticado salvo na sessão STOMP")
    void shouldAllowSubscribeForUserStoredInSessionAttributes() {
        User user = createUser(VALID_USER_ID);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/list/" + VALID_LIST_ID + "/items");
        accessor.setSessionAttributes(Collections.singletonMap("user", user));
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(listMemberRepository.existsByListIdAndUserId(VALID_LIST_ID, VALID_USER_ID))
            .thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("CONNECT sem sessão deve ser rejeitado")
    void shouldRejectConnectWithoutSession() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("Sessão ausente");
    }

    @Test
    @DisplayName("Mensagem não-CONNECT e não-SUBSCRIBE deve passar sem verificação")
    void shouldPassNonConnectNonSubscribeMessages() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("UUID inválido no tópico de notificações do usuário deve ser rejeitado")
    void shouldRejectSubscribeToUserTopicWithInvalidUuid() {
        User user = createUser(VALID_USER_ID);
        Message<?> message = buildSubscribeMessage("/topic/user/not-a-valid-uuid/notifications", user);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("Acesso negado");
    }

    private User createUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private Message<?> buildSubscribeMessage(String destination, User user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (user != null) {
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            accessor.setUser(auth);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
