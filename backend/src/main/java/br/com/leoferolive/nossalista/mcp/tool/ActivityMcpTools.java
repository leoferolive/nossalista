package br.com.leoferolive.nossalista.mcp.tool;

import br.com.leoferolive.nossalista.activity.dto.ActivityLogResponse;
import br.com.leoferolive.nossalista.activity.dto.ActivityPageResponse;
import br.com.leoferolive.nossalista.activity.service.ActivityLogService;
import br.com.leoferolive.nossalista.mcp.dto.ActivityEntrySummary;
import br.com.leoferolive.nossalista.mcp.dto.ActivityPageResult;
import br.com.leoferolive.nossalista.mcp.interceptor.McpToolMetrics;
import br.com.leoferolive.nossalista.mcp.security.McpSecurityContext;
import br.com.leoferolive.nossalista.mcp.support.McpIds;
import br.com.leoferolive.nossalista.mcp.support.McpLimits;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tool MCP de leitura do histórico de atividades de uma lista. Delega
 * integralmente para {@link ActivityLogService#getActivities}, que já
 * verifica se o usuário é owner/membro da lista antes de retornar qualquer dado.
 */
@Component
public class ActivityMcpTools {

    private static final int DEFAULT_PAGE_SIZE = 50;

    private final ActivityLogService activityLogService;
    private final McpSecurityContext security;
    private final McpToolMetrics metrics;

    public ActivityMcpTools(
        ActivityLogService activityLogService,
        McpSecurityContext security,
        McpToolMetrics metrics
    ) {
        this.activityLogService = activityLogService;
        this.security = security;
        this.metrics = metrics;
    }

    @McpTool(
        name = "get_list_activity",
        description = "Returns the paginated activity history of a list (items added/checked/"
            + "updated/removed, members joined/left/removed).",
        generateOutputSchema = true
    )
    public ActivityPageResult getListActivity(
        @McpToolParam(description = "UUID of the list.")
        String listId,
        @McpToolParam(required = false, description = "Zero-based page number (default 0).")
        Integer page,
        @McpToolParam(required = false, description = "Page size (default 50, maximum "
            + McpLimits.MAX_ACTIVITY_PAGE_SIZE + ").")
        Integer size
    ) {
        return metrics.record("get_list_activity", () -> {
            User user = security.currentUser();
            UUID id = McpIds.parseUuid(listId, "listId");
            int effectivePage = page == null || page < 0 ? 0 : page;
            int effectiveSize = McpLimits.requirePageSizeWithinLimit(
                size, DEFAULT_PAGE_SIZE, McpLimits.MAX_ACTIVITY_PAGE_SIZE, "size");

            ActivityPageResponse response =
                activityLogService.getActivities(id, user.getId(), effectivePage, effectiveSize);
            var entries = response.content().stream().map(this::toSummary).toList();
            return new ActivityPageResult(
                entries, response.number(), response.size(), response.totalElements(), response.totalPages());
        });
    }

    private ActivityEntrySummary toSummary(ActivityLogResponse entry) {
        return new ActivityEntrySummary(
            entry.id().toString(),
            entry.userName(),
            entry.action(),
            entry.targetType(),
            entry.targetName(),
            entry.details(),
            entry.createdAt() == null ? null : entry.createdAt().toString()
        );
    }
}
