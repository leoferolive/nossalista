package br.com.leoferolive.nossalista.mcpoauth.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro do endpoint {@code POST /oauth/token} (e {@code /oauth/revoke}), no
 * formato de código de erro OAuth padrão (RFC 6749 §5.2) — o corpo JSON
 * {@code {"error": "...", "error_description": "..."}} é montado a partir de
 * {@link #getErrorCode()} pelo {@code McpOAuthExceptionHandler}, nunca no
 * formato RFC 7807 usado pelo resto da API (clientes OAuth genéricos esperam
 * especificamente esse shape).
 */
public class OAuthTokenException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public OAuthTokenException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public OAuthTokenException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public static OAuthTokenException invalidGrant(String message) {
        return new OAuthTokenException("invalid_grant", message);
    }

    public static OAuthTokenException invalidRequest(String message) {
        return new OAuthTokenException("invalid_request", message);
    }

    public static OAuthTokenException invalidClient(String message) {
        return new OAuthTokenException("invalid_client", message, HttpStatus.UNAUTHORIZED);
    }

    public static OAuthTokenException invalidTarget(String message) {
        return new OAuthTokenException("invalid_target", message);
    }

    public static OAuthTokenException unsupportedGrantType(String message) {
        return new OAuthTokenException("unsupported_grant_type", message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
