package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requisição para login de usuário
 *
 * @param email    Email do usuário (obrigatório)
 * @param password Senha do usuário (obrigatório)
 */
public record LoginRequest(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    String password
) {
}
