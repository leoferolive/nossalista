package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpOAuthJwtServiceTest {

    private static final String SIGNING_KEY = "unit-test-signing-key-minimum-32-bytes-for-hmac";
    private static final String RESOURCE = "http://localhost:8080/mcp";

    private McpOAuthProperties properties(Duration accessTokenTtl) {
        McpOAuthProperties properties = new McpOAuthProperties();
        properties.setIssuer("http://localhost:8080");
        properties.setResource(RESOURCE);
        properties.setSigningKey(SIGNING_KEY);
        properties.setAccessTokenTtl(accessTokenTtl);
        properties.setClients(List.of());
        return properties;
    }

    private McpOAuthJwtService newService(Duration ttl) {
        McpOAuthJwtService service = new McpOAuthJwtService(properties(ttl));
        service.validateSigningKey();
        return service;
    }

    @Test
    void validateSigningKeyFailsFastWhenKeyIsBlank() {
        McpOAuthProperties properties = properties(Duration.ofMinutes(30));
        properties.setSigningKey("");
        McpOAuthJwtService service = new McpOAuthJwtService(properties);

        assertThatThrownBy(service::validateSigningKey).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateSigningKeyFailsFastWhenKeyIsTooShort() {
        McpOAuthProperties properties = properties(Duration.ofMinutes(30));
        properties.setSigningKey("too-short");
        McpOAuthJwtService service = new McpOAuthJwtService(properties);

        assertThatThrownBy(service::validateSigningKey).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void issuedTokenValidatesWithMatchingClaims() {
        McpOAuthJwtService service = newService(Duration.ofMinutes(30));
        UUID userId = UUID.randomUUID();

        String token = service.issueAccessToken(userId, "claude-ai", TokenScope.READ_WRITE, RESOURCE);
        Optional<McpOAuthJwtService.McpOAuthAccessTokenClaims> claims = service.validate(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().userId()).isEqualTo(userId);
        assertThat(claims.get().clientId()).isEqualTo("claude-ai");
        assertThat(claims.get().scope()).isEqualTo(TokenScope.READ_WRITE);
    }

    @Test
    void tokenWithWrongAudienceFailsValidation() {
        McpOAuthJwtService service = newService(Duration.ofMinutes(30));
        String token = service.issueAccessToken(UUID.randomUUID(), "claude-ai", TokenScope.READ, "http://other/mcp");

        assertThat(service.validate(token)).isEmpty();
    }

    @Test
    void expiredTokenFailsValidation() throws InterruptedException {
        McpOAuthJwtService service = newService(Duration.ofMillis(1));
        String token = service.issueAccessToken(UUID.randomUUID(), "claude-ai", TokenScope.READ, RESOURCE);

        Thread.sleep(50);

        assertThat(service.validate(token)).isEmpty();
    }

    @Test
    void tokenSignedWithDifferentKeyFailsValidation() {
        McpOAuthJwtService issuer = newService(Duration.ofMinutes(30));
        String token = issuer.issueAccessToken(UUID.randomUUID(), "claude-ai", TokenScope.READ, RESOURCE);

        McpOAuthProperties otherProperties = properties(Duration.ofMinutes(30));
        otherProperties.setSigningKey("a-completely-different-signing-key-of-32-bytes-plus");
        McpOAuthJwtService otherService = new McpOAuthJwtService(otherProperties);

        assertThat(otherService.validate(token)).isEmpty();
    }

    @Test
    void garbageTokenFailsValidation() {
        McpOAuthJwtService service = newService(Duration.ofMinutes(30));
        assertThat(service.validate("not-a-jwt")).isEmpty();
    }
}
