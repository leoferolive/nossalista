package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requisição para reenviar o e-mail de verificação (Q2.7)
 *
 * @param email Email do usuário (obrigatório, formato válido)
 */
public record ResendVerificationRequest(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    String email
) {
}
