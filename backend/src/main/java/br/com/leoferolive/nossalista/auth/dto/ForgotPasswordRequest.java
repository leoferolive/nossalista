package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requisição para solicitar reset de senha
 *
 * @param email Email do usuário (obrigatório, formato válido)
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    String email
) {
}
