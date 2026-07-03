package br.com.leoferolive.nossalista.mcpoauth.dto;

import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRegisteredClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Corpo de resposta de {@code POST /oauth/register} (RFC 7591 §3.2.1) —
 * ecoa a metadata registrada mais {@code client_id}/{@code client_id_issued_at}.
 *
 * <p>Sem {@code client_secret}: todo cliente DCR aqui é PÚBLICO (PKCE,
 * {@code token_endpoint_auth_method=none}, mesma regra dos clientes estáticos
 * claude-ai/claude-code) — ver docs/DECISIONS.md D-024.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientRegistrationResponse(
    @JsonProperty("client_id") String clientId,
    @JsonProperty("client_id_issued_at") long clientIdIssuedAt,
    @JsonProperty("redirect_uris") List<String> redirectUris,
    @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod,
    @JsonProperty("grant_types") List<String> grantTypes,
    @JsonProperty("response_types") List<String> responseTypes,
    String scope,
    @JsonProperty("client_name") String clientName
) {

    private static final List<String> SUPPORTED_RESPONSE_TYPES = List.of("code");

    public static ClientRegistrationResponse from(McpOAuthRegisteredClient entity) {
        return new ClientRegistrationResponse(
            entity.getClientId(),
            entity.getCreatedAt().toEpochSecond(ZoneOffset.UTC),
            entity.getRedirectUris(),
            entity.getTokenEndpointAuthMethod(),
            entity.getGrantTypes(),
            SUPPORTED_RESPONSE_TYPES,
            entity.getScope(),
            entity.getClientName()
        );
    }
}
