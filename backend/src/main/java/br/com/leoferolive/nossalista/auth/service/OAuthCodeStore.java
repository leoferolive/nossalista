package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.OAuthAuthorizationCode;
import br.com.leoferolive.nossalista.auth.repository.OAuthAuthorizationCodeRepository;
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
 * <p>New rows contain only a SHA-256 digest, the user id, expiry and consumption
 * timestamp. Legacy rows remain readable while older application instances are
 * being drained.</p>
 */
@Component
public class OAuthCodeStore {

    static final Duration DEFAULT_TTL = Duration.ofSeconds(60);
    private static final int CODE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final OAuthAuthorizationCodeRepository repository;
    private final Duration ttl;

    @Autowired
    public OAuthCodeStore(OAuthAuthorizationCodeRepository repository) {
        this(repository, DEFAULT_TTL);
    }

    OAuthCodeStore(OAuthAuthorizationCodeRepository repository, Duration ttl) {
        this.repository = repository;
        this.ttl = ttl;
    }

    /**
     * Creates a hash-only handoff for a user.
     *
     * @param userId authenticated user to recover during exchange
     * @return 256-bit URL-safe code to send to the browser
     */
    @Transactional
    public String issue(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId deve ser informado para emitir o code OAuth");
        }
        String code = generateCode();
        OAuthAuthorizationCode entity = new OAuthAuthorizationCode();
        entity.setCodeHash(hash(code));
        entity.setUserId(userId);
        entity.setExpiresAt(LocalDateTime.now().plus(ttl));
        repository.save(entity);
        repository.flush();
        return code;
    }

    /**
     * Creates a legacy code-to-JWT row for rolling deployment compatibility.
     * New callers should use {@link #issue(UUID)}.
     *
     * @param jwt session JWT held by an older application instance
     * @return 256-bit URL-safe code
     */
    @Transactional
    public String issue(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("jwt legado deve ser informado para emitir o code OAuth");
        }
        String code = generateCode();
        OAuthAuthorizationCode entity = new OAuthAuthorizationCode();
        entity.setCode(code);
        entity.setJwt(jwt);
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
     * New hash-only rows intentionally have no JWT to return from this method.
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
