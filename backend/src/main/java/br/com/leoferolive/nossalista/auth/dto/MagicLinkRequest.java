package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de requisição para solicitar um magic link. */
public record MagicLinkRequest(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    String email
) {
}
