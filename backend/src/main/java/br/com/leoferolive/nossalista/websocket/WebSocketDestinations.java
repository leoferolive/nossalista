package br.com.leoferolive.nossalista.websocket;

import java.util.UUID;

public final class WebSocketDestinations {

    public static final String LIST_TOPIC_PREFIX = "/topic/list/";
    public static final String ITEMS_SUFFIX = "/items";
    public static final String PRESENCE_SUFFIX = "/presence";
    public static final String USER_TOPIC_PREFIX = "/topic/user/";

    private WebSocketDestinations() {
    }

    public static String listItems(UUID listId) {
        return LIST_TOPIC_PREFIX + listId + ITEMS_SUFFIX;
    }

    public static String listPresence(UUID listId) {
        return LIST_TOPIC_PREFIX + listId + PRESENCE_SUFFIX;
    }

    public static String userNotifications(UUID userId) {
        return USER_TOPIC_PREFIX + userId + "/notifications";
    }
}
