package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PatAuthorizationSupport")
class PatAuthorizationSupportTest {

    private static UsernamePasswordAuthenticationToken authenticatedWith(GrantedAuthority... authorities) {
        return new UsernamePasswordAuthenticationToken("user", null, List.of(authorities));
    }

    @Test
    @DisplayName("isAuthenticatedUser é falso para null e para autenticação anônima")
    void isAuthenticatedUserFalseForNullAndAnonymous() {
        assertThat(PatAuthorizationSupport.isAuthenticatedUser(null)).isFalse();

        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        assertThat(PatAuthorizationSupport.isAuthenticatedUser(anonymous)).isFalse();
    }

    @Test
    @DisplayName("isAuthenticatedUser é verdadeiro para autenticação normal")
    void isAuthenticatedUserTrueForRealAuthentication() {
        var auth = authenticatedWith(new SimpleGrantedAuthority("ROLE_USER"));
        assertThat(PatAuthorizationSupport.isAuthenticatedUser(auth)).isTrue();
    }

    @Test
    @DisplayName("isPersonalAccessToken detecta a authority PAT_AUTH")
    void isPersonalAccessTokenDetectsMarkerAuthority() {
        var jwtAuth = authenticatedWith(new SimpleGrantedAuthority("ROLE_USER"));
        var patAuth = authenticatedWith(
            new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("PAT_AUTH"));

        assertThat(PatAuthorizationSupport.isPersonalAccessToken(null)).isFalse();
        assertThat(PatAuthorizationSupport.isPersonalAccessToken(jwtAuth)).isFalse();
        assertThat(PatAuthorizationSupport.isPersonalAccessToken(patAuth)).isTrue();
    }

    @Test
    @DisplayName("hasReadOnlyScope detecta a authority SCOPE_READ")
    void hasReadOnlyScopeDetectsScopeReadAuthority() {
        var readOnly = authenticatedWith(
            new SimpleGrantedAuthority("PAT_AUTH"), new SimpleGrantedAuthority("SCOPE_READ"));
        var readWrite = authenticatedWith(
            new SimpleGrantedAuthority("PAT_AUTH"), new SimpleGrantedAuthority("SCOPE_READ_WRITE"));

        assertThat(PatAuthorizationSupport.hasReadOnlyScope(readOnly)).isTrue();
        assertThat(PatAuthorizationSupport.hasReadOnlyScope(readWrite)).isFalse();
        assertThat(PatAuthorizationSupport.hasReadOnlyScope(null)).isFalse();
    }

    @Test
    @DisplayName("isSafeMethod reconhece GET/HEAD/OPTIONS e rejeita métodos de mutação")
    void isSafeMethodRecognizesSafeVerbs() {
        assertThat(PatAuthorizationSupport.isSafeMethod("GET")).isTrue();
        assertThat(PatAuthorizationSupport.isSafeMethod("head")).isTrue();
        assertThat(PatAuthorizationSupport.isSafeMethod("Options")).isTrue();
        assertThat(PatAuthorizationSupport.isSafeMethod("POST")).isFalse();
        assertThat(PatAuthorizationSupport.isSafeMethod("DELETE")).isFalse();
        assertThat(PatAuthorizationSupport.isSafeMethod(null)).isFalse();
    }
}
