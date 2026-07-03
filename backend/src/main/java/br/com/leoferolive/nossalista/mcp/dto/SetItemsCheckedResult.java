package br.com.leoferolive.nossalista.mcp.dto;

import java.util.List;

public record SetItemsCheckedResult(
    int updated,
    int failed,
    List<BatchItemOutcome> results
) {
}
