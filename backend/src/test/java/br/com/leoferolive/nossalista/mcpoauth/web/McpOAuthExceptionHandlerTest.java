package br.com.leoferolive.nossalista.mcpoauth.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do handler de {@link HttpMessageNotReadableException} —
 * achado do QA (Fase C.1): um corpo JSON mal-formado ou com campo de tipo
 * errado em {@code POST /oauth/register} virava um 500 genérico antes desta
 * correção (sem handler dedicado em lugar nenhum da API). Cobre também que a
 * mesma correção, aplicada a QUALQUER outro endpoint (esta exceção não é
 * escopada por controller), responde 400 no formato genérico da API
 * (ProblemDetail), NUNCA no formato OAuth — ver Javadoc de
 * {@link McpOAuthExceptionHandler#handleMessageNotReadable}.
 */
class McpOAuthExceptionHandlerTest {

    private final McpOAuthExceptionHandler handler = new McpOAuthExceptionHandler();

    @Test
    @DisplayName("corpo mal-formado em /oauth/register responde 400 no formato OAuth (invalid_client_metadata)")
    void handlesMessageNotReadableOnRegisterEndpointWithOAuthFormat() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/oauth/register");
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException("redirect_uris must be an array", mock(HttpInputMessage.class));

        ResponseEntity<?> response = handler.handleMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsEntry("error", "invalid_client_metadata");
        assertThat(body).containsKey("error_description");
    }

    @Test
    @DisplayName("corpo mal-formado em QUALQUER outro endpoint responde 400 ProblemDetail genérico, nunca o formato OAuth")
    void handlesMessageNotReadableOnOtherEndpointsWithGenericProblemDetail() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/lists");
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException("malformed body", mock(HttpInputMessage.class));

        ResponseEntity<?> response = handler.handleMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem.getInstance()).hasToString("/api/lists");
        assertThat(problem.getType()).hasToString("https://api.nossalista.com/docs/errors/malformed-request-body");
    }
}
