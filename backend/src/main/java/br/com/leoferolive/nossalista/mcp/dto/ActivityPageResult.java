package br.com.leoferolive.nossalista.mcp.dto;

import java.util.List;

public record ActivityPageResult(
    List<ActivityEntrySummary> entries,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
