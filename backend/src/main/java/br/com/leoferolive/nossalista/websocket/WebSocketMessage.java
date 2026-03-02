package br.com.leoferolive.nossalista.websocket;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para mensagens broadcast via WebSocket STOMP
 * Formato Event-Type Envelope (AC1)
 */
public class WebSocketMessage {

    private String type;
    private Object payload;
    private UUID userId;
    private String username;
    private Instant timestamp;

    private WebSocketMessage() {}

    public String getType() { return type; }
    public Object getPayload() { return payload; }
    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public Instant getTimestamp() { return timestamp; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final WebSocketMessage instance = new WebSocketMessage();

        public Builder type(String type) { instance.type = type; return this; }
        public Builder payload(Object payload) { instance.payload = payload; return this; }
        public Builder userId(UUID userId) { instance.userId = userId; return this; }
        public Builder username(String username) { instance.username = username; return this; }
        public Builder timestamp(Instant timestamp) { instance.timestamp = timestamp; return this; }

        public WebSocketMessage build() { return instance; }
    }
}
