package br.com.leoferolive.nossalista.member.dto;

import jakarta.validation.constraints.NotBlank;

public record InviteByUsernameRequest(
    @NotBlank(message = "Username é obrigatório")
    String username
) {
}
