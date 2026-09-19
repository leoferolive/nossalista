package br.com.leoferolive.nossalista.auth;

import br.com.leoferolive.nossalista.auth.service.GoogleIdentityRejectedException;
import br.com.leoferolive.nossalista.auth.service.GoogleIdentityClaims;
import br.com.leoferolive.nossalista.auth.service.GoogleIdentityService;
import br.com.leoferolive.nossalista.auth.service.OAuthCodeStore;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2SuccessHandler")
class OAuth2SuccessHandlerTest {

    @Mock
    private GoogleIdentityService googleIdentityService;

    @Mock
    private OAuthCodeStore oauthCodeStore;

    @Mock
    private HttpServletResponse response;

    private OAuth2SuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2SuccessHandler(googleIdentityService, oauthCodeStore);
        successHandler.setFrontendUrl("http://localhost:5173");
    }

    @Test
    @DisplayName("resolve identidade Google e redireciona somente com code opaco")
    void redirectsWithOpaqueCodeAfterIdentityResolution() throws IOException {
        User user = googleUser();
        when(googleIdentityService.resolve(any())).thenReturn(user);
        when(oauthCodeStore.issue(user.getId())).thenReturn("opaque-code");

        successHandler.onAuthenticationSuccess(null, response, googleAuthentication());

        ArgumentCaptor<String> redirect = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirect.capture());
        assertThat(redirect.getValue()).isEqualTo("http://localhost:5173/auth/callback?code=opaque-code");
        verify(oauthCodeStore).issue(user.getId());
    }

    @Test
    @DisplayName("repassa sub e email_verified para o resolvedor Google")
    void passesStableClaimsToIdentityResolver() throws IOException {
        User user = googleUser();
        when(googleIdentityService.resolve(any())).thenReturn(user);
        when(oauthCodeStore.issue(user.getId())).thenReturn("opaque-code");

        successHandler.onAuthenticationSuccess(null, response, googleAuthentication());

        ArgumentCaptor<GoogleIdentityClaims> claims = ArgumentCaptor.forClass(GoogleIdentityClaims.class);
        verify(googleIdentityService).resolve(claims.capture());
        assertThat(claims.getValue().subject()).isEqualTo("google-sub-1");
        assertThat(claims.getValue().emailVerified()).isTrue();
        assertThat(claims.getValue().issuer()).isEqualTo("https://accounts.google.com");
    }

    @Test
    @DisplayName("não expõe detalhes quando vínculo Google é rejeitado")
    void redirectsWithGenericErrorWhenIdentityIsRejected() throws IOException {
        doThrow(new GoogleIdentityRejectedException()).when(googleIdentityService).resolve(any());

        successHandler.onAuthenticationSuccess(null, response, googleAuthentication());

        verify(response).sendRedirect("http://localhost:5173/auth/callback?error=google_identity_rejected");
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
}
