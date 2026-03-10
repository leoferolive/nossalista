package br.com.leoferolive.nossalista.websocket.dto;

public record MemberLeftPayload(
    String userId,
    String username,
    String listId,
    String listName,
    String reason
) {}
