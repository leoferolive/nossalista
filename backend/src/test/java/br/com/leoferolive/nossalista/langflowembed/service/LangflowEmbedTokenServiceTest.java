package br.com.leoferolive.nossalista.langflowembed.service;

import br.com.leoferolive.nossalista.langflowembed.LangflowEmbedTestKeys;
import br.com.leoferolive.nossalista.langflowembed.config.LangflowEmbedProperties;
import br.com.leoferolive.nossalista.langflowembed.dto.LangflowJwksResponse;
import br.com.leoferolive.nossalista.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LangflowEmbedTokenService")
class LangflowEmbedTokenServiceTest {

    private static final String ISSUER = "https://nossalista-test.example.com.br";
    private static final String AUDIENCE = "langflow";
    private static final KeyPair KEYS = LangflowEmbedTestKeys.generate(2048);
    private static final KeyPair OTHER_KEYS = LangflowEmbedTestKeys.generate(2048);

    private static LangflowJwksResponse jwks;

    @BeforeAll
    static void startKeyService() {
        jwks = keyService(KEYS).jwks();
    }

    private static LangflowEmbedKeyService keyService(KeyPair pair) {
        LangflowEmbedProperties properties = new LangflowEmbedProperties();
        properties.setEnabled(true);
        properties.setIssuer(ISSUER);
        properties.setPrivateKeyPem(LangflowEmbedTestKeys.pkcs8Pem(pair.getPrivate()));
        LangflowEmbedKeyService service = new LangflowEmbedKeyService(properties);
        service.loadKeysWhenEnabled();
        return service;
    }

    private static LangflowEmbedProperties properties(Duration tokenTtl) {
        LangflowEmbedProperties properties = new LangflowEmbedProperties();
        properties.setEnabled(true);
        properties.setIssuer(ISSUER);
        properties.setAudience(AUDIENCE);
        properties.setTokenTtl(tokenTtl);
        return properties;
    }

    private static User user(String name) {
        User user = new User();
        user.setId(UUID.fromString("0b6f2c1e-6d2a-4c3e-9f7a-1a2b3c4d5e6f"));
        user.setUsername("ana.lista");
        user.setEmail("ana@example.com");
        user.setName(name);
        return user;
    }

    private static RSAPublicKey publicKeyFromPublishedJwks() {
        var jwk = jwks.keys().get(0);
        return LangflowEmbedTestKeys.publicKeyFromJwk(jwk.n(), jwk.e());
    }

    private static Jws<Claims> verify(String token, RSAPublicKey publicKey) {
        return verifyAt(token, publicKey, Instant.now());
    }

    private static Jws<Claims> verifyAt(String token, RSAPublicKey publicKey, Instant now) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .requireIssuer(ISSUER)
            .requireAudience(AUDIENCE)
            .clock(() -> Date.from(now))
            .build()
            .parseSignedClaims(token);
    }

    @Test
    @DisplayName("token é RS256, traz kid no header e valida contra a chave publicada no JWKS")
    void tokenVerifiesAgainstPublishedJwks() {
        LangflowEmbedTokenService service = new LangflowEmbedTokenService(
            keyService(KEYS), properties(Duration.ofMinutes(15)));

        String token = service.issue(user("Ana Lista"));

        Jws<Claims> parsed = verify(token, publicKeyFromPublishedJwks());
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("RS256");
        assertThat(parsed.getHeader().getKeyId()).isEqualTo("nl-langflow-1");
    }

    @Test
    @DisplayName("claims seguem o contrato: iss, aud, sub, email, preferred_username, name, iat e exp")
    void claimsMatchContract() {
        LangflowEmbedTokenService service = new LangflowEmbedTokenService(
            keyService(KEYS), properties(Duration.ofMinutes(15)));

        Claims claims = verify(service.issue(user("Ana Lista")), publicKeyFromPublishedJwks()).getPayload();

        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getAudience()).containsExactly(AUDIENCE);
        assertThat(claims.getSubject()).isEqualTo("0b6f2c1e-6d2a-4c3e-9f7a-1a2b3c4d5e6f");
        assertThat(claims.get("email", String.class)).isEqualTo("ana@example.com");
        assertThat(claims.get("preferred_username", String.class)).isEqualTo("ana.lista");
        assertThat(claims.get("name", String.class)).isEqualTo("Ana Lista");
    }

    @Test
    @DisplayName("aud sai como string única no JSON, formato que o Langflow vai ler")
    void audienceIsSerializedAsSingleString() {
        LangflowEmbedTokenService service = new LangflowEmbedTokenService(
            keyService(KEYS), properties(Duration.ofMinutes(15)));

        String payload = new String(Base64.getUrlDecoder().decode(
            service.issue(user("Ana Lista")).split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).contains("\"aud\":\"langflow\"");
    }

    @Test
    @DisplayName("name cai para o username quando o nome de exibição está vazio")
    void nameFallsBackToUsername() {
        LangflowEmbedTokenService service = new LangflowEmbedTokenService(
            keyService(KEYS), properties(Duration.ofMinutes(15)));

        Claims claims = verify(service.issue(user("  ")), publicKeyFromPublishedJwks()).getPayload();

        assertThat(claims.get("name", String.class)).isEqualTo("ana.lista");
    }

    @Test
    @DisplayName("token expirado é rejeitado pela verificação contra o JWKS")
    void expiredTokenIsRejected() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        LangflowEmbedTokenService service = new LangflowEmbedTokenService(
            keyService(KEYS), properties(Duration.ofMinutes(15)), twoHoursAgo);
        String token = service.issue(user("Ana Lista"));

        assertThatThrownBy(() -> verify(token, publicKeyFromPublishedJwks()))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("token assinado por outra chave é rejeitado pela chave publicada")
    void tokenFromOtherKeyIsRejected() {
        LangflowEmbedTokenService otherService = new LangflowEmbedTokenService(
            keyService(OTHER_KEYS), properties(Duration.ofMinutes(15)));
        String token = otherService.issue(user("Ana Lista"));

        assertThatThrownBy(() -> verify(token, publicKeyFromPublishedJwks()))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("iat fica 30 s no passado e exp é agora + token-ttl, não iat + token-ttl")
    void issuedAtIsThirtySecondsBeforeNowAndExpIsRelativeToNow() {
        Instant now = Instant.parse("2026-10-28T12:00:00Z");
        LangflowEmbedTokenService service = new LangflowEmbedTokenService(
            keyService(KEYS), properties(Duration.ofMinutes(15)), Clock.fixed(now, ZoneOffset.UTC));

        String token = service.issue(user("Ana Lista"));

        Claims claims = verifyAt(token, publicKeyFromPublishedJwks(), now.plusSeconds(1)).getPayload();
        assertThat(claims.getIssuedAt()).isEqualTo(Date.from(now.minusSeconds(30)));
        assertThat(claims.getExpiration()).isEqualTo(Date.from(now.plus(Duration.ofMinutes(15))));
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
            .isEqualTo(Duration.ofMinutes(15).plusSeconds(30));
    }
}
