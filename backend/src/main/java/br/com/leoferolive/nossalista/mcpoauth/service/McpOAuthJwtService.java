package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite e valida os access tokens JWT do servidor de autorização OAuth do MCP.
 *
 * <p>Assinados com {@code MCP_OAUTH_SIGNING_KEY} — uma chave PRÓPRIA, distinta
 * do {@code JWT_SECRET} usado pelo JWT de sessão do SPA ({@code JwtService}).
 * Chaves separadas garantem que o comprometimento de uma não afeta a outra e
 * que os dois tipos de token nunca são intercambiáveis (um JWT de sessão nunca
 * valida aqui, e vice-versa — a verificação de assinatura falha silenciosamente
 * e cada filtro deixa a requisição seguir para o próximo, ver
 * {@code McpOAuthTokenAuthenticationFilter}).</p>
 */
@Service
public class McpOAuthJwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final McpOAuthProperties properties;

    public McpOAuthJwtService(McpOAuthProperties properties) {
        this.properties = properties;
    }

    /**
     * Valida {@code MCP_OAUTH_SIGNING_KEY} na inicialização (fail-fast), mesmo
     * padrão de {@code JwtService.validateSecret}.
     */
    @PostConstruct
    void validateSigningKey() {
        String key = properties.getSigningKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                "MCP_OAUTH_SIGNING_KEY ausente: defina a variável de ambiente MCP_OAUTH_SIGNING_KEY "
                    + "com pelo menos " + MIN_SECRET_BYTES + " bytes (256 bits) para HS256, DIFERENTE "
                    + "de JWT_SECRET. A aplicação não sobe sem uma chave válida.");
        }
        int keyBytes = key.getBytes(StandardCharsets.UTF_8).length;
        if (keyBytes < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "MCP_OAUTH_SIGNING_KEY curta demais para HS256: " + keyBytes + " bytes "
                    + "(mínimo " + MIN_SECRET_BYTES + " bytes / 256 bits).");
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getSigningKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Emite um access token JWT vinculado ao usuário, cliente, escopo e
     * audiência (resource, RFC 8707) informados.
     */
    public String issueAccessToken(UUID userId, String clientId, TokenScope scope, String resource) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessTokenTtl());

        return Jwts.builder()
            .issuer(properties.getIssuer())
            .subject(userId.toString())
            .audience().add(resource).and()
            .claim("client_id", clientId)
            .claim("scope", scope.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey())
            .compact();
    }

    public Duration accessTokenTtl() {
        return properties.getAccessTokenTtl();
    }

    /**
     * Valida assinatura, expiração e audiência (deve conter o {@code resource}
     * canônico configurado) de um access token OAuth do MCP.
     *
     * @return os claims decodificados, ou {@link Optional#empty()} se o token
     *         for inválido, expirado, ou tiver audiência diferente da canônica
     */
    public Optional<McpOAuthAccessTokenClaims> validate(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }

        if (!claims.getAudience().contains(properties.getResource())) {
            return Optional.empty();
        }

        String scopeClaim = claims.get("scope", String.class);
        String clientId = claims.get("client_id", String.class);
        TokenScope scope;
        try {
            scope = TokenScope.valueOf(scopeClaim);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }

        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }

        return Optional.of(new McpOAuthAccessTokenClaims(userId, clientId, scope));
    }

    public record McpOAuthAccessTokenClaims(UUID userId, String clientId, TokenScope scope) {
    }
}
