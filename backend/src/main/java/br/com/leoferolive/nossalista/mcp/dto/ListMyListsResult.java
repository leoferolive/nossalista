package br.com.leoferolive.nossalista.mcp.dto;

import java.util.List;

public record ListMyListsResult(
    List<ListSummary> lists,
    int count
) {
}
