package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthCode;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRefreshToken;
import br.com.leoferolive.nossalista.mcpoauth.dto.TokenResponse;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthTokenException;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthCodeRepository;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Endpoint de token OAuth: troca authorization code (+ PKCE) por tokens,
 * rotaciona refresh tokens com detecção de reuso, e revoga tokens/famílias.
 *
 * <p>Refresh tokens seguem o mesmo padrão hash-only de {@code PersonalAccessToken}
 * (D-018): só o SHA-256 é persistido, o valor em claro é devolvido uma única vez
 * na resposta do token endpoint.</p>
 */
@Service
public class McpOAuthTokenService {

    /** 32 bytes = 256 bits de entropia para o segredo do refresh token. */
    private static final int REFRESH_TOKEN_ENTROPY_BYTES = 32;

    private final McpOAuthProperties properties;
    private final McpOAuthClientRegistry clientRegistry;
    private final McpOAuthCodeRepository codeRepository;
    private final McpOAuthRefreshTokenRepository refreshTokenRepository;
    private final McpOAuthJwtService jwtService;
    private final PkceValidator pkceValidator;
    private final SecureRandom secureRandom = new SecureRandom();

    public McpOAuthTokenService(
        McpOAuthProperties properties,
        McpOAuthClientRegistry clientRegistry,
        McpOAuthCodeRepository codeRepository,
        McpOAuthRefreshTokenRepository refreshTokenRepository,
        McpOAuthJwtService jwtService,
        PkceValidator pkceValidator
    ) {
        this.properties = properties;
        this.clientRegistry = clientRegistry;
        this.codeRepository = codeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.pkceValidator = pkceValidator;
    }

    /**
     * {@code grant_type=authorization_code}: troca o code + PKCE verifier por
     * um par access/refresh token.
     *
     * <p>Um code JÁ CONSUMIDO reenviado (replay) revoga toda a família de
     * refresh tokens que ele originou, em vez de apenas falhar — sinal de que o
     * code pode ter vazado (ex.: interceptado no redirect).</p>
     *
     * <p>{@code noRollbackFor}: a revogação da família no ramo de replay precisa
     * ser PERSISTIDA mesmo o método terminando em exceção — sem isso, o rollback
     * padrão do Spring para {@link RuntimeException} desfaria a própria
     * revogação de segurança que este método acabou de executar.</p>
     */
    @Transactional(noRollbackFor = OAuthTokenException.class)
    public TokenResponse exchangeAuthorizationCode(
        String code, String redirectUri, String clientId, String codeVerifier, String resource
    ) {
        clientRegistry.require(clientId);

        McpOAuthCode entity = codeRepository.findByCode(code)
            .orElseThrow(() -> OAuthTokenException.invalidGrant("Unknown authorization code."));

        rejectIfReplayed(entity);
        LocalDateTime now = LocalDateTime.now();
        validateCodeAgainstRequest(entity, redirectUri, clientId, resource, now);
        if (!pkceValidator.matches(codeVerifier, entity.getCodeChallenge())) {
            throw OAuthTokenException.invalidGrant("code_verifier does not match code_challenge.");
        }

        UUID familyId = UUID.randomUUID();
        entity.setConsumedAt(now);
        entity.setTokenFamilyId(familyId);
        codeRepository.save(entity);

        return issueTokenPair(entity.getUserId(), entity.getClientId(), entity.getScope(), entity.getResource(), familyId);
    }

    /**
     * Um code JÁ CONSUMIDO reenviado é replay: revoga a família de tokens que
     * ele originou (se houver) e rejeita.
     */
    private void rejectIfReplayed(McpOAuthCode entity) {
        if (!entity.isConsumed()) {
            return;
        }
        if (entity.getTokenFamilyId() != null) {
            refreshTokenRepository.revokeFamily(entity.getTokenFamilyId(), LocalDateTime.now());
        }
        throw OAuthTokenException.invalidGrant(
            "Authorization code already used. This is a replay attempt — issued tokens were revoked.");
    }

    private void validateCodeAgainstRequest(
        McpOAuthCode entity, String redirectUri, String clientId, String resource, LocalDateTime now
    ) {
        if (entity.isExpired(now)) {
            throw OAuthTokenException.invalidGrant("Authorization code expired.");
        }
        if (!entity.getClientId().equals(clientId)) {
            throw OAuthTokenException.invalidGrant("client_id does not match the authorization code.");
        }
        if (!entity.getRedirectUri().equals(redirectUri)) {
            throw OAuthTokenException.invalidGrant("redirect_uri does not match the authorization code.");
        }
        validateResourceMatches(resource, entity.getResource());
    }

