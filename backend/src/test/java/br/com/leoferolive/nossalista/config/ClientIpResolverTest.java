package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientIpResolver")
class ClientIpResolverTest {

    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() {
        clientIpResolver = new ClientIpResolver();
    }

    @Test
    @DisplayName("deve resolver o IP a partir do header confiável CF-Connecting-IP")
    void resolvesIpFromTrustedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5"); // IP interno do proxy (Traefik)
        request.addHeader("CF-Connecting-IP", "203.0.113.42");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("203.0.113.42");
    }

    @Test
    @DisplayName("deve ignorar X-Forwarded-For (spoofável pelo cliente) e usar fallback seguro")
    void ignoresSpoofableXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        // Tentativa de spoof: atacante envia um XFF arbitrário
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");

        // O XFF NÃO deve ser confiado; cai no fallback do IP remoto da conexão
        assertThat(clientIpResolver.resolve(request)).isEqualTo("10.0.0.5");
        assertThat(clientIpResolver.resolve(request)).isNotEqualTo("1.2.3.4");
    }

    @Test
    @DisplayName("header confiável deve prevalecer mesmo com X-Forwarded-For spoofado presente")
    void trustedHeaderWinsOverSpoofedXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("CF-Connecting-IP", "203.0.113.42");
        request.addHeader("X-Forwarded-For", "1.2.3.4"); // ignorado

        assertThat(clientIpResolver.resolve(request)).isEqualTo("203.0.113.42");
    }

    @Test
    @DisplayName("deve fazer fallback para getRemoteAddr quando não há header confiável")
    void fallsBackToRemoteAddrWhenNoTrustedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("deve fazer fallback quando header confiável está em branco")
    void fallsBackWhenTrustedHeaderIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("CF-Connecting-IP", "   ");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("deve normalizar espaços ao redor do IP do header confiável")
    void trimsTrustedHeaderValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("CF-Connecting-IP", "  203.0.113.42  ");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("203.0.113.42");
    }
}
