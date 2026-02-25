package br.com.leoferolive.nossalista.member.controller;

import br.com.leoferolive.nossalista.member.dto.InviteByUsernameRequest;
import br.com.leoferolive.nossalista.member.dto.InviteByUsernameResponse;
import br.com.leoferolive.nossalista.member.dto.ListMemberResponse;
import br.com.leoferolive.nossalista.member.service.MemberService;
import br.com.leoferolive.nossalista.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/lists")
@Tag(name = "Membros", description = "Gestão de membros da lista")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/{id}/invite")
    @Operation(
        summary = "Convidar usuário por username",
        description = "Apenas o dono da lista pode convidar por username",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Membro adicionado",
            content = @Content(schema = @Schema(implementation = InviteByUsernameResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Usuário ou lista não encontrados",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Conflito de convite",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<InviteByUsernameResponse> inviteByUsername(
        @PathVariable UUID id,
        @Valid @RequestBody InviteByUsernameRequest request,
        @AuthenticationPrincipal User authenticatedUser
    ) {
        UUID requesterId = authenticatedUser.getId();
        InviteByUsernameResponse response = memberService.inviteByUsername(id, requesterId, request.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/members")
    @Operation(
        summary = "Listar membros da lista",
        description = "Retorna os membros da lista para OWNER ou MEMBER",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Membros retornados",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListMemberResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<java.util.List<ListMemberResponse>> getMembers(
        @PathVariable UUID id,
        @AuthenticationPrincipal User authenticatedUser
    ) {
        UUID requesterId = authenticatedUser.getId();
        return ResponseEntity.ok(memberService.getMembers(id, requesterId));
    }

    @PostMapping("/{id}/leave")
    @Operation(
        summary = "Sair da lista",
        description = "Permite que um MEMBER saia da lista",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Saiu da lista"),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Owner não pode sair",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Lista não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> leaveList(
        @PathVariable UUID id,
        @AuthenticationPrincipal User authenticatedUser
    ) {
        UUID requesterId = authenticatedUser.getId();
        memberService.leaveList(id, requesterId);
        return ResponseEntity.noContent().header(HttpHeaders.CONTENT_LENGTH, "0").build();
    }
}
