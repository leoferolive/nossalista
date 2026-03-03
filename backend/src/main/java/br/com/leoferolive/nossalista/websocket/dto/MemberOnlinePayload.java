package br.com.leoferolive.nossalista.websocket.dto;

public record MemberOnlinePayload(String userId, String username, String name, String avatarUrl) {
}
