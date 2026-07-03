package br.com.leoferolive.nossalista.mcpoauth.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro de {@code POST /oauth/register} (Dynamic Client Registration, RFC 7591
 * §3.2.2) — o corpo JSON {@code {"error": "...", "error_description": "..."}}
 * é montado a partir de {@link #getErrorCode()} pelo
 * {@code McpOAuthExceptionHandler}, no MESMO formato de {@link OAuthTokenException}
 * (nunca RFC 7807 Problem Details usado pelo resto da API) — clientes DCR
 * genéricos esperam especificamente esse shape.
 */
public class OAuthClientRegistrationException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    private OAuthClientRegistrationException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    /** {@code redirect_uris} ausente, vazio, ou contendo uma URI que viola o hardening (ver D-024). */
    public static OAuthClientRegistrationException invalidRedirectUri(String message) {
        return new OAuthClientRegistrationException("invalid_redirect_uri", message, HttpStatus.BAD_REQUEST);
    }

    /** Metadata do cliente inválida além de {@code redirect_uris} (ex.: {@code grant_types} não suportado). */
    public static OAuthClientRegistrationException invalidClientMetadata(String message) {
        return new OAuthClientRegistrationException("invalid_client_metadata", message, HttpStatus.BAD_REQUEST);
    }

    /** Rate limit por IP excedido (não é um código RFC 7591, mas o formato de corpo é o mesmo). */
    public static OAuthClientRegistrationException tooManyRequests(String message) {
        return new OAuthClientRegistrationException("too_many_requests", message, HttpStatus.TOO_MANY_REQUESTS);
    }

    /** Teto global de clientes registrados atingido — proteção contra flood do banco. */
    public static OAuthClientRegistrationException temporarilyUnavailable(String message) {
        return new OAuthClientRegistrationException("temporarily_unavailable", message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /** {@code app.mcp-oauth.dcr.enabled=false} neste ambiente. */
    public static OAuthClientRegistrationException disabled(String message) {
        return new OAuthClientRegistrationException("access_denied", message, HttpStatus.FORBIDDEN);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
