package br.com.leoferolive.nossalista.apitoken.controller;

import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.dto.PersonalAccessTokenCreatedResponse;
import br.com.leoferolive.nossalista.apitoken.dto.PersonalAccessTokenResponse;
import br.com.leoferolive.nossalista.apitoken.service.PersonalAccessTokenService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.exception.NotAuthenticatedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gestão de Personal Access Tokens (PAT) do usuário
 * autenticado.
 *
 * <p><b>Restrição de segurança:</b> estes endpoints só podem ser acessados
 * com JWT de sessão normal — um PAT não pode gerenciar tokens (nem criar,
 * nem listar, nem revogar). Isso é imposto em {@code SecurityConfig}, não
 * neste controller (ver {@code PatAuthorizationSupport}).</p>
 */
@RestController
@RequestMapping("/api/users/me/tokens")
@Tag(name = "Personal Access Tokens", description = "Gestão de tokens de acesso pessoal para integrações MCP/API")
public class PersonalAccessTokenController {

    private final PersonalAccessTokenService tokenService;

    public PersonalAccessTokenController(PersonalAccessTokenService tokenService) {
        this.tokenService = tokenService;
    }

    private User getCurrentAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new NotAuthenticatedException("Usuário não autenticado");
        }

        return (User) auth.getPrincipal();
    }

    /**
     * Cria um novo Personal Access Token para o usuário autenticado.
     *
     * @param request nome, escopo e validade opcional do token
     * @return token em claro (única exposição) e metadados
     */
    @PostMapping
    @Operation(
        summary = "Criar Personal Access Token",
        description = "Cria um novo PAT para autenticar clientes MCP/API externos. "
            + "O valor em claro do token só é retornado nesta resposta — o backend guarda apenas o hash."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Token criado com sucesso (valor em claro incluso)"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Requisição autenticada via PAT (não pode gerenciar tokens)"),
        @ApiResponse(responseCode = "409", description = "Limite de tokens ativos atingido")
    })
    public ResponseEntity<PersonalAccessTokenCreatedResponse> create(
        @Valid @RequestBody CreatePersonalAccessTokenRequest request
    ) {
        User user = getCurrentAuthenticatedUser();
        PersonalAccessTokenCreatedResponse response = tokenService.create(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista os Personal Access Tokens do usuário autenticado (metadados, sem o token/hash).
     *
     * @return lista de tokens, mais recentes primeiro
     */
    @GetMapping
    @Operation(
        summary = "Listar Personal Access Tokens",
        description = "Retorna os metadados dos tokens do usuário autenticado. Nunca inclui o valor em claro ou o hash."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Requisição autenticada via PAT (não pode gerenciar tokens)")
    })
    public ResponseEntity<List<PersonalAccessTokenResponse>> list() {
        User user = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(tokenService.list(user.getId()));
    }

    /**
     * Revoga um Personal Access Token do usuário autenticado. Idempotente.
     *
     * @param id ID do token a revogar
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Revogar Personal Access Token",
        description = "Revoga o token informado. Idempotente — revogar um token já revogado não é erro. "
            + "Retorna 404 se o token não existir ou pertencer a outro usuário."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Token revogado (ou já estava revogado)"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Requisição autenticada via PAT (não pode gerenciar tokens)"),
        @ApiResponse(responseCode = "404", description = "Token não encontrado para este usuário")
    })
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        User user = getCurrentAuthenticatedUser();
        tokenService.revoke(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
