package br.com.leoferolive.nossalista.mcpoauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Corpo de resposta de {@code POST /oauth/token} (RFC 6749 §5.1).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresInSeconds,
    @JsonProperty("refresh_token") String refreshToken,
    String scope
) {
    public static TokenResponse of(String accessToken, long expiresInSeconds, String refreshToken, String scope) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds, refreshToken, scope);
    }
}
