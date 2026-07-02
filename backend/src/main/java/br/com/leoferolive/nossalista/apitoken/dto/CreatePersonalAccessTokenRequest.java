package br.com.leoferolive.nossalista.apitoken.dto;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para criação de um Personal Access Token.
 *
 * @param name           nome descritivo do token (ex.: "Claude Desktop")
 * @param scope          escopo de acesso (READ ou READ_WRITE)
 * @param expiresInDays  validade em dias a partir de agora; {@code null} = sem expiração
 */
public record CreatePersonalAccessTokenRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 1, max = 100, message = "Nome deve ter entre 1 e 100 caracteres")
    String name,

    @NotNull(message = "Escopo é obrigatório")
    TokenScope scope,

    @Positive(message = "Prazo de expiração deve ser positivo")
    @Max(value = 3650, message = "Prazo de expiração máximo é 3650 dias")
    Integer expiresInDays
) {
}
