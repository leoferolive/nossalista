package br.com.leoferolive.nossalista.mcp.dto;

public record ListSummary(
    String id,
    String name,
    String type,
    boolean owner,
    int itemsCount,
    String updatedAt
) {
}
