package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.UserAuthIdentity;
import br.com.leoferolive.nossalista.auth.repository.UserAuthIdentityRepository;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Resolves Google callbacks by stable provider identity and performs the
 * bounded migration of legacy Google accounts.
 */
@Service
public class GoogleIdentityService {

    private static final String PROVIDER = "GOOGLE";
    private static final String DEFAULT_ISSUER = "https://accounts.google.com";
    private static final Instant LEGACY_BINDING_DEADLINE =
        Instant.parse("2026-12-18T03:00:00Z");

    private final UserAuthIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final Clock clock;
    private final String canonicalIssuer;

    /** Creates the application resolver using the configured issuer. */
    @Autowired
    public GoogleIdentityService(
        UserAuthIdentityRepository identityRepository,
        UserRepository userRepository,
        AuthService authService,
        @Value("${app.auth.google.issuer:https://accounts.google.com}") String canonicalIssuer
    ) {
        this(identityRepository, userRepository, authService, canonicalIssuer, Clock.systemUTC());
    }

    GoogleIdentityService(
        UserAuthIdentityRepository identityRepository,
        UserRepository userRepository,
        AuthService authService,
        Clock clock
    ) {
        this(identityRepository, userRepository, authService, DEFAULT_ISSUER, clock);
    }

    private GoogleIdentityService(
        UserAuthIdentityRepository identityRepository,
        UserRepository userRepository,
        AuthService authService,
        String canonicalIssuer,
        Clock clock
    ) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.canonicalIssuer = canonicalIssuer;
        this.clock = clock;
    }

    /**
     * Resolves a trusted Google callback to a local user, creating or migrating
     * the account only when the provider assertions permit it.
     *
     * @param claims Google OpenID Connect claims
     * @return the local user to authenticate
     * @throws GoogleIdentityRejectedException for every rejected callback
     */
    @Transactional
    public User resolve(GoogleIdentityClaims claims) {
        validateClaims(claims);
        String email = normalizeEmail(claims.email());
        Optional<UserAuthIdentity> identity = identityRepository
            .findByProviderAndIssuerAndSubject(PROVIDER, canonicalIssuer, claims.subject().trim());

        if (identity.isPresent()) {
            return refreshKnownIdentity(identity.get(), claims, email);
        }

        return migrateLegacyOrCreate(claims, email);
    }

    private void validateClaims(GoogleIdentityClaims claims) {
        if (claims == null
            || !canonicalIssuer.equals(claims.issuer())
            || claims.subject() == null
            || claims.subject().isBlank()
            || !claims.emailVerified()
            || claims.email() == null
            || claims.email().isBlank()) {
            throw new GoogleIdentityRejectedException();
        }
    }

    private User refreshKnownIdentity(
        UserAuthIdentity identity, GoogleIdentityClaims claims, String email
    ) {
        User user = userRepository.findById(identity.getUserId())
            .filter(candidate -> candidate.getAuthProvider() == AuthProvider.GOOGLE)
            .orElseThrow(GoogleIdentityRejectedException::new);
        LocalDateTime now = now();
        boolean userChanged = refreshUserDisplayData(user, claims);
        identity.setProviderEmail(email);
        identity.setProviderEmailVerified(claims.emailVerified());
        identity.setUpdatedAt(now);
        identityRepository.save(identity);
        if (userChanged) {
            userRepository.save(user);
        }
        return user;
    }

    private User migrateLegacyOrCreate(GoogleIdentityClaims claims, String email) {
        Optional<User> legacyUser = userRepository.findByEmail(email);
        if (legacyUser.isEmpty()) {
            return createGoogleUser(claims, email);
        }
        if (!isBeforeLegacyDeadline()) {
            throw new GoogleIdentityRejectedException();
        }
        User user = legacyUser.get();
        if (user.getAuthProvider() != AuthProvider.GOOGLE
            || identityRepository.findByProviderAndIssuerAndUserId(
                PROVIDER, canonicalIssuer, user.getId()).isPresent()) {
            throw new GoogleIdentityRejectedException();
        }
        User updatedUser = updateLegacyGoogleUser(user, claims);
        bindIdentity(updatedUser, claims, email);
        return updatedUser;
    }

    private User createGoogleUser(GoogleIdentityClaims claims, String email) {
        User user = new User();
        user.setUsername(authService.generateUniqueUsername(email));
        user.setEmail(email);
        user.setName(claims.name());
        user.setAvatarUrl(claims.picture());
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setRole(Role.USER);
        user.setEmailVerified(claims.emailVerified());
        User saved = userRepository.save(user);
        bindIdentity(saved, claims, email);
        return saved;
    }

    private User updateLegacyGoogleUser(User user, GoogleIdentityClaims claims) {
        boolean changed = refreshUserDisplayData(user, claims);
        if (!user.isEmailVerified()) {
            user.setEmailVerified(claims.emailVerified());
            changed = true;
        }
        return changed ? userRepository.save(user) : user;
    }

    private void bindIdentity(User user, GoogleIdentityClaims claims, String email) {
        UserAuthIdentity identity = new UserAuthIdentity();
        identity.setProvider(PROVIDER);
        identity.setIssuer(canonicalIssuer);
        identity.setSubject(claims.subject().trim());
        identity.setUserId(user.getId());
        identity.setProviderEmail(email);
        identity.setProviderEmailVerified(claims.emailVerified());
        identity.setCreatedAt(now());
        identity.setUpdatedAt(now());
        identityRepository.save(identity);
    }

    private boolean refreshUserDisplayData(User user, GoogleIdentityClaims claims) {
        boolean changed = false;
        if (claims.name() != null && !claims.name().equals(user.getName())) {
            user.setName(claims.name());
            changed = true;
        }
        if (claims.picture() != null && !claims.picture().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(claims.picture());
            changed = true;
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerified(claims.emailVerified());
            changed = true;
        }
        return changed;
    }

    private boolean isBeforeLegacyDeadline() {
        return clock.instant().isBefore(LEGACY_BINDING_DEADLINE);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
