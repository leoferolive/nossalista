package br.com.leoferolive.nossalista.mcp.tool;

import br.com.leoferolive.nossalista.common.exception.InvalidInputException;
import br.com.leoferolive.nossalista.list.domain.ListType;
import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.dto.UpdateListNameRequest;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.listitem.dto.ListItemResponseDTO;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import br.com.leoferolive.nossalista.listitem.service.ListItemService;
import br.com.leoferolive.nossalista.mcp.dto.CreateListResult;
import br.com.leoferolive.nossalista.mcp.dto.DeleteListResult;
import br.com.leoferolive.nossalista.mcp.dto.GetListResult;
import br.com.leoferolive.nossalista.mcp.dto.ItemSummary;
import br.com.leoferolive.nossalista.mcp.dto.ListMyListsResult;
import br.com.leoferolive.nossalista.mcp.dto.ListSummary;
import br.com.leoferolive.nossalista.mcp.dto.OwnerSummary;
import br.com.leoferolive.nossalista.mcp.dto.RenameListResult;
import br.com.leoferolive.nossalista.mcp.interceptor.McpMutationRateLimiter;
import br.com.leoferolive.nossalista.mcp.security.McpSecurityContext;
import br.com.leoferolive.nossalista.mcp.support.DtoValidator;
import br.com.leoferolive.nossalista.mcp.support.ListNameResolver;
import br.com.leoferolive.nossalista.mcp.support.McpIds;
import br.com.leoferolive.nossalista.mcp.support.McpLimits;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tools MCP para o ciclo de vida de listas: descoberta, leitura, criação,
 * renomeação e exclusão. Toda tool resolve o usuário autenticado a partir de
 * {@link McpSecurityContext#currentUser()} e delega toda regra de negócio e
 * autorização para {@link ListService} — nenhuma checagem de owner/membro é
 * duplicada aqui.
 */
@Component
public class ListMcpTools {

    private static final int DEFAULT_ITEM_LIMIT = 100;

    private final ListService listService;
    private final ListItemService listItemService;
    private final ListItemRepository listItemRepository;
    private final McpSecurityContext security;
    private final DtoValidator validator;
    private final ListNameResolver listNameResolver;
    private final McpMutationRateLimiter mutationRateLimiter;

    public ListMcpTools(
        ListService listService,
        ListItemService listItemService,
        ListItemRepository listItemRepository,
        McpSecurityContext security,
        DtoValidator validator,
        ListNameResolver listNameResolver,
        McpMutationRateLimiter mutationRateLimiter
    ) {
        this.listService = listService;
        this.listItemService = listItemService;
        this.listItemRepository = listItemRepository;
        this.security = security;
        this.validator = validator;
        this.listNameResolver = listNameResolver;
        this.mutationRateLimiter = mutationRateLimiter;
    }

    @McpTool(
        name = "list_my_lists",
        description = "Lists all shared lists the authenticated user owns or is a member of, with "
            + "type and item count. Use this first to discover which lists exist before calling "
            + "other tools by name.",
        generateOutputSchema = true
    )
    public ListMyListsResult listMyLists() {
        User user = security.currentUser();
        List<SharedList> lists = listService.getAllListsForUser(user.getId());
        Map<UUID, Integer> counts = countItems(lists);

        List<ListSummary> summaries = lists.stream()
            .map(list -> toSummary(list, user.getId(), counts.getOrDefault(list.getId(), 0)))
            .toList();
        return new ListMyListsResult(summaries, summaries.size());
    }

    @McpTool(
        name = "get_list",
        description = "Fetches a single list with its items, ordered by position. Identify the list "
            + "by listId (preferred) or by name (case-insensitive; an exact match wins, otherwise "
            + "falls back to a contains match). If more than one list matches the name, returns an "
            + "error listing the candidate ids so you can retry with listId. Use limit/offset to "
            + "paginate items (default: first 100).",
        generateOutputSchema = true
    )
    public GetListResult getList(
        @McpToolParam(required = false, description = "UUID of the list. Provide this OR name.")
        String listId,
        @McpToolParam(required = false, description = "Exact or partial list name (case-insensitive). "
            + "Provide this OR listId.")
        String name,
        @McpToolParam(required = false, description = "Max number of items to return (default 100, maximum "
            + McpLimits.MAX_LIST_ITEMS_PAGE_SIZE + ").")
        Integer limit,
        @McpToolParam(required = false, description = "Number of items to skip before returning "
            + "results (default 0).")
        Integer offset
    ) {
        User user = security.currentUser();
        SharedList list = listNameResolver.resolve(listId, name, user.getId());
        List<ListItemResponseDTO> allItems = listItemService.getItemsByListId(list.getId(), user);
        List<ItemSummary> page = paginate(allItems, offset, limit);

        return new GetListResult(
            list.getId().toString(),
            list.getName(),
            typeName(list),
            list.getOwner().getId().equals(user.getId()),
            toOwnerSummary(list),
            allItems.size(),
            page
        );
    }

    @McpTool(
        name = "create_list",
        description = "Creates a new shared list owned by the authenticated user and returns its "
            + "generated invite code.",
        generateOutputSchema = true
    )
    public CreateListResult createList(
        @McpToolParam(description = "Name of the new list, 3-100 characters.")
        String name,
        @McpToolParam(description = "Type of list: SHOPPING, TASK, WISHLIST or GENERIC.")
        String type
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();
        mutationRateLimiter.enforce();

        CreateListRequest request = new CreateListRequest(name, resolveTypeId(type));
        validator.validate(request);
        SharedList list = listService.createList(request, user);

        return new CreateListResult(list.getId().toString(), list.getName(), typeName(list), list.getInviteCode());
    }

    @McpTool(
        name = "rename_list",
        description = "Renames an existing list. Only the list owner can rename it.",
        generateOutputSchema = true
    )
    public RenameListResult renameList(
        @McpToolParam(description = "UUID of the list to rename.")
        String listId,
        @McpToolParam(description = "New name for the list, 3-100 characters.")
        String name
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();
        mutationRateLimiter.enforce();

        UpdateListNameRequest request = new UpdateListNameRequest(name);
        validator.validate(request);
        SharedList updated = listService.updateListName(McpIds.parseUuid(listId, "listId"), user.getId(), request);

        return new RenameListResult(updated.getId().toString(), updated.getName());
    }

    @McpTool(
        name = "delete_list",
        description = "Permanently deletes a list and all its items and members. Only the list owner "
            + "can delete it. This cannot be undone — always confirm with the user before calling "
            + "this tool.",
        generateOutputSchema = true
    )
    public DeleteListResult deleteList(
        @McpToolParam(description = "UUID of the list to delete.")
        String listId
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();
        mutationRateLimiter.enforce();

        UUID id = McpIds.parseUuid(listId, "listId");
        listService.deleteList(id, user.getId());
        return new DeleteListResult(listId, true);
    }

    private Integer resolveTypeId(String type) {
        if (type == null || type.isBlank()) {
            throw new InvalidInputException("type is required: SHOPPING, TASK, WISHLIST or GENERIC");
        }
        try {
            return ListType.valueOf(type.trim().toUpperCase(Locale.ROOT)).ordinal() + 1;
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException(
                "Invalid type: \"" + type + "\". Use SHOPPING, TASK, WISHLIST or GENERIC");
        }
    }

    private String typeName(SharedList list) {
        return list.getType().name();
    }

    private OwnerSummary toOwnerSummary(SharedList list) {
        User owner = list.getOwner();
        return new OwnerSummary(owner.getId().toString(), owner.getUsername(), owner.getName());
    }

    private ListSummary toSummary(SharedList list, UUID currentUserId, int itemsCount) {
        return new ListSummary(
            list.getId().toString(),
            list.getName(),
            typeName(list),
            list.getOwner().getId().equals(currentUserId),
            itemsCount,
            list.getUpdatedAt() == null ? null : list.getUpdatedAt().toString()
        );
    }

    private ItemSummary toItemSummary(ListItemResponseDTO item) {
        return new ItemSummary(
            item.id().toString(),
            item.name(),
            item.checked(),
            item.quantity(),
            item.dueDate() == null ? null : item.dueDate().toString(),
            item.url(),
            item.position()
        );
    }

    private List<ItemSummary> paginate(List<ListItemResponseDTO> allItems, Integer offset, Integer limit) {
        int effectiveOffset = offset == null || offset < 0 ? 0 : offset;
        int effectiveLimit = McpLimits.requirePageSizeWithinLimit(
            limit, DEFAULT_ITEM_LIMIT, McpLimits.MAX_LIST_ITEMS_PAGE_SIZE, "limit");
        return allItems.stream()
            .skip(effectiveOffset)
            .limit(effectiveLimit)
            .map(this::toItemSummary)
            .toList();
    }

    private Map<UUID, Integer> countItems(List<SharedList> lists) {
        List<UUID> listIds = lists.stream().map(SharedList::getId).toList();
        if (listIds.isEmpty()) {
            return Map.of();
        }
        return listItemRepository.countByListIds(listIds).stream()
            .collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Long) row[1]).intValue()));
    }
}
