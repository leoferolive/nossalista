package br.com.leoferolive.nossalista.mcp.dto;

import java.util.List;

public record RemoveItemsResult(
    int removed,
    int failed,
    List<BatchItemOutcome> results
) {
}
