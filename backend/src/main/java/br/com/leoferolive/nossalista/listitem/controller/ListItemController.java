package br.com.leoferolive.nossalista.listitem.controller;

import br.com.leoferolive.nossalista.listitem.dto.CreateItemRequestDTO;
import br.com.leoferolive.nossalista.listitem.dto.ListItemResponseDTO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
