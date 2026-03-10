package br.com.leoferolive.nossalista.websocket.dto;

public record MemberJoinedPayload(
    String userId,
    String username,
    String name,
    String avatarUrl,
    String listId,
    String listName
) {}
