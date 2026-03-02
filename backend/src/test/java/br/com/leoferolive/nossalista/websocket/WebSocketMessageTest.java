package br.com.leoferolive.nossalista.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("Deve serializar campos obrigatórios: type, payload, userId, username, timestamp")
    void shouldSerializeAllRequiredFields() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Map<String, String> payload = Map.of("id", "abc", "name", "Item Teste");

        WebSocketMessage message = WebSocketMessage.builder()
                .type("ITEM_ADDED")
                .payload(payload)
                .userId(userId)
                .username("testuser")
                .timestamp(now)
                .build();

        String json = objectMapper.writeValueAsString(message);
        Map<?, ?> parsed = objectMapper.readValue(json, Map.class);

        assertTrue(parsed.containsKey("type"), "Campo 'type' deve estar presente");
        assertTrue(parsed.containsKey("payload"), "Campo 'payload' deve estar presente");
        assertTrue(parsed.containsKey("userId"), "Campo 'userId' deve estar presente");
        assertTrue(parsed.containsKey("username"), "Campo 'username' deve estar presente");
        assertTrue(parsed.containsKey("timestamp"), "Campo 'timestamp' deve estar presente");
    }

    @Test
    @DisplayName("Deve serializar timestamp como string ISO 8601 (não como número)")
    void shouldSerializeTimestampAsIso8601String() throws Exception {
        Instant now = Instant.parse("2026-03-01T22:00:00Z");

        WebSocketMessage message = WebSocketMessage.builder()
                .type("ITEM_ADDED")
                .payload(Map.of("name", "Item"))
                .userId(UUID.randomUUID())
                .username("user")
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
                .type("ITEM_REMOVED")
                .payload(Map.of())
                .userId(UUID.randomUUID())
                .username("user")
                .timestamp(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(message);
        assertTrue(json.contains("\"type\":\"ITEM_REMOVED\""), "type deve ser 'ITEM_REMOVED'");
    }

    @Test
    @DisplayName("Getters devem retornar valores corretos")
    void shouldReturnCorrectValuesFromGetters() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        String payload = "test-payload";

        WebSocketMessage message = WebSocketMessage.builder()
                .type("ITEM_CHECKED")
                .payload(payload)
                .userId(userId)
                .username("testuser")
                .timestamp(now)
                .build();

        assertEquals("ITEM_CHECKED", message.getType());
        assertEquals(payload, message.getPayload());
        assertEquals(userId, message.getUserId());
        assertEquals("testuser", message.getUsername());
        assertEquals(now, message.getTimestamp());
    }
}
