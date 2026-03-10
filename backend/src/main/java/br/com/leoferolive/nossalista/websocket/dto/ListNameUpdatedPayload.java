package br.com.leoferolive.nossalista.websocket.dto;

public record ListNameUpdatedPayload(
    String listId,
    String oldName,
    String newName
) {}
