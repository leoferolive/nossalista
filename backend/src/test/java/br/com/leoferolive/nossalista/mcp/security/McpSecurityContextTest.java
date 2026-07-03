package br.com.leoferolive.nossalista.mcp.security;

import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link McpSecurityContext}, incluindo o fix do review
 * sênior da Fase B: {@link McpSecurityContext#requireWriteAccess()} precisa
 * rejeitar autenticação ausente/anônima por conta própria, sem depender de
 * uma chamada anterior a {@link McpSecurityContext#currentUser()} na mesma tool.
 */
@DisplayName("McpSecurityContext")
class McpSecurityContextTest {

    private final McpSecurityContext security = new McpSecurityContext();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static UsernamePasswordAuthenticationToken authWith(Object principal, GrantedAuthority... authorities) {
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(authorities));
    }

    private static User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("mcpuser");
        return user;
    }

    @Test
    @DisplayName("currentUser lança McpAuthenticationException quando não há autenticação")
    void currentUserThrowsWhenNoAuthentication() {
        assertThatThrownBy(security::currentUser).isInstanceOf(McpAuthenticationException.class);
    }

    @Test
    @DisplayName("currentUser lança McpAuthenticationException para autenticação anônima")
    void currentUserThrowsForAnonymous() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThatThrownBy(security::currentUser).isInstanceOf(McpAuthenticationException.class);
    }

    @Test
    @DisplayName("currentUser retorna o principal quando autenticado com um User")
    void currentUserReturnsUserWhenAuthenticated() {
        User user = user();
        SecurityContextHolder.getContext().setAuthentication(authWith(user, new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(security.currentUser()).isSameAs(user);
    }

    @Test
    @DisplayName("requireWriteAccess lança McpAuthenticationException quando não há autenticação, mesmo sem currentUser() antes")
    void requireWriteAccessThrowsAuthenticationExceptionWhenNotAuthenticated() {
        assertThatThrownBy(security::requireWriteAccess).isInstanceOf(McpAuthenticationException.class);
    }

    @Test
    @DisplayName("requireWriteAccess lança McpAuthenticationException para autenticação anônima")
    void requireWriteAccessThrowsForAnonymous() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThatThrownBy(security::requireWriteAccess).isInstanceOf(McpAuthenticationException.class);
    }

    @Test
    @DisplayName("requireWriteAccess lança McpScopeException para PAT de escopo READ")
    void requireWriteAccessThrowsScopeExceptionForReadOnlyPat() {
        SecurityContextHolder.getContext().setAuthentication(authWith(
            user(), new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("PAT_AUTH"), new SimpleGrantedAuthority("SCOPE_READ")));

        assertThatThrownBy(security::requireWriteAccess).isInstanceOf(McpScopeException.class);
    }

    @Test
    @DisplayName("requireWriteAccess não lança para PAT de escopo READ_WRITE")
    void requireWriteAccessAllowsReadWritePat() {
        SecurityContextHolder.getContext().setAuthentication(authWith(
            user(), new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("PAT_AUTH"), new SimpleGrantedAuthority("SCOPE_READ_WRITE")));

        security.requireWriteAccess();
    }

    @Test
    @DisplayName("requireWriteAccess não lança para sessão JWT normal (sem authority PAT_AUTH)")
    void requireWriteAccessAllowsJwtSession() {
        SecurityContextHolder.getContext().setAuthentication(authWith(user(), new SimpleGrantedAuthority("ROLE_USER")));

        security.requireWriteAccess();
    }
}
