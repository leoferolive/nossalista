package br.com.leoferolive.nossalista.member.dto;

public record InviteByUsernameResponse(
    String invited_username,
    String message
) {
}