    /**
     * O {@code resource} (RFC 8707) do pedido, quando informado, precisa bater
     * com o vinculado ao code/refresh token original.
     */
    private void validateResourceMatches(String requestedResource, String boundResource) {
        boolean mismatch = requestedResource != null && !requestedResource.equals(boundResource);
        if (mismatch) {
            throw OAuthTokenException.invalidTarget("resource does not match the authorization request.");
        }
    }

    /**
     * {@code grant_type=refresh_token}: rotaciona o refresh token (revoga o
     * atual, emite um novo na MESMA família) e emite um novo access token.
     *
     * <p>Reapresentar um refresh token JÁ revogado (por rotação anterior ou por
     * detecção de reuso prévia) revoga a família inteira — reuso é o sinal
     * clássico de vazamento de refresh token (RFC 6749 §10.4).</p>
     *
     * <p>{@code noRollbackFor}: mesmo motivo de {@link #exchangeAuthorizationCode} —
     * a revogação da família no ramo de reuso precisa sobreviver à exceção lançada
     * em seguida.</p>
     */
    @Transactional(noRollbackFor = OAuthTokenException.class)
    public TokenResponse refresh(String refreshToken, String clientId, String resource) {
        clientRegistry.require(clientId);

        String hash = sha256Hex(refreshToken);
        McpOAuthRefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> OAuthTokenException.invalidGrant("Unknown refresh token."));

        LocalDateTime now = LocalDateTime.now();
        rejectIfReused(existing, now);
        validateRefreshTokenAgainstRequest(existing, clientId, resource, now);

        existing.setRevokedAt(now);
        existing.setLastUsedAt(now);
        refreshTokenRepository.save(existing);

        return issueTokenPair(
            existing.getUserId(), existing.getClientId(), existing.getScope(), existing.getResource(),
            existing.getFamilyId());
    }

    /**
     * Reapresentar um refresh token JÁ revogado (rotação anterior ou reuso
     * prévio) é o sinal clássico de vazamento: revoga a família inteira.
     */
    private void rejectIfReused(McpOAuthRefreshToken existing, LocalDateTime now) {
        if (existing.getRevokedAt() == null) {
            return;
        }
        refreshTokenRepository.revokeFamily(existing.getFamilyId(), now);
        throw OAuthTokenException.invalidGrant("Refresh token reuse detected. Token family revoked.");
    }

    private void validateRefreshTokenAgainstRequest(
        McpOAuthRefreshToken existing, String clientId, String resource, LocalDateTime now
    ) {
        if (existing.getExpiresAt().isBefore(now)) {
            throw OAuthTokenException.invalidGrant("Refresh token expired.");
        }
        if (!existing.getClientId().equals(clientId)) {
            throw OAuthTokenException.invalidGrant("client_id does not match refresh token.");
        }
        validateResourceMatches(resource, existing.getResource());
    }

    /**
     * {@code POST /oauth/revoke} (RFC 7009): revoga a família inteira do
     * refresh token informado. Idempotente e silencioso em token
     * desconhecido/já revogado — a RFC exige que o endpoint nunca revele se um
     * token era válido.
     */
    @Transactional
    public void revoke(String token, String clientId) {
        if (token == null || token.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(sha256Hex(token)).ifPresent(rt -> {
            if (clientId == null || clientId.equals(rt.getClientId())) {
                refreshTokenRepository.revokeFamily(rt.getFamilyId(), LocalDateTime.now());
            }
        });
    }

    private TokenResponse issueTokenPair(UUID userId, String clientId, TokenScope scope, String resource, UUID familyId) {
        String accessToken = jwtService.issueAccessToken(userId, clientId, scope, resource);

        String rawRefreshSecret = generateRefreshTokenSecret();
        McpOAuthRefreshToken refresh = new McpOAuthRefreshToken();
        refresh.setTokenHash(sha256Hex(rawRefreshSecret));
        refresh.setFamilyId(familyId);
        refresh.setUserId(userId);
        refresh.setClientId(clientId);
        refresh.setScope(scope);
        refresh.setResource(resource);
        refresh.setExpiresAt(LocalDateTime.now().plus(properties.getRefreshTokenTtl()));
        refreshTokenRepository.save(refresh);

        return TokenResponse.of(
            accessToken, jwtService.accessTokenTtl().toSeconds(), rawRefreshSecret, scope.name().toLowerCase(Locale.ROOT));
    }

    private String generateRefreshTokenSecret() {
        byte[] bytes = new byte[REFRESH_TOKEN_ENTROPY_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }
}
