package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.UserAuthIdentity;
import br.com.leoferolive.nossalista.auth.repository.UserAuthIdentityRepository;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Google identity binding")
class GoogleIdentityServiceTest {

    private static final String ISSUER = "https://accounts.google.com";
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");

    @Mock
    private UserAuthIdentityRepository identityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    private GoogleIdentityService service;

    @BeforeEach
    void setUp() {
        service = new GoogleIdentityService(
            identityRepository,
            userRepository,
            authService,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void rejectsMissingSubjectBeforeAnyLookup() {
        GoogleIdentityClaims claims = claims(" ", "person@example.com", true);

        assertThatThrownBy(() -> service.resolve(claims))
            .isInstanceOf(GoogleIdentityRejectedException.class);
        verify(identityRepository, never()).findByProviderAndIssuerAndSubject(any(), any(), any());
    }

    @Test
    void rejectsUnverifiedProviderEmailBeforeAnyLookup() {
        GoogleIdentityClaims claims = claims("subject-1", "person@example.com", false);

        assertThatThrownBy(() -> service.resolve(claims))
            .isInstanceOf(GoogleIdentityRejectedException.class);
        verify(identityRepository, never()).findByProviderAndIssuerAndSubject(any(), any(), any());
    }

    @Test
    void rejectsNonCanonicalIssuer() {
        GoogleIdentityClaims claims = new GoogleIdentityClaims(
            "https://accounts.google.com/", "subject-1", "person@example.com", true,
            "Person", "https://example.com/avatar"
        );

        assertThatThrownBy(() -> service.resolve(claims))
            .isInstanceOf(GoogleIdentityRejectedException.class);
        verify(identityRepository, never()).findByProviderAndIssuerAndSubject(any(), any(), any());
    }

    @Test
    void resolvesKnownIdentityAndRefreshesDisplayDataAndLastUse() {
        User user = user(AuthProvider.GOOGLE, "person@example.com");
        UserAuthIdentity identity = identity(user.getId(), "subject-1");
        identity.setProviderEmail("old@example.com");
        identity.setUpdatedAt(NOW.minus(1, ChronoUnit.DAYS).atZone(ZoneOffset.UTC).toLocalDateTime());
        when(identityRepository.findByProviderAndIssuerAndSubject("GOOGLE", ISSUER, "subject-1"))
            .thenReturn(Optional.of(identity));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(identityRepository.save(any(UserAuthIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User resolved = service.resolve(claims("subject-1", "PERSON@EXAMPLE.COM", true));

        assertThat(resolved).isSameAs(user);
        assertThat(user.getName()).isEqualTo("Person");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar");
        assertThat(identity.getProviderEmail()).isEqualTo("person@example.com");
        assertThat(identity.isProviderEmailVerified()).isTrue();
        assertThat(identity.getUpdatedAt()).isAfter(NOW.minus(1, ChronoUnit.DAYS).atZone(ZoneOffset.UTC).toLocalDateTime());
        verify(identityRepository).save(identity);
        verify(userRepository).save(user);
    }

    @Test
    void bindsUnboundLegacyGoogleUserByVerifiedEmailBeforeDeadline() {
        User user = user(AuthProvider.GOOGLE, "person@example.com");
        when(identityRepository.findByProviderAndIssuerAndSubject("GOOGLE", ISSUER, "subject-1"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));
        when(identityRepository.findByProviderAndIssuerAndUserId("GOOGLE", ISSUER, user.getId()))
            .thenReturn(Optional.empty());
        when(identityRepository.save(any(UserAuthIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User resolved = service.resolve(claims("subject-1", "person@example.com", true));

        assertThat(resolved).isSameAs(user);
        ArgumentCaptor<UserAuthIdentity> identityCaptor = ArgumentCaptor.forClass(UserAuthIdentity.class);
        verify(identityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(identityCaptor.getValue().getIssuer()).isEqualTo(ISSUER);
        assertThat(identityCaptor.getValue().getSubject()).isEqualTo("subject-1");
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void doesNotMigrateLegacyGoogleUserAfterDeadline() {
        GoogleIdentityService expiredService = new GoogleIdentityService(
            identityRepository,
            userRepository,
            authService,
            Clock.fixed(Instant.parse("2026-12-18T03:00:00Z"), ZoneOffset.UTC)
        );
        User user = user(AuthProvider.GOOGLE, "person@example.com");
        when(identityRepository.findByProviderAndIssuerAndSubject("GOOGLE", ISSUER, "subject-1"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> expiredService.resolve(claims("subject-1", "person@example.com", true)))
            .isInstanceOf(GoogleIdentityRejectedException.class);
        verify(identityRepository, never()).save(any(UserAuthIdentity.class));
    }

    @Test
    void createsNewGoogleUserAfterLegacyMigrationDeadline() {
        GoogleIdentityService expiredService = new GoogleIdentityService(
            identityRepository,
            userRepository,
            authService,
            Clock.fixed(Instant.parse("2026-12-18T03:00:00Z"), ZoneOffset.UTC)
        );
        when(identityRepository.findByProviderAndIssuerAndSubject("GOOGLE", ISSUER, "subject-1"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.empty());
        when(authService.generateUniqueUsername("person@example.com")).thenReturn("person");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(identityRepository.save(any(UserAuthIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = expiredService.resolve(claims("subject-1", "person@example.com", true));

        assertThat(created.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        verify(identityRepository).save(any(UserAuthIdentity.class));
    }

    @Test
    void neverAutoLinksEmailAccountByMatchingEmail() {
        User user = user(AuthProvider.EMAIL, "person@example.com");
        when(identityRepository.findByProviderAndIssuerAndSubject("GOOGLE", ISSUER, "subject-1"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resolve(claims("subject-1", "person@example.com", true)))
            .isInstanceOf(GoogleIdentityRejectedException.class);
        verify(identityRepository, never()).save(any(UserAuthIdentity.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createsNewGoogleUserAndIdentityWhenEmailIsUnbound() {
        when(identityRepository.findByProviderAndIssuerAndSubject("GOOGLE", ISSUER, "subject-1"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.empty());
        when(authService.generateUniqueUsername("person@example.com")).thenReturn("person");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
        when(identityRepository.save(any(UserAuthIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.resolve(claims("subject-1", "person@example.com", true));

        assertThat(created.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(created.getEmail()).isEqualTo("person@example.com");
        assertThat(created.isEmailVerified()).isTrue();
        ArgumentCaptor<UserAuthIdentity> identityCaptor = ArgumentCaptor.forClass(UserAuthIdentity.class);
        verify(identityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getUserId()).isEqualTo(created.getId());
        assertThat(identityCaptor.getValue().isProviderEmailVerified()).isTrue();
    }

    private GoogleIdentityClaims claims(String subject, String email, boolean verified) {
        return new GoogleIdentityClaims(ISSUER, subject, email, verified, "Person", "https://example.com/avatar");
    }

    private User user(AuthProvider provider, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("person");
        user.setEmail(email);
        user.setAuthProvider(provider);
        user.setRole(Role.USER);
        return user;
    }

    private UserAuthIdentity identity(UUID userId, String subject) {
        UserAuthIdentity identity = new UserAuthIdentity();
        identity.setId(UUID.randomUUID());
        identity.setProvider("GOOGLE");
        identity.setIssuer(ISSUER);
        identity.setSubject(subject);
        identity.setUserId(userId);
        return identity;
    }
}
