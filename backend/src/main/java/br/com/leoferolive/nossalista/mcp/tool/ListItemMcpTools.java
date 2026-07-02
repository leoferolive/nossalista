package br.com.leoferolive.nossalista.mcp.tool;

import br.com.leoferolive.nossalista.common.exception.InvalidInputException;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.listitem.dto.CreateItemRequestDTO;
import br.com.leoferolive.nossalista.listitem.dto.ListItemResponseDTO;
import br.com.leoferolive.nossalista.listitem.dto.UpdateItemRequest;
import br.com.leoferolive.nossalista.listitem.exception.ItemNotFoundException;
import br.com.leoferolive.nossalista.listitem.service.ListItemService;
import br.com.leoferolive.nossalista.mcp.dto.AddItemInput;
import br.com.leoferolive.nossalista.mcp.dto.AddItemsResult;
import br.com.leoferolive.nossalista.mcp.dto.BatchItemOutcome;
import br.com.leoferolive.nossalista.mcp.dto.RemoveItemsResult;
import br.com.leoferolive.nossalista.mcp.dto.SetItemsCheckedResult;
import br.com.leoferolive.nossalista.mcp.dto.UpdateItemResult;
import br.com.leoferolive.nossalista.mcp.security.McpSecurityContext;
import br.com.leoferolive.nossalista.mcp.support.DtoValidator;
import br.com.leoferolive.nossalista.mcp.support.McpIds;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tools MCP para itens de lista: adicionar em lote, editar, marcar/desmarcar
 * em lote e remover em lote. Cada tool de mutação chama
 * {@link McpSecurityContext#requireWriteAccess()} primeiro — PATs de escopo
 * READ nunca chegam a tocar {@link ListItemService}.
 */
@Component
public class ListItemMcpTools {

    private final ListService listService;
    private final ListItemService listItemService;
    private final McpSecurityContext security;
    private final DtoValidator validator;

    public ListItemMcpTools(
        ListService listService,
        ListItemService listItemService,
        McpSecurityContext security,
        DtoValidator validator
    ) {
        this.listService = listService;
        this.listItemService = listItemService;
        this.security = security;
        this.validator = validator;
    }

    @McpTool(
        name = "add_items",
        description = "Adds one or more items to a list in a single batch call. Each item may "
            + "include quantity (SHOPPING lists), dueDate (TASK lists, ISO-8601) or url (WISHLIST "
            + "lists) — fields not supported by the list's type are rejected for that item. If some "
            + "items in the batch are invalid, the valid ones are still added and each item's "
            + "outcome is reported individually. Use this when the user shares a photo of a "
            + "handwritten/shopping list: extract the items from the image and add them in one batch.",
        generateOutputSchema = true
    )
    public AddItemsResult addItems(
        @McpToolParam(description = "UUID of the list to add items to.")
        String listId,
        @McpToolParam(description = "Items to add. Each item needs a name; quantity, dueDate and url "
            + "are optional and depend on the list type.")
        List<AddItemInput> items
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();

        UUID id = McpIds.parseUuid(listId, "listId");
        listService.getListById(id, user.getId());
        if (items == null || items.isEmpty()) {
            throw new InvalidInputException("items não pode ser vazio");
        }

        List<BatchItemOutcome> outcomes = items.stream().map(item -> addOneItem(id, item, user)).toList();
        int added = (int) outcomes.stream().filter(BatchItemOutcome::success).count();
        return new AddItemsResult(added, outcomes.size() - added, outcomes);
    }

    @McpTool(
        name = "update_item",
        description = "Updates one or more fields of an existing item. Only the fields you provide "
            + "are changed; omitted fields keep their current value. Field availability depends on "
            + "the list type (quantity for SHOPPING, dueDate for TASK, url for WISHLIST).",
        generateOutputSchema = true
    )
    public UpdateItemResult updateItem(
        @McpToolParam(description = "UUID of the list containing the item.")
        String listId,
        @McpToolParam(description = "UUID of the item to update.")
        String itemId,
        @McpToolParam(required = false, description = "New name for the item.")
        String name,
        @McpToolParam(required = false, description = "New quantity (SHOPPING lists only).")
        Integer quantity,
        @McpToolParam(required = false, description = "New due date, ISO-8601 (TASK lists only).")
        String dueDate,
        @McpToolParam(required = false, description = "New URL (WISHLIST lists only).")
        String url
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();

        UUID listUuid = McpIds.parseUuid(listId, "listId");
        UUID itemUuid = McpIds.parseUuid(itemId, "itemId");
        UpdateItemRequest request = buildAndValidatePartialUpdate(name, quantity, dueDate, url);

        ListItemResponseDTO updated = listItemService.updateItem(listUuid, itemUuid, request, user);
        return toUpdateItemResult(updated);
    }

    @McpTool(
        name = "set_items_checked",
        description = "Marks or unmarks a batch of items as checked/done in a single call. Reports "
            + "success/failure per item id.",
        generateOutputSchema = true
    )
    public SetItemsCheckedResult setItemsChecked(
        @McpToolParam(description = "UUID of the list containing the items.")
        String listId,
        @McpToolParam(description = "Item UUIDs to update.")
        List<String> itemIds,
        @McpToolParam(description = "Target checked state to apply to all the given items.")
        boolean checked
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();

        UUID listUuid = McpIds.parseUuid(listId, "listId");
        listService.getListById(listUuid, user.getId());
        requireNonEmptyIds(itemIds);

        Map<UUID, ListItemResponseDTO> currentItems = listItemService.getItemsByListId(listUuid, user).stream()
            .collect(Collectors.toMap(ListItemResponseDTO::id, item -> item));

        List<BatchItemOutcome> outcomes = itemIds.stream()
            .map(rawId -> setOneItemChecked(listUuid, rawId, checked, currentItems, user))
            .toList();
        int updated = (int) outcomes.stream().filter(BatchItemOutcome::success).count();
        return new SetItemsCheckedResult(updated, outcomes.size() - updated, outcomes);
    }

    @McpTool(
        name = "remove_items",
        description = "Removes a batch of items from a list in a single call. Reports success/failure "
            + "per item id.",
        generateOutputSchema = true
    )
    public RemoveItemsResult removeItems(
        @McpToolParam(description = "UUID of the list containing the items.")
        String listId,
        @McpToolParam(description = "Item UUIDs to remove.")
        List<String> itemIds
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();

        UUID listUuid = McpIds.parseUuid(listId, "listId");
        listService.getListById(listUuid, user.getId());
        requireNonEmptyIds(itemIds);

        List<BatchItemOutcome> outcomes = itemIds.stream()
            .map(rawId -> removeOneItem(listUuid, rawId, user))
            .toList();
        int removed = (int) outcomes.stream().filter(BatchItemOutcome::success).count();
        return new RemoveItemsResult(removed, outcomes.size() - removed, outcomes);
    }

    private BatchItemOutcome addOneItem(UUID listId, AddItemInput item, User user) {
        try {
            LocalDateTime dueDate = McpIds.parseDateTime(item.dueDate(), "dueDate");
            CreateItemRequestDTO dto = new CreateItemRequestDTO(item.name(), item.quantity(), dueDate, item.url(), null);
            validator.validate(dto);
            ListItemResponseDTO created = listItemService.addItem(listId, dto, user);
            return new BatchItemOutcome(created.id().toString(), created.name(), true, null);
        } catch (RuntimeException ex) {
            return new BatchItemOutcome(null, item == null ? null : item.name(), false, ex.getMessage());
        }
    }

    private BatchItemOutcome setOneItemChecked(
        UUID listId,
        String rawItemId,
        boolean checked,
        Map<UUID, ListItemResponseDTO> currentItems,
        User user
    ) {
        try {
            UUID itemId = McpIds.parseUuid(rawItemId, "itemId");
            ListItemResponseDTO current = currentItems.get(itemId);
            if (current == null) {
                throw new ItemNotFoundException("Item não encontrado nesta lista");
            }
            ListItemResponseDTO result = current.checked() == checked
                ? current
                : listItemService.toggleItemCheck(listId, itemId, user);
            return new BatchItemOutcome(itemId.toString(), result.name(), true, null);
        } catch (RuntimeException ex) {
            return new BatchItemOutcome(rawItemId, null, false, ex.getMessage());
        }
    }

    private BatchItemOutcome removeOneItem(UUID listId, String rawItemId, User user) {
        try {
            UUID itemId = McpIds.parseUuid(rawItemId, "itemId");
            listItemService.deleteItem(listId, itemId, user);
            return new BatchItemOutcome(itemId.toString(), null, true, null);
        } catch (RuntimeException ex) {
            return new BatchItemOutcome(rawItemId, null, false, ex.getMessage());
        }
    }

    private UpdateItemRequest buildAndValidatePartialUpdate(String name, Integer quantity, String dueDate, String url) {
        UpdateItemRequest request = new UpdateItemRequest();
        request.setName(name);
        request.setQuantity(quantity);
        request.setDueDate(McpIds.parseDateTime(dueDate, "dueDate"));
        request.setUrl(url);

        if (name != null) {
            validator.validateProperty(request, "name");
        }
        if (quantity != null) {
            validator.validateProperty(request, "quantity");
        }
        if (request.getDueDate() != null) {
            validator.validateProperty(request, "dueDate");
        }
        if (url != null) {
            validator.validateProperty(request, "url");
        }
        return request;
    }

    private UpdateItemResult toUpdateItemResult(ListItemResponseDTO item) {
        return new UpdateItemResult(
            item.id().toString(),
            item.name(),
            item.checked(),
            item.quantity(),
            item.dueDate() == null ? null : item.dueDate().toString(),
            item.url()
        );
    }

    private void requireNonEmptyIds(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new InvalidInputException("itemIds não pode ser vazio");
        }
    }
}
