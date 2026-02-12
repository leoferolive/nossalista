package br.com.leoferolive.nossalista.list.controller;

import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.dto.ListMapper;
import br.com.leoferolive.nossalista.list.dto.ListResponse;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller para gerenciamento de listas
 */
@RestController
@RequestMapping("/api/lists")
@Tag(name = "Listas", description = "CRUD de listas compartilhadas")
@SecurityRequirement(name = "JWT")
public class ListController {

    private final ListService listService;
    private final ListMapper listMapper;

    public ListController(ListService listService, ListMapper listMapper) {
        this.listService = listService;
        this.listMapper = listMapper;
    }

    /**
     * Cria uma nova lista para o usuário autenticado
     *
     * @param request DTO com nome e tipo da lista
     * @param owner Usuário autenticado (injetado pelo JWT)
     * @return DTO com dados completos da lista criada
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Criar nova lista",
        description = "Cria uma nova lista associada ao usuário autenticado. Gera automaticamente um código de convite único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Lista criada com sucesso",
            content = @Content(schema = @Schema(implementation = ListResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos (nome < 3 chars, typeId inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ListResponse createList(
            @Valid @RequestBody CreateListRequest request,
            @AuthenticationPrincipal User owner) {
        List list = listService.createList(request, owner);
        return listMapper.toListResponse(list);
    }
}
