package br.com.leoferolive.nossalista.member.controller;

import br.com.leoferolive.nossalista.list.dto.JoinListResponse;
import br.com.leoferolive.nossalista.member.dto.ListJoinedResponse;
import br.com.leoferolive.nossalista.member.service.ListJoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para endpoints de join em listas via convite.
 * GET /join/{code}: endpoint público (read-only, sem autenticação).
 * POST /join/{code}: endpoint autenticado (requer JWT — cria membership).
 */
@RestController
@RequestMapping("/api/lists")
@Tag(name = "Convites", description = "Endpoints de convite: visualização pública (GET) e entrada autenticada (POST)")
@Validated
public class ListJoinController {

    private final ListJoinService listJoinService;

    public ListJoinController(ListJoinService listJoinService) {
        this.listJoinService = listJoinService;
    }

    /**
     * Visualiza uma lista via código de convite (modo read-only, sem autenticação).
     *
     * @param inviteCode código de convite da lista
     * @return dados da lista em modo read-only
     */
    @GetMapping("/join/{inviteCode}")
    @Operation(
        summary = "Visualizar lista via convite (read-only, sem autenticação)",
        description = "Endpoint público para visualização de lista via código de convite. " +
                      "Não requer autenticação. Retorna dados em modo READ_ONLY."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista encontrada e retornada em modo read-only",
            content = @Content(schema = @Schema(implementation = JoinListResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Código de convite não encontrado"
        ),
        @ApiResponse(
            responseCode = "410",
            description = "Link de convite expirado"
        )
    })
    public ResponseEntity<JoinListResponse> getListByInviteCode(
        @Parameter(description = "Código de convite (12 caracteres alfanuméricos)", required = true)
        @PathVariable
        @Size(min = 1, max = 20, message = "Código de convite inválido")
        @Pattern(regexp = "[A-Za-z0-9]+", message = "Código de convite deve conter apenas letras e números")
        String inviteCode
    ) {
        JoinListResponse response = listJoinService.getListByInviteCode(inviteCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Entra em uma lista via código de convite (endpoint autenticado).
     * Cria um registro de membro na lista se o usuário ainda não for membro.
     *
     * @param inviteCode  código de convite da lista
     * @param userDetails usuário autenticado (via JWT)
     * @return dados da lista com mensagem de boas-vindas
     */
    @PostMapping("/join/{inviteCode}")
    @Operation(
        summary = "Entrar em uma lista via convite (requer autenticação)",
        description = "Endpoint autenticado para entrar em uma lista via código de convite. " +
                      "Requer JWT válido. Cria um registro de membro se o usuário ainda não for membro. " +
                      "Retorna 201 Created para novo membro, 200 OK se já era membro/dono.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Usuário entrou na lista como novo membro",
            content = @Content(schema = @Schema(implementation = ListJoinedResponse.class))
        ),
        @ApiResponse(
            responseCode = "200",
            description = "Usuário já era membro ou é o dono da lista",
            content = @Content(schema = @Schema(implementation = ListJoinedResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT ausente ou inválido"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Código de convite não encontrado"
        ),
        @ApiResponse(
            responseCode = "410",
            description = "Link de convite expirado"
        )
    })
    public ResponseEntity<ListJoinedResponse> joinList(
        @Parameter(description = "Código de convite (12 caracteres alfanuméricos)", required = true)
        @PathVariable
        @Size(min = 1, max = 20, message = "Código de convite inválido")
        @Pattern(regexp = "[A-Za-z0-9]+", message = "Código de convite deve conter apenas letras e números")
        String inviteCode,
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        UserDetails userDetails
    ) {
        java.util.UUID userId = java.util.UUID.fromString(userDetails.getUsername());
        ListJoinedResponse response = listJoinService.joinList(userId, inviteCode);

        // 201 Created se novo membro foi criado; 200 OK se já era membro/dono
        HttpStatus status = response.created() ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }
}
