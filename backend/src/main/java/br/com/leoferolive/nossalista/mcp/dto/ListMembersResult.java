package br.com.leoferolive.nossalista.mcp.dto;

import java.util.List;

public record ListMembersResult(
    List<MemberSummary> members
) {
}
