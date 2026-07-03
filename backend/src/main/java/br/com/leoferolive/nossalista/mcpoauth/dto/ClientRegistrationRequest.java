package br.com.leoferolive.nossalista.mcpoauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Corpo de {@code POST /oauth/register} (client metadata, RFC 7591 §2). Todos
 * os campos são opcionais no JSON de entrada — ausência/valor inválido é
 * resolvido com default ou rejeitado por {@code McpOAuthDynamicClientService}
 * (nunca aqui, este DTO é só transporte).
 */
public record ClientRegistrationRequest(
    @JsonProperty("redirect_uris") List<String> redirectUris,
    @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod,
    @JsonProperty("grant_types") List<String> grantTypes,
    @JsonProperty("response_types") List<String> responseTypes,
    String scope,
    @JsonProperty("client_name") String clientName,
    @JsonProperty("client_uri") String clientUri
) {
}
