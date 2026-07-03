package br.com.leoferolive.nossalista.mcp.dto;

import java.util.List;

public record GetListResult(
    String id,
    String name,
    String type,
    boolean owner,
    OwnerSummary ownedBy,
    int itemsTotalCount,
    List<ItemSummary> items
) {
}
