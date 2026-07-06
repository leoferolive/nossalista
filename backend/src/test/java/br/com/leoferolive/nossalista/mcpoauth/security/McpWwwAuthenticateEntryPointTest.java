package br.com.leoferolive.nossalista.mcpoauth.security;

import br.com.leoferolive.nossalista.config.ClientIpResolver;
import br.com.leoferolive.nossalista.config.Http401UnauthorizedEntryPoint;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica que a negacao 401 em {@code /mcp} loga o IP real do cliente
 * ({@code CF-Connecting-IP}) e o {@code User-Agent} — a observabilidade que
 * permite identificar QUEM tenta acessar o servidor MCP sem credencial valida
 * sem recorrer a captura de pacotes. Confirma tambem que o comportamento
 * original (header {@code WWW-Authenticate} + delegacao do corpo 401) e
 * preservado.
 */
class McpWwwAuthenticateEntryPointTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Http401UnauthorizedEntryPoint delegate;
    private McpWwwAuthenticateEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        delegate = mock(Http401UnauthorizedEntryPoint.class);
        McpOAuthProperties properties = mock(McpOAuthProperties.class);
        when(properties.getIssuer()).thenReturn("https://nossalista.leoferolive.com.br");
        entryPoint = new McpWwwAuthenticateEntryPoint(delegate, properties, new ClientIpResolver());

        logger = (Logger) LoggerFactory.getLogger(McpWwwAuthenticateEntryPoint.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("loga CF-Connecting-IP e User-Agent na negacao 401, preservando WWW-Authenticate e delegacao")
    void logsRealClientIpAndUserAgentOnDenial() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/mcp");
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.7");
        when(request.getHeader("User-Agent")).thenReturn("SomeMcpClient/1.2");
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = mock(AuthenticationException.class);
        when(authException.getMessage()).thenReturn("Full authentication is required to access this resource");

        entryPoint.commence(request, response, authException);

        // Comportamento original preservado: header de descoberta OAuth + delegacao do corpo 401.
        verify(response).setHeader(eq("WWW-Authenticate"), contains("oauth-protected-resource"));
        verify(delegate).commence(request, response, authException);

        // Log com o IP real (CF-Connecting-IP, nao o do proxy) + User-Agent + path.
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
            .contains("ip=203.0.113.7")
            .contains("SomeMcpClient/1.2")
            .contains("uri=/mcp");
    }

    @Test
    @DisplayName("User-Agent ausente nao quebra o log (usa string vazia)")
    void toleratesMissingUserAgent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/mcp");
        when(request.getHeader("CF-Connecting-IP")).thenReturn("198.51.100.9");
        when(request.getHeader("User-Agent")).thenReturn(null);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = mock(AuthenticationException.class);
        when(authException.getMessage()).thenReturn("denied");

        entryPoint.commence(request, response, authException);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("ip=198.51.100.9");
    }
}
