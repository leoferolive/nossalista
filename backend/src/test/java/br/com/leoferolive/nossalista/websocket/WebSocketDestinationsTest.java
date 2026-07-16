package br.com.leoferolive.nossalista.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSocketDestinations Tests")
class WebSocketDestinationsTest {

    @Test
    @DisplayName("listItems monta o tópico de itens da lista")
    void listItemsBuildsItemsTopic() {
        UUID listId = UUID.randomUUID();

        assertThat(WebSocketDestinations.listItems(listId))
            .isEqualTo("/topic/list/" + listId + "/items");
    }

    @Test
    @DisplayName("listPresence monta o tópico de presença da lista")
    void listPresenceBuildsPresenceTopic() {
        UUID listId = UUID.randomUUID();

        assertThat(WebSocketDestinations.listPresence(listId))
            .isEqualTo("/topic/list/" + listId + "/presence");
    }

    @Test
    @DisplayName("userNotifications monta o tópico de notificações do usuário")
    void userNotificationsBuildsNotificationsTopic() {
        UUID userId = UUID.randomUUID();

        assertThat(WebSocketDestinations.userNotifications(userId))
            .isEqualTo("/topic/user/" + userId + "/notifications");
    }

    @Test
    @DisplayName("Construtor privado existe apenas para impedir instanciação da classe utilitária")
    void privateConstructorPreventsInstantiation() throws Exception {
        Constructor<WebSocketDestinations> constructor = WebSocketDestinations.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThat(constructor.newInstance()).isNotNull();
    }
}
