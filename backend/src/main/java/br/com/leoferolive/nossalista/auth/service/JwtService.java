package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:604800000}") // 7 dias em milissegundos
    private long expirationMs;

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
