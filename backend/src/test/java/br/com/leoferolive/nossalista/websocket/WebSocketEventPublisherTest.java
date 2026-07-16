package br.com.leoferolive.nossalista.websocket;

import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketEventPublisher Tests")
class WebSocketEventPublisherTest {

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    private WebSocketEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new WebSocketEventPublisher(simpMessagingTemplate);
    }

    @Test
    @DisplayName("publishEvent com actor e revisão envia para o tópico de items com actor e revisão preenchidos")
    void publishEventWithActorAndRevisionSendsToItemsTopic() {
        UUID listId = UUID.randomUUID();
        User actor = createUser("maria");
        String payload = "payload";

        publisher.publishEvent(listId, "ITEM_ADDED", payload, actor, 7L);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(simpMessagingTemplate).convertAndSend(destinationCaptor.capture(), messageCaptor.capture());

        assertThat(destinationCaptor.getValue()).isEqualTo(WebSocketDestinations.listItems(listId));
        WebSocketMessage message = messageCaptor.getValue();
        assertThat(message.getListId()).isEqualTo(listId);
        assertThat(message.getChannel()).isEqualTo("items");
        assertThat(message.getType()).isEqualTo("ITEM_ADDED");
        assertThat(message.getPayload()).isEqualTo(payload);
        assertThat(message.getRevision()).isEqualTo(7L);
        assertThat(message.getActor()).isNotNull();
        assertThat(message.getActor().id()).isEqualTo(actor.getId());
        assertThat(message.getActor().username()).isEqualTo("maria");
    }

    @Test
    @DisplayName("publishEvent sem revisão explícita usa o overload de 4 argumentos e não define actor quando nulo")
    void publishEventWithoutRevisionOmitsActorWhenNull() {
        UUID listId = UUID.randomUUID();
        String payload = "payload";

        publisher.publishEvent(listId, "ITEM_REMOVED", payload, null);

        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(simpMessagingTemplate).convertAndSend(eq(WebSocketDestinations.listItems(listId)), messageCaptor.capture());

        WebSocketMessage message = messageCaptor.getValue();
        assertThat(message.getRevision()).isNull();
        assertThat(message.getActor()).isNull();
    }

    @Test
    @DisplayName("publishPresenceEvent com actor envia para o tópico de presence")
    void publishPresenceEventWithActorSendsToPresenceTopic() {
        UUID listId = UUID.randomUUID();
        User actor = createUser("joao");

        publisher.publishPresenceEvent(listId, "MEMBER_ONLINE", "payload", actor);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(simpMessagingTemplate).convertAndSend(destinationCaptor.capture(), messageCaptor.capture());

        assertThat(destinationCaptor.getValue()).isEqualTo(WebSocketDestinations.listPresence(listId));
        assertThat(messageCaptor.getValue().getChannel()).isEqualTo("presence");
        assertThat(messageCaptor.getValue().getActor().username()).isEqualTo("joao");
    }

    @Test
    @DisplayName("publishPresenceEvent sem actor envia mensagem sem actor")
    void publishPresenceEventWithoutActorSendsMessageWithoutActor() {
        UUID listId = UUID.randomUUID();

        publisher.publishPresenceEvent(listId, "PRESENCE_SNAPSHOT", "payload");

        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(simpMessagingTemplate).convertAndSend(eq(WebSocketDestinations.listPresence(listId)), messageCaptor.capture());

        assertThat(messageCaptor.getValue().getActor()).isNull();
        assertThat(messageCaptor.getValue().getChannel()).isEqualTo("presence");
    }

    private User createUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        return user;
    }
}
