package br.com.leoferolive.nossalista.auth.provider;

import br.com.leoferolive.nossalista.auth.service.GoogleIdentityClaims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GoogleOAuth2ClaimsAdapter")
class GoogleOAuth2ClaimsAdapterTest {

    @Test
    @DisplayName("converte sub, email e email_verified do Google em claims tipados")
    void mapsGoogleIdentityClaims() {
        var googleUser = new DefaultOAuth2User(
            List.of(),
            Map.of(
                "sub", "google-sub-1",
                "email", "person@gmail.com",
                "email_verified", "true",
                "name", "Person",
                "picture", "https://example.com/avatar"
            ),
            "sub"
        );

        GoogleIdentityClaims claims = new GoogleOAuth2ClaimsAdapter().from(googleUser);

        assertThat(claims.issuer()).isEqualTo("https://accounts.google.com");
        assertThat(claims.subject()).isEqualTo("google-sub-1");
        assertThat(claims.email()).isEqualTo("person@gmail.com");
        assertThat(claims.emailVerified()).isTrue();
    }
}
