package br.com.leoferolive.nossalista.listitem.controller;

import br.com.leoferolive.nossalista.listitem.dto.CreateItemRequestDTO;
import br.com.leoferolive.nossalista.listitem.dto.ListItemResponseDTO;
import br.com.leoferolive.nossalista.listitem.dto.UpdateItemRequest;
import br.com.leoferolive.nossalista.listitem.service.ListItemService;
import br.com.leoferolive.nossalista.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller para gerenciamento de itens de lista
 */
@RestController
@RequestMapping("/api/lists/{listId}/items")
@Tag(name = "Itens de Lista", description = "CRUD de itens dentro de listas compartilhadas")
@SecurityRequirement(name = "JWT")
public class ListItemController {

    private final ListItemService listItemService;

    public ListItemController(ListItemService listItemService) {
        this.listItemService = listItemService;
    }

    /**
     * Adiciona um novo item à lista especificada
     * Usuário deve ser participante da lista (dono ou membro)
     *
     * @param listId  ID da lista onde o item será adicionado
     * @param dto     DTO com dados do item (nome, quantity, dueDate, url)
     * @param creator Usuário autenticado (injetado pelo JWT)
     * @return DTO com dados completos do item criado, status 201 Created
     */
    @PostMapping
    @Operation(
        summary = "Adicionar item à lista",
        description = "Adiciona um novo item à lista especificada. Usuário deve ser dono ou membro da lista. " +
                      "Campos dinâmicos: quantity (Compras), dueDate (Tarefas), url (Wishlist). " +
                      "Position é calculado automaticamente."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Item criado com sucesso",
            content = @Content(schema = @Schema(implementation = ListItemResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos (nome vazio, > 200 chars, campos específicos inválidos)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuário não tem permissão para adicionar itens nesta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<ListItemResponseDTO> addItem(
            @PathVariable UUID listId,
            @Valid @RequestBody CreateItemRequestDTO dto,
            @AuthenticationPrincipal User creator) {

        ListItemResponseDTO response = listItemService.addItem(listId, dto, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retorna todos os itens de uma lista ordenados por position
     * Usuário deve ser participante da lista (dono ou membro)
     *
     * @param listId      ID da lista
     * @param userDetails Usuário autenticado (injetado pelo JWT)
     * @return Lista de DTOs com todos os itens, status 200 OK
     */
    @GetMapping
    @Operation(
        summary = "Listar itens da lista",
        description = "Retorna todos os itens de uma lista ordenados por position. " +
                      "Usuário deve ser dono ou membro da lista."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Itens retornados com sucesso",
            content = @Content(schema = @Schema(implementation = ListItemResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuário não tem permissão para ver os itens desta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<List<ListItemResponseDTO>> getItems(
            @PathVariable UUID listId,
            @AuthenticationPrincipal User user) {

        List<ListItemResponseDTO> items = listItemService.getItemsByListId(listId, user);
        return ResponseEntity.ok(items);
    }

    /**
     * PATCH /api/lists/{listId}/items/{itemId}/check
     * Toggle do estado checked de um item
     */
    @PatchMapping("/{itemId}/check")
    @Operation(
        summary = "Marcar/desmarcar item como concluído",
        description = "Inverte o estado checked do item (toggle). " +
                      "Usuário deve ser dono ou membro da lista."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = ListItemResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuário não tem permissão para modificar itens desta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista ou item não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<ListItemResponseDTO> toggleItemCheck(
            @PathVariable UUID listId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal User user) {

        ListItemResponseDTO updated = listItemService.toggleItemCheck(listId, itemId, user);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/lists/{listId}/items/{itemId}
     * Atualiza um item existente
     */
    @PatchMapping("/{itemId}")
    @Operation(
        summary = "Atualizar item",
        description = "Atualiza campos específicos de um item existente. " +
                      "Campos permitidos dependem do tipo da lista (Compras/Tarefas/Wishlist/Genérica). " +
                      "Usuário deve ser dono ou membro da lista."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = ListItemResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Erro de validação (campos inválidos para o tipo)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuário não tem permissão para modificar itens desta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista ou item não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<ListItemResponseDTO> updateItem(
            @PathVariable UUID listId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest request,
            @AuthenticationPrincipal User user) {

        ListItemResponseDTO updated = listItemService.updateItem(listId, itemId, request, user);
        return ResponseEntity.ok(updated);
    }
}
