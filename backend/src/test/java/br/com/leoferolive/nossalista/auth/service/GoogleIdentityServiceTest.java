package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.UserAuthIdentity;
import br.com.leoferolive.nossalista.auth.repository.UserAuthIdentityRepository;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("Google identity binding")
class GoogleIdentityServiceTest {

    private static final String ISSUER = "https://accounts.google.com";
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");

    @Autowired
    private UserAuthIdentityRepository identityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private GoogleIdentityService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(NOW);
    }

    @Test
    void rejectsMissingSubjectWithoutPersistingAnIdentity() {
        assertRejected(claims(" ", "person@example.com", true));

        assertThat(identityRepository.count()).isZero();
    }

    @Test
    void rejectsUnverifiedProviderEmailWithoutPersistingAnIdentity() {
        assertRejected(claims("subject-1", "person@example.com", false));

        assertThat(identityRepository.count()).isZero();
    }

    @Test
    void rejectsNonCanonicalIssuer() {
        GoogleIdentityClaims claims = new GoogleIdentityClaims(
            "https://accounts.google.com/", "subject-1", "person@example.com", true,
            "Person", "https://example.com/avatar"
        );

        assertRejected(claims);
    }

    @Test
    void resolvesKnownIdentityAndRefreshesDisplayDataAndLastUse() {
        User user = persist(user(AuthProvider.GOOGLE, "person@example.com"));
        UserAuthIdentity identity = identity(user.getId(), "subject-1");
        identity.setProviderEmail("old@example.com");
        LocalDateTime staleLastUse = NOW.minus(1, ChronoUnit.DAYS)
            .atZone(ZoneOffset.UTC).toLocalDateTime();
        identity.setUpdatedAt(staleLastUse);
        persist(identity);

        User resolved = service.resolve(claims("subject-1", "PERSON@EXAMPLE.COM", true));
        flushAndClear();

        User refreshedUser = userRepository.findById(user.getId()).orElseThrow();
        UserAuthIdentity refreshedIdentity = findIdentity("subject-1").orElseThrow();

        assertThat(resolved.getId()).isEqualTo(user.getId());
        assertThat(refreshedUser.getName()).isEqualTo("Person");
        assertThat(refreshedUser.getAvatarUrl()).isEqualTo("https://example.com/avatar");
        assertThat(refreshedIdentity.getProviderEmail()).isEqualTo("person@example.com");
        assertThat(refreshedIdentity.isProviderEmailVerified()).isTrue();
        assertThat(refreshedIdentity.getUpdatedAt()).isAfter(staleLastUse);
    }

    @Test
    void bindsUnboundLegacyGoogleUserByVerifiedEmailBeforeDeadline() {
        User user = persist(user(AuthProvider.GOOGLE, "person@example.com"));

        User resolved = service.resolve(claims("subject-1", "person@example.com", true));
        flushAndClear();

        UserAuthIdentity identity = findIdentity("subject-1").orElseThrow();
        User migratedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(resolved.getId()).isEqualTo(user.getId());
        assertThat(identity.getUserId()).isEqualTo(user.getId());
        assertThat(identity.getIssuer()).isEqualTo(ISSUER);
        assertThat(identity.getSubject()).isEqualTo("subject-1");
        assertThat(migratedUser.isEmailVerified()).isTrue();
    }

    @Test
    void doesNotMigrateLegacyGoogleUserAfterDeadline() {
        persist(user(AuthProvider.GOOGLE, "person@example.com"));
        GoogleIdentityService expiredService = serviceAt(Instant.parse("2026-12-18T03:00:00Z"));

        assertThatThrownBy(() -> expiredService.resolve(claims("subject-1", "person@example.com", true)))
            .isInstanceOf(GoogleIdentityRejectedException.class);

        assertThat(identityRepository.count()).isZero();
    }

    @Test
    void createsNewGoogleUserAfterLegacyMigrationDeadline() {
        GoogleIdentityService expiredService = serviceAt(Instant.parse("2026-12-18T03:00:00Z"));

        User created = expiredService.resolve(claims("subject-1", "person@example.com", true));
        flushAndClear();

        UserAuthIdentity identity = findIdentity("subject-1").orElseThrow();

        assertThat(created.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(identity.getUserId()).isEqualTo(created.getId());
    }

    @Test
    void neverAutoLinksEmailAccountByMatchingEmail() {
        persist(user(AuthProvider.EMAIL, "person@example.com"));

        assertRejected(claims("subject-1", "person@example.com", true));

        assertThat(identityRepository.count()).isZero();
    }

    @Test
    void createsNewGoogleUserAndIdentityWhenEmailIsUnbound() {
        User created = service.resolve(claims("subject-1", "person@example.com", true));
        flushAndClear();

        UserAuthIdentity identity = findIdentity("subject-1").orElseThrow();

        assertThat(created.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(created.getEmail()).isEqualTo("person@example.com");
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(identity.getUserId()).isEqualTo(created.getId());
        assertThat(identity.isProviderEmailVerified()).isTrue();
    }

    private GoogleIdentityService serviceAt(Instant instant) {
        return new GoogleIdentityService(
            identityRepository,
            userRepository,
            new FakeGoogleUsernameGenerator("person"),
            Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    private void assertRejected(GoogleIdentityClaims claims) {
        assertThatThrownBy(() -> service.resolve(claims))
            .isInstanceOf(GoogleIdentityRejectedException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Optional<UserAuthIdentity> findIdentity(String subject) {
        return identityRepository.findByProviderAndIssuerAndSubject("GOOGLE", ISSUER, subject);
    }

    private GoogleIdentityClaims claims(String subject, String email, boolean verified) {
        return new GoogleIdentityClaims(ISSUER, subject, email, verified, "Person", "https://example.com/avatar");
    }

    private User persist(User user) {
        return userRepository.save(user);
    }

    private UserAuthIdentity persist(UserAuthIdentity identity) {
        return identityRepository.save(identity);
    }

    private User user(AuthProvider provider, String email) {
        User user = new User();
        user.setUsername("person-" + UUID.randomUUID());
        user.setEmail(email);
        if (provider == AuthProvider.EMAIL) {
            user.setPassword("hashed-password");
        }
        user.setAuthProvider(provider);
        user.setRole(Role.USER);
        return user;
    }

    private UserAuthIdentity identity(UUID userId, String subject) {
        UserAuthIdentity identity = new UserAuthIdentity();
        identity.setProvider("GOOGLE");
        identity.setIssuer(ISSUER);
        identity.setSubject(subject);
        identity.setUserId(userId);
        return identity;
    }

    private static final class FakeGoogleUsernameGenerator extends AuthService {

        private final String username;

        private FakeGoogleUsernameGenerator(String username) {
            super(null, null, null, null);
            this.username = username;
        }

        @Override
        public String generateUniqueUsername(String email) {
            return username;
        }
    }
}
