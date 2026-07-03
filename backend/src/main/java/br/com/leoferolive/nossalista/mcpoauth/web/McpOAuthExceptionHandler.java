package br.com.leoferolive.nossalista.mcpoauth.web;

import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthClientRegistrationException;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthConsentForbiddenException;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthInvalidRedirectUriException;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthTokenException;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthUnknownClientException;
import br.com.leoferolive.nossalista.mcpoauth.exception.PendingAuthorizationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;

/**
 * Handler de exceções isolado para o módulo {@code mcpoauth} — mantido
 * separado do {@code GlobalExceptionHandler} compartilhado por dois motivos:
 * (1) minimiza conflito de merge com trabalho em paralelo nesse arquivo
 * (Fase D mexe em handlers de erro); (2) {@link OAuthTokenException} exige o
 * formato de erro padrão OAuth (RFC 6749 §5.2:
 * {@code {"error": "...", "error_description": "..."}}), diferente do RFC 7807
 * Problem Details usado no resto da API.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)}: o {@code GlobalExceptionHandler} tem um
 * handler catch-all para {@code Exception.class} sem {@code @Order} explícito.
 * Quando múltiplos beans {@code @RestControllerAdvice} existem, o Spring resolve
 * o handler dentro do PRIMEIRO bean (por ordem) que tiver QUALQUER match — não
 * necessariamente o match mais específico entre todos os beans. Sem esta
 * anotação, o catch-all de {@code GlobalExceptionHandler} venceria antes deste
 * bean ser sequer consultado, convertendo os erros OAuth em 500 genérico.</p>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class McpOAuthExceptionHandler {

    /**
     * Erros de {@code POST /oauth/token} e {@code POST /oauth/revoke} — corpo
     * no formato OAuth padrão, consumido por SDKs de cliente OAuth genéricos.
     */
    @ExceptionHandler(OAuthTokenException.class)
    public ResponseEntity<Map<String, String>> handleOAuthTokenException(OAuthTokenException ex) {
        return ResponseEntity.status(ex.getStatus())
            .body(Map.of("error", ex.getErrorCode(), "error_description", ex.getMessage()));
    }

    /**
     * Erros de {@code POST /oauth/register} (Dynamic Client Registration, RFC
     * 7591 §3.2.2) — mesmo formato de corpo de {@link OAuthTokenException}.
     */
    @ExceptionHandler(OAuthClientRegistrationException.class)
    public ResponseEntity<Map<String, String>> handleClientRegistrationException(OAuthClientRegistrationException ex) {
        return ResponseEntity.status(ex.getStatus())
            .body(Map.of("error", ex.getErrorCode(), "error_description", ex.getMessage()));
    }

    /**
     * {@code client_id} desconhecido em {@code GET /oauth/authorize} — nunca
     * seguro redirecionar (redirect_uri do atacante não é confiável).
     */
    @ExceptionHandler(OAuthUnknownClientException.class)
    public ResponseEntity<ProblemDetail> handleUnknownClient(OAuthUnknownClientException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), "invalid-client", "Cliente OAuth desconhecido", request);
    }

    /**
     * {@code redirect_uri} não registrado em {@code GET /oauth/authorize} —
     * nunca seguro redirecionar (evita open redirect).
     */
    @ExceptionHandler(OAuthInvalidRedirectUriException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRedirectUri(
        OAuthInvalidRedirectUriException ex, HttpServletRequest request
    ) {
        return problem(
            HttpStatus.BAD_REQUEST, ex.getMessage(), "invalid-redirect-uri", "redirect_uri inválido", request);
    }

    /**
     * Pedido de autorização pendente inexistente/expirado/já decidido — usado
     * pela tela de consentimento da SPA.
     */
    @ExceptionHandler(PendingAuthorizationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePendingNotFound(
        PendingAuthorizationNotFoundException ex, HttpServletRequest request
    ) {
        return problem(
            HttpStatus.NOT_FOUND, ex.getMessage(), "oauth-authorization-request-not-found",
            "Pedido de autorização não encontrado", request);
    }

    /**
     * Cookie de vínculo ausente/incorreto, ou pedido reivindicado por outro
     * usuário — defesa contra sequestro de consentimento cross-user (achado do
     * QA, ver Javadoc de {@code PendingAuthorization}/D-022).
     */
    @ExceptionHandler(OAuthConsentForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleConsentForbidden(
        OAuthConsentForbiddenException ex, HttpServletRequest request
    ) {
        return problem(
            HttpStatus.FORBIDDEN, ex.getMessage(), "oauth-consent-forbidden", "Consentimento não autorizado", request);
    }

    private ResponseEntity<ProblemDetail> problem(
        HttpStatus status, String detail, String typeSlug, String title, HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/" + typeSlug));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }
}
