package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.OAuthAuthorizationCode;
import br.com.leoferolive.nossalista.auth.repository.OAuthAuthorizationCodeRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Persisted one-time handoff codes for the web OAuth login.
 *
 * <p>During the V19 rolling deployment, new rows dual-write a SHA-256 digest,
 * user id and a legacy code-to-JWT representation. Legacy rows remain readable
 * while older application instances are being drained.</p>
 */
@Component
public class OAuthCodeStore {

    static final Duration DEFAULT_TTL = Duration.ofSeconds(60);
    private static final int CODE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final OAuthAuthorizationCodeRepository repository;
    private final JwtService jwtService;
    private final UserService userService;
    private final Duration ttl;

    @Autowired
    public OAuthCodeStore(OAuthAuthorizationCodeRepository repository, JwtService jwtService,
                          UserService userService) {
        this(repository, jwtService, userService, DEFAULT_TTL);
    }

    OAuthCodeStore(OAuthAuthorizationCodeRepository repository, JwtService jwtService,
                   UserService userService, Duration ttl) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.userService = userService;
        this.ttl = ttl;
    }

    protected OAuthCodeStore(OAuthAuthorizationCodeRepository repository) {
        this(repository, null, null, DEFAULT_TTL);
    }

    /**
     * Creates a dual-written handoff for a user during the V19 rolling deployment.
     *
     * @param userId authenticated user to recover during exchange
     * @return 256-bit URL-safe code to send to the browser
     */
    @Transactional
    public String issue(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId deve ser informado para emitir o code OAuth");
        }
        User user = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("userId não encontrado para emitir o code OAuth"));
        return issue(user);
    }

    /**
     * Creates a rolling-compatible handoff with a freshly signed session JWT.
     *
     * <p>The legacy fields remain populated only until every pre-V19 pod has been
     * drained. The code hash and user id preserve the atomic new-code exchange.</p>
     *
     * @param user authenticated user whose current session version is signed
     * @return 256-bit URL-safe code to send to the browser
     */
    @Transactional
    public String issue(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("usuário com id deve ser informado para emitir o code OAuth");
        }
        return persistIssuedCode(user.getId(), jwtService.generateToken(user));
    }

    /**
     * Creates a legacy code-to-JWT row for compatibility tests and old callers.
     * New callers should use {@link #issue(User)}.
     *
     * @param jwt session JWT held by an older application instance
     * @return 256-bit URL-safe code
     */
    @Transactional
    public String issue(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("jwt legado deve ser informado para emitir o code OAuth");
        }
        return persistIssuedCode(null, jwt);
    }

    private String persistIssuedCode(UUID userId, String jwt) {
        String code = generateCode();
        OAuthAuthorizationCode entity = new OAuthAuthorizationCode();
        entity.setCode(code);
        entity.setJwt(jwt);
        if (userId != null) {
            entity.setCodeHash(hash(code));
            entity.setUserId(userId);
        }
        entity.setExpiresAt(LocalDateTime.now().plus(ttl));
        repository.save(entity);
        repository.flush();
        return code;
    }

    /**
     * Atomically claims a new or legacy row.
     *
     * @param code opaque code supplied by the browser
     * @return claimed user id for new rows, or legacy JWT for old rows
     */
    @Transactional
    public Optional<OAuthCodeClaim> consumeForExchange(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        LocalDateTime now = LocalDateTime.now();
        String codeHash = hash(code);
        if (repository.claimByCodeHash(codeHash, now) > 0) {
            repository.flush();
            return repository.findByCodeHash(codeHash).map(this::toClaim);
        }

        if (repository.claimLegacyCode(code, now) > 0) {
            repository.flush();
            return repository.findByCode(code).map(this::toClaim);
        }

        return consumeLegacyForPreV19Repository(code, now);
    }

    /**
     * Compatibility view used by the pre-hardened handler tests and old callers.
     * Rolling dual-written rows retain a legacy JWT until old pods are retired.
     *
     * @param code opaque code supplied by the browser
     * @return legacy JWT when a legacy row is claimed
     */
    @Transactional
    public Optional<String> consume(String code) {
        return consumeForExchange(code).map(OAuthCodeClaim::legacyJwt).filter(jwt -> jwt != null);
    }

    /** Removes rows whose short handoff window has elapsed. */
    @Transactional
    public void evictExpired() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /** Number of rows, retained for focused store tests. */
    long size() {
        return repository.count();
    }

    private Optional<OAuthCodeClaim> consumeLegacyForPreV19Repository(String code, LocalDateTime now) {
        Optional<OAuthAuthorizationCode> legacy = repository.findByCode(code);
        if (legacy.isEmpty() || legacy.get().getConsumedAt() != null
            || legacy.get().getExpiresAt().isBefore(now)) {
            return Optional.empty();
        }
        repository.delete(legacy.get());
        repository.flush();
        return Optional.of(toClaim(legacy.get()));
    }

    private OAuthCodeClaim toClaim(OAuthAuthorizationCode entity) {
        return new OAuthCodeClaim(entity.getUserId(), entity.getJwt());
    }

    private String generateCode() {
        byte[] bytes = new byte[CODE_BYTES];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }

    private String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(code.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível para proteger o code OAuth", exception);
        }
    }

    /** Result of an atomic claim, with exactly one populated credential reference. */
    public record OAuthCodeClaim(UUID userId, String legacyJwt) {
    }
}
