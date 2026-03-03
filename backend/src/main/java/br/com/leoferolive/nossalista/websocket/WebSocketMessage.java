package br.com.leoferolive.nossalista.websocket;

import java.time.Instant;
import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebSocketMessage)) return false;
        WebSocketMessage that = (WebSocketMessage) o;
        return Objects.equals(type, that.type)
            && Objects.equals(payload, that.payload)
            && Objects.equals(userId, that.userId)
            && Objects.equals(username, that.username)
            && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, payload, userId, username, timestamp);
    }

    @Override
    public String toString() {
        return "WebSocketMessage{" +
            "type='" + type + '\'' +
            ", userId=" + userId +
            ", username='" + username + '\'' +
            ", timestamp=" + timestamp +
            '}';
    }

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
