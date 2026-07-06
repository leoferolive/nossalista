package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO de requisição para consumir um magic link e autenticar. */
public record MagicLoginRequest(
    @NotBlank(message = "Token é obrigatório")
    String token
) {
}
