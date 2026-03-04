package br.com.leoferolive.nossalista.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para WebSocketMessage — AC1: Formato Event-Type Envelope
 */
@DisplayName("WebSocketMessage - Formato Event-Type Envelope")
class WebSocketMessageTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("Deve serializar campos obrigatórios definidos no contrato compartilhado")
    void shouldSerializeAllRequiredFields() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        Instant now = Instant.now();
        Map<String, String> payload = Map.of("id", "abc", "name", "Item Teste");

        WebSocketMessage message = WebSocketMessage.builder()
                .eventId("event-1")
                .listId(listId)
                .channel("items")
                .revision(1741040400000L)
                .type("ITEM_ADDED")
                .payload(payload)
                .actor(new WebSocketActor(userId, "testuser"))
                .timestamp(now)
                .build();

        String json = objectMapper.writeValueAsString(message);
        Map<?, ?> parsed = objectMapper.readValue(json, Map.class);
        JsonNode contract = objectMapper.readTree(Files.readString(Path.of("..", "contracts", "websocket-envelope-v2.json")));

        for (JsonNode field : contract.get("requiredEnvelopeFields")) {
            assertTrue(parsed.containsKey(field.asText()), "Campo obrigatório ausente: " + field.asText());
        }
        assertTrue(parsed.containsKey("actor"), "Campo 'actor' deve estar presente");
        assertEquals(2, parsed.get("schemaVersion"));
    }

    @Test
    @DisplayName("Deve serializar timestamp como string ISO 8601 (não como número)")
    void shouldSerializeTimestampAsIso8601String() throws Exception {
        Instant now = Instant.parse("2026-03-01T22:00:00Z");

        WebSocketMessage message = WebSocketMessage.builder()
                .eventId("event-2")
                .listId(UUID.randomUUID())
                .channel("items")
                .revision(1741040400000L)
                .type("ITEM_ADDED")
                .payload(Map.of("name", "Item"))
                .actor(new WebSocketActor(UUID.randomUUID(), "user"))
                .timestamp(now)
                .build();

        String json = objectMapper.writeValueAsString(message);
        Map<?, ?> parsed = objectMapper.readValue(json, Map.class);

        Object timestamp = parsed.get("timestamp");
        assertInstanceOf(String.class, timestamp, "timestamp deve ser serializado como String ISO 8601");
        assertTrue(timestamp.toString().contains("2026-03-01"), "timestamp deve conter a data correta");
    }

    @Test
    @DisplayName("Deve serializar type como string exata passada no builder")
    void shouldSerializeTypeCorrectly() throws Exception {
        WebSocketMessage message = WebSocketMessage.builder()
                .eventId("event-3")
                .listId(UUID.randomUUID())
                .channel("items")
                .revision(1741040400001L)
                .type("ITEM_REMOVED")
                .payload(Map.of())
                .actor(new WebSocketActor(UUID.randomUUID(), "user"))
                .timestamp(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(message);
        assertTrue(json.contains("\"type\":\"ITEM_REMOVED\""), "type deve ser 'ITEM_REMOVED'");
    }

    @Test
    @DisplayName("Dois WebSocketMessage iguais devem ser equals e ter mesmo hashCode")
    void shouldBeEqualWhenSameFields() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-03-01T22:00:00Z");
        Map<String, String> payload = Map.of("id", "abc");

        WebSocketMessage m1 = WebSocketMessage.builder()
                .eventId("event-4")
                .listId(UUID.randomUUID())
                .channel("items")
                .revision(1741040400002L)
                .type("ITEM_ADDED").payload(payload)
                .actor(new WebSocketActor(userId, "user")).timestamp(now).build();
        WebSocketMessage m2 = WebSocketMessage.builder()
                .eventId("event-4")
                .listId(m1.getListId())
                .channel("items")
                .revision(1741040400002L)
                .type("ITEM_ADDED").payload(payload)
                .actor(new WebSocketActor(userId, "user")).timestamp(now).build();

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertNotNull(m1.toString());
        assertTrue(m1.toString().contains("ITEM_ADDED"));
    }

    @Test
    @DisplayName("Getters devem retornar valores corretos")
    void shouldReturnCorrectValuesFromGetters() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        String payload = "test-payload";

        WebSocketMessage message = WebSocketMessage.builder()
                .eventId("event-5")
                .listId(UUID.randomUUID())
                .channel("items")
                .revision(1741040400003L)
                .type("ITEM_CHECKED")
                .payload(payload)
                .actor(new WebSocketActor(userId, "testuser"))
                .timestamp(now)
                .build();

        assertEquals("ITEM_CHECKED", message.getType());
        assertEquals(payload, message.getPayload());
        assertEquals("items", message.getChannel());
        assertNotNull(message.getRevision());
        assertNotNull(message.getActor());
        assertEquals(userId, message.getActor().id());
        assertEquals("testuser", message.getActor().username());
        assertEquals(now, message.getTimestamp());
    }

    @Test
    @DisplayName("Não deve serializar campos nulos")
    void shouldNotSerializeNullFields() throws Exception {
        WebSocketMessage message = WebSocketMessage.builder()
                .eventId("event-6")
                .listId(UUID.randomUUID())
                .channel("presence")
                .type("MEMBER_ONLINE")
                .payload(Map.of("event", "presence"))
                .timestamp(Instant.parse("2026-03-01T22:00:00Z"))
                .build();

        String json = objectMapper.writeValueAsString(message);

        assertFalse(json.contains("\"actor\""), "actor nulo não deve aparecer no payload");
    }
}
