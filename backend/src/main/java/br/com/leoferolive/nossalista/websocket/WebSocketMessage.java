package br.com.leoferolive.nossalista.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO para mensagens broadcast via WebSocket STOMP
 * Formato Event-Type Envelope (AC1)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {

    private Integer schemaVersion;
    private String eventId;
    private UUID listId;
    private String channel;
    private Long revision;
    private String type;
    private Object payload;
    private WebSocketActor actor;
    private Instant timestamp;

    private WebSocketMessage() {}

    public Integer getSchemaVersion() { return schemaVersion; }
    public String getEventId() { return eventId; }
    public UUID getListId() { return listId; }
    public String getChannel() { return channel; }
    public Long getRevision() { return revision; }
    public String getType() { return type; }
    public Object getPayload() { return payload; }
    public WebSocketActor getActor() { return actor; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebSocketMessage)) return false;
        WebSocketMessage that = (WebSocketMessage) o;
        return Objects.equals(schemaVersion, that.schemaVersion)
            && Objects.equals(eventId, that.eventId)
            && Objects.equals(listId, that.listId)
            && Objects.equals(channel, that.channel)
            && Objects.equals(revision, that.revision)
            && Objects.equals(type, that.type)
            && Objects.equals(payload, that.payload)
            && Objects.equals(actor, that.actor)
            && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, eventId, listId, channel, revision, type, payload, actor, timestamp);
    }

    @Override
    public String toString() {
        return "WebSocketMessage{" +
            "schemaVersion=" + schemaVersion +
            ", eventId='" + eventId + '\'' +
            ", listId=" + listId +
            ", channel='" + channel + '\'' +
            ", revision=" + revision +
            ", " +
            "type='" + type + '\'' +
            ", actor=" + actor +
            ", timestamp=" + timestamp +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final WebSocketMessage instance = new WebSocketMessage();

        public Builder schemaVersion(Integer schemaVersion) { instance.schemaVersion = schemaVersion; return this; }
        public Builder eventId(String eventId) { instance.eventId = eventId; return this; }
        public Builder listId(UUID listId) { instance.listId = listId; return this; }
        public Builder channel(String channel) { instance.channel = channel; return this; }
        public Builder revision(Long revision) { instance.revision = revision; return this; }
        public Builder type(String type) { instance.type = type; return this; }
        public Builder payload(Object payload) { instance.payload = payload; return this; }
        public Builder actor(WebSocketActor actor) { instance.actor = actor; return this; }
        public Builder timestamp(Instant timestamp) { instance.timestamp = timestamp; return this; }

        public WebSocketMessage build() {
            if (instance.schemaVersion == null) {
                instance.schemaVersion = 2;
            }
            if (instance.timestamp == null) {
                instance.timestamp = Instant.now();
            }
            return instance;
        }
    }
}
