package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para redefinir a senha usando um token
 *
 * @param token       Token de reset (obrigatório)
 * @param newPassword Nova senha (obrigatório, mínimo 6 caracteres)
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Token é obrigatório")
    String token,

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    String newPassword
) {
}
