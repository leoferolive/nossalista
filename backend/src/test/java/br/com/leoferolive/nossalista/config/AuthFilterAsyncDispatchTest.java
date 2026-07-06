package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que os filtros de autenticacao re-executam no dispatch ASYNC (fix de
 * D-026): {@code shouldNotFilterAsyncDispatch()} deve retornar {@code false}, sem
 * o que o segundo passo do {@code AsyncContext.dispatch()} do transporte MCP
 * Streamable HTTP fica sem {@code SecurityContext} e nega o token valido.
 *
 * <p>O comportamento end-to-end e verificado em {@code AsyncDispatchReauthenticationTest};
 * estes asserts cobrem o override diretamente (metodo {@code protected}, acessivel
 * no mesmo pacote). Os deps do construtor nao sao usados pelo override, por isso
 * {@code null} basta.</p>
 */
class AuthFilterAsyncDispatchTest {

    @Test
    @DisplayName("JwtAuthenticationFilter re-autentica no dispatch async")
    void jwtFilterReauthenticatesOnAsyncDispatch() {
        assertThat(new JwtAuthenticationFilter(null, null).shouldNotFilterAsyncDispatch()).isFalse();
    }

    @Test
    @DisplayName("PersonalAccessTokenAuthenticationFilter re-autentica no dispatch async")
    void patFilterReauthenticatesOnAsyncDispatch() {
        assertThat(
            new PersonalAccessTokenAuthenticationFilter(null, null, null, null).shouldNotFilterAsyncDispatch()
        ).isFalse();
    }
}
