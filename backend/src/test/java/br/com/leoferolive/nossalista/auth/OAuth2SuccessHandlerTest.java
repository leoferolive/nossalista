package br.com.leoferolive.nossalista.auth;

import br.com.leoferolive.nossalista.auth.provider.GoogleOAuth2ClaimsAdapter;
import br.com.leoferolive.nossalista.auth.service.GoogleIdentityRejectedException;
import br.com.leoferolive.nossalista.auth.service.GoogleIdentityClaims;
import br.com.leoferolive.nossalista.auth.service.GoogleIdentityService;
import br.com.leoferolive.nossalista.auth.service.OAuthCodeStore;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth2SuccessHandler")
class OAuth2SuccessHandlerTest {

    private FakeGoogleIdentityService googleIdentityService;
    private FakeOAuthCodeStore oauthCodeStore;
    private MockHttpServletResponse response;

    private OAuth2SuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        googleIdentityService = FakeGoogleIdentityService.resolving(googleUser());
        oauthCodeStore = new FakeOAuthCodeStore("opaque-code");
        response = new MockHttpServletResponse();
        successHandler = new OAuth2SuccessHandler(
            googleIdentityService,
            oauthCodeStore,
            new GoogleOAuth2ClaimsAdapter()
        );
        successHandler.setFrontendUrl("http://localhost:5173");
    }

    @Test
    @DisplayName("resolve identidade Google e redireciona somente com code opaco")
    void redirectsWithOpaqueCodeAfterIdentityResolution() throws IOException {
        User user = googleUser();
        googleIdentityService = FakeGoogleIdentityService.resolving(user);
        successHandler = new OAuth2SuccessHandler(
            googleIdentityService,
            oauthCodeStore,
            new GoogleOAuth2ClaimsAdapter()
        );
        successHandler.setFrontendUrl("http://localhost:5173");

        successHandler.onAuthenticationSuccess(null, response, googleAuthentication());

        assertThat(response.getRedirectedUrl())
            .isEqualTo("http://localhost:5173/auth/callback?code=opaque-code");
        assertThat(oauthCodeStore.issuedForUser()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("repassa sub e email_verified para o resolvedor Google")
    void passesStableClaimsToIdentityResolver() throws IOException {
        successHandler.onAuthenticationSuccess(null, response, googleAuthentication());

        GoogleIdentityClaims claims = googleIdentityService.receivedClaims();
        assertThat(claims.subject()).isEqualTo("google-sub-1");
        assertThat(claims.emailVerified()).isTrue();
        assertThat(claims.issuer()).isEqualTo("https://accounts.google.com");
    }

    @Test
    @DisplayName("não expõe detalhes quando vínculo Google é rejeitado")
    void redirectsWithGenericErrorWhenIdentityIsRejected() throws IOException {
        googleIdentityService = FakeGoogleIdentityService.rejecting();
        successHandler = new OAuth2SuccessHandler(
            googleIdentityService,
            oauthCodeStore,
            new GoogleOAuth2ClaimsAdapter()
        );
        successHandler.setFrontendUrl("http://localhost:5173");

        successHandler.onAuthenticationSuccess(null, response, googleAuthentication());

        assertThat(response.getRedirectedUrl())
            .isEqualTo("http://localhost:5173/auth/callback?error=google_identity_rejected");
    }

    private Authentication googleAuthentication() {
        Map<String, Object> attributes = Map.of(
            "sub", "google-sub-1",
            "email", "person@gmail.com",
            "email_verified", true,
            "name", "Person",
            "picture", "https://example.com/avatar"
        );
        var principal = new DefaultOAuth2User(
            List.of(), attributes, "sub");
        return new OAuth2AuthenticationToken(principal, List.of(), "google");
    }

    private User googleUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("person@gmail.com");
        user.setUsername("person");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        return user;
    }

    private static final class FakeGoogleIdentityService extends GoogleIdentityService {

        private final User resolvedUser;
        private final boolean rejectsIdentity;
        private GoogleIdentityClaims receivedClaims;

        private FakeGoogleIdentityService(User resolvedUser, boolean rejectsIdentity) {
            super(null, null, null, "https://accounts.google.com");
            this.resolvedUser = resolvedUser;
            this.rejectsIdentity = rejectsIdentity;
        }

        static FakeGoogleIdentityService resolving(User user) {
            return new FakeGoogleIdentityService(user, false);
        }

        static FakeGoogleIdentityService rejecting() {
            return new FakeGoogleIdentityService(null, true);
        }

        @Override
        public User resolve(GoogleIdentityClaims claims) {
            receivedClaims = claims;
            if (rejectsIdentity) {
                throw new GoogleIdentityRejectedException();
            }
            return resolvedUser;
        }

        GoogleIdentityClaims receivedClaims() {
            return receivedClaims;
        }
    }

    private static final class FakeOAuthCodeStore extends OAuthCodeStore {

        private final String issuedCode;
        private UUID issuedForUser;

        private FakeOAuthCodeStore(String issuedCode) {
            super(null);
            this.issuedCode = issuedCode;
        }

        @Override
        public String issue(UUID userId) {
            issuedForUser = userId;
            return issuedCode;
        }

        UUID issuedForUser() {
            return issuedForUser;
        }
    }
}
