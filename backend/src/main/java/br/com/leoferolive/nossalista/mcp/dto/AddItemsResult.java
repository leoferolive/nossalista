package br.com.leoferolive.nossalista.mcp.dto;

import java.util.List;

public record AddItemsResult(
    int added,
    int failed,
    List<BatchItemOutcome> results
) {
}
