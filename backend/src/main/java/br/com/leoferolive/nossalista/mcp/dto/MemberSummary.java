package br.com.leoferolive.nossalista.mcp.dto;

public record MemberSummary(
    String userId,
    String username,
    String name,
    String role,
    String joinedAt
) {
}
