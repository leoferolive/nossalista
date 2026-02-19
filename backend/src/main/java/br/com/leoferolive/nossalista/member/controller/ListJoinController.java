package br.com.leoferolive.nossalista.member.controller;

import br.com.leoferolive.nossalista.list.dto.JoinListResponse;
import br.com.leoferolive.nossalista.member.service.ListJoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para endpoints públicos de join em listas via convite.
 * Estes endpoints não requerem autenticação (modo read-only).
 */
@RestController
@RequestMapping("/api/lists")
@Tag(name = "Convites", description = "Endpoints públicos de convite para visualização de listas")
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
}
