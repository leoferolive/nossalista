package br.com.leoferolive.nossalista.mcp.dto;

public record CreateListResult(
    String id,
    String name,
    String type,
    String inviteCode
) {
}
