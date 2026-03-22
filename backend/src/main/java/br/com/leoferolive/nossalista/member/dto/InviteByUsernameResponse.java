package br.com.leoferolive.nossalista.member.dto;

public record InviteByUsernameResponse(
    String invitedUsername,
    String message
) {
}
