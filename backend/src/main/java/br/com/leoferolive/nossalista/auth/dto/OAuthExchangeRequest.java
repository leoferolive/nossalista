package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requisição para troca do one-time code OAuth2 pelo JWT (Q2.3).
 *
 * @param code one-time code opaco recebido no redirect do OAuth2
 */
public record OAuthExchangeRequest(
    @NotBlank(message = "Code é obrigatório")
    String code
) {
}
