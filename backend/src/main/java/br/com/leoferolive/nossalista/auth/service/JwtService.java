package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.user.domain.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * Service para geração e validação de tokens JWT
 */
@Service
public class JwtService {

    /**
     * Tamanho mínimo da chave em bytes exigido pelo HMAC-SHA256 (HS256).
     * RFC 7518 (seção 3.2) exige que a chave tenha pelo menos o tamanho do
     * output do hash — 256 bits / 32 bytes.
     */
    private static final int MIN_SECRET_BYTES = 32;

    /**
     * Placeholder histórico inseguro que ficava no application.yml.
     * Mantido aqui apenas para rejeitar explicitamente caso reapareça.
     */
    private static final String INSECURE_PLACEHOLDER =
        "change-this-secret-key-in-production-minimum-32-characters-for-hs256";

    @Value("${jwt.secret:}")
    private String secretKey;

    @Value("${jwt.expiration:604800000}") // 7 dias em milissegundos
    private long expirationMs;

    /**
     * Valida o secret JWT na inicialização da aplicação (fail-fast).
     *
     * <p>Lança {@link IllegalStateException} se o secret estiver ausente, for o
     * placeholder inseguro ou for curto demais para HS256 (&lt; 32 bytes).
     * Isso impede que a aplicação suba com um secret fraco/default, fechando
     * a brecha de assinatura de tokens com chave conhecida.</p>
     *
     * @throws IllegalStateException se {@code JWT_SECRET} for inválido
     */
    @PostConstruct
    void validateSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET ausente: defina a variável de ambiente JWT_SECRET com "
                    + "pelo menos " + MIN_SECRET_BYTES + " bytes (256 bits) para HS256. "
                    + "A aplicação não sobe sem um secret válido.");
        }
        if (INSECURE_PLACEHOLDER.equals(secretKey)) {
            throw new IllegalStateException(
                "JWT_SECRET está usando o placeholder inseguro de exemplo. "
                    + "Gere um secret aleatório de pelo menos " + MIN_SECRET_BYTES + " bytes "
                    + "e configure-o via variável de ambiente JWT_SECRET.");
        }
        int secretBytes = secretKey.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET curto demais para HS256: " + secretBytes + " bytes "
                    + "(mínimo " + MIN_SECRET_BYTES + " bytes / 256 bits). "
                    + "Gere um secret mais longo e configure-o via JWT_SECRET.");
        }
    }

    /**
     * Obtém a chave de assinatura a partir da chave secreta configurada
     *
     * @return chave de assinatura HMAC SHA-256
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gera um token JWT para o usuário autenticado
     *
     * @param user usuário para o qual gerar o token
     * @return token JWT assinado
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("username", user.getUsername())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Valida a assinatura e expiração de um token JWT
     *
     * @param token token JWT a validar
     * @return true se o token é válido, false caso contrário
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extrai o ID do usuário (subject) do token JWT
     *
     * @param token token JWT
     * @return UUID do usuário
     */
    public UUID extractUserId(String token) {
        String subject = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
        return UUID.fromString(subject);
    }

    /**
     * Calcula a data/hora de expiração para novos tokens
     *
     * @return LocalDateTime representando quando o token expirará
     */
    public LocalDateTime getExpirationTime() {
        long expiryTimestamp = System.currentTimeMillis() + expirationMs;
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(expiryTimestamp),
            ZoneId.systemDefault()
        );
    }
}
