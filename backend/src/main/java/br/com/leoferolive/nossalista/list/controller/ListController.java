package br.com.leoferolive.nossalista.list.controller;

import br.com.leoferolive.nossalista.activity.dto.ActivityPageResponse;
import br.com.leoferolive.nossalista.activity.service.ActivityLogService;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.dto.UpdateListNameRequest;
import br.com.leoferolive.nossalista.list.dto.ListMapper;
import br.com.leoferolive.nossalista.list.dto.ListResponse;
import br.com.leoferolive.nossalista.list.dto.ListStateResponse;
import br.com.leoferolive.nossalista.list.dto.InviteLinkResponse;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
    private final ActivityLogService activityLogService;

    @Value("${frontend.url}")
    private String frontendBaseUrl;

    public ListController(ListService listService, ListMapper listMapper, ActivityLogService activityLogService) {
        this.listService = listService;
        this.listMapper = listMapper;
        this.activityLogService = activityLogService;
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
        // Usa toListResponse(list) sem currentUserId - mapper usa owner.getId() automaticamente
        return listMapper.toListResponse(list);
    }

    /**
     * Lista todas as listas do usuário autenticado
     * Retorna listas onde o usuário é owner OU member, ordenadas por updatedAt DESC
     *
     * @param authenticatedUser Usuário autenticado (injetado pelo JWT)
     * @return Lista de DTOs com dados completos das listas
     */
    @GetMapping
    @Operation(
        summary = "Listar listas do usuário",
        description = "Retorna todas as listas onde o usuário é dono ou membro, ordenadas por data de atualização"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Listas retornadas com sucesso (array vazio se sem listas)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListResponse.class)))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public java.util.List<ListResponse> getAllLists(@AuthenticationPrincipal User authenticatedUser) {
        // Passa currentUserId explicitamente para calcular isOwner corretamente
        // (usuário autenticado pode não ser owner em listas compartilhadas)
        return listService.getAllListsForUser(authenticatedUser.getId()).stream()
                .map(list -> listMapper.toListResponse(list, authenticatedUser.getId()))
                .toList();
    }

    /**
     * Obtém detalhes de uma lista específica
     * Retorna dados completos da lista se o usuário for owner ou member
     *
     * @param id                ID da lista (UUID)
     * @param authenticatedUser Usuário autenticado (injetado pelo JWT)
     * @return DTO com dados completos da lista
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Obter detalhes de uma lista",
        description = "Retorna detalhes completos de uma lista específica. Usuário deve ser dono ou membro."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = ListResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuário não tem permissão para acessar esta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ListResponse getListById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User authenticatedUser) {
        List list = listService.getListById(id, authenticatedUser.getId());
        // Passa currentUserId explicitamente para calcular isOwner corretamente
        return listMapper.toListResponse(list, authenticatedUser.getId());
    }

    @GetMapping("/{id}/state")
    @Operation(
        summary = "Obter estado leve da lista",
        description = "Retorna revision, updatedAt e contagem de itens para sincronização leve da lista."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Estado leve retornado com sucesso",
            content = @Content(schema = @Schema(implementation = ListStateResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuário não tem permissão para acessar esta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ListStateResponse getListState(
            @PathVariable UUID id,
            @AuthenticationPrincipal User authenticatedUser) {
        return listService.getListState(id, authenticatedUser.getId());
    }

    @GetMapping("/{id}/activity")
    @Operation(
        summary = "Listar atividades da lista",
        description = "Retorna o histórico paginado de atividades da lista para owner ou membros."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Atividades retornadas com sucesso",
            content = @Content(schema = @Schema(implementation = ActivityPageResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuário não tem permissão para acessar esta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ActivityPageResponse getActivity(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User authenticatedUser) {
        return activityLogService.getActivities(id, authenticatedUser.getId(), page, size);
    }

    /**
     * Atualiza o nome de uma lista existente
     * Apenas o dono da lista pode editar o nome
     *
     * @param id                ID da lista (UUID)
     * @param request           DTO com o novo nome da lista
     * @param authenticatedUser Usuário autenticado (injetado pelo JWT)
     * @return DTO com dados completos da lista atualizada
     */
    @PatchMapping("/{id}")
    @Operation(
        summary = "Atualizar nome da lista",
        description = "Atualiza o nome de uma lista existente. Apenas o dono pode editar. O tipo da lista não pode ser alterado."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Nome atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = ListResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Nome inválido (< 3 caracteres) ou tentativa de alterar typeId",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Apenas o dono pode editar esta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ListResponse updateListName(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateListNameRequest request,
            @AuthenticationPrincipal User authenticatedUser) {
        List list = listService.updateListName(id, authenticatedUser.getId(), request);
        return listMapper.toListResponse(list, authenticatedUser.getId());
    }

    /**
     * Exclui uma lista existente
     * Apenas o dono da lista pode excluí-la
     * Itens e membros são deletados automaticamente via CASCADE
     *
     * @param id                ID da lista (UUID)
     * @param authenticatedUser Usuário autenticado (injetado pelo JWT)
     * @return ResponseEntity com status 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Excluir lista",
        description = "Exclui uma lista permanentemente. Apenas o dono pode realizar esta ação. Todos os itens e membros serão removidos automaticamente."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Lista excluída com sucesso"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Apenas o dono pode excluir esta lista",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<Void> deleteList(
            @PathVariable UUID id,
            @AuthenticationPrincipal User authenticatedUser) {
        listService.deleteList(id, authenticatedUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Gera ou retorna um link de convite válido para a lista
     * Apenas o dono da lista pode gerar links de convite
     *
     * @param id                ID da lista (UUID)
     * @param authenticatedUser Usuário autenticado (injetado pelo JWT)
     * @return DTO com código de convite, link completo e data de expiração
     */
    @PostMapping("/{id}/invite-link")
    @Operation(
        summary = "Gerar link de convite",
        description = "Gera ou retorna link de convite válido. Apenas o dono pode gerar. Links expiram em 24 horas."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Link gerado ou reutilizado com sucesso",
            content = @Content(schema = @Schema(implementation = InviteLinkResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado (JWT ausente ou inválido)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Apenas o dono pode gerar link de convite",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public InviteLinkResponse generateInviteLink(
            @PathVariable UUID id,
            @AuthenticationPrincipal User authenticatedUser) {
        List list = listService.generateInviteLink(id, authenticatedUser.getId());

        String inviteLink = frontendBaseUrl + "/join/" + list.getInviteCode();

        return new InviteLinkResponse(
                list.getInviteCode(),
                inviteLink,
                list.getInviteExpiresAt()
        );
    }
}
