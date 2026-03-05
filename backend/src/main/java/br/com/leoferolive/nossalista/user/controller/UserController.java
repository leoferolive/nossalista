package br.com.leoferolive.nossalista.user.controller;

import br.com.leoferolive.nossalista.auth.dto.UserMapper;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.dto.UpdateProfileRequest;
import br.com.leoferolive.nossalista.user.dto.UserProfileResponse;
import br.com.leoferolive.nossalista.user.dto.UserSearchResponse;
import br.com.leoferolive.nossalista.user.service.UserService;
import br.com.leoferolive.nossalista.user.exception.NotAuthenticatedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST para operações de perfil e busca de usuários
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = "Endpoints de perfil e busca de usuários")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * Extrai usuário autenticado do SecurityContext
     *
     * @return User autenticado
     * @throws NotAuthenticatedException se não houver usuário autenticado
     */
    private User getCurrentAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new NotAuthenticatedException("Usuário não autenticado");
        }

        return (User) auth.getPrincipal();
    }

    /**
     * Retorna o perfil do usuário autenticado
     *
     * @return UserProfileResponse com dados completos do usuário (sem password)
     */
    @GetMapping("/me")
    @Operation(
        summary = "Obter perfil do usuário autenticado",
        description = "Retorna informações completas do próprio perfil (sem password). Requer token JWT válido no header Authorization."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado - Token JWT ausente, inválido ou expirado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        // Extrair usuário do SecurityContext (já autenticado via JwtAuthenticationFilter)
        User user = getCurrentAuthenticatedUser();

        UserProfileResponse response = userMapper.toUserProfileResponse(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza perfil do usuário autenticado
     *
     * @param request DTO com name e/ou avatarUrl
     * @return UserProfileResponse com dados atualizados
     */
    @PatchMapping("/me")
    @Operation(
        summary = "Atualizar perfil do usuário autenticado",
        description = "Atualiza nome e/ou avatar. Username e email são campos somente leitura e não podem ser alterados."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Tentativa de alterar campos somente leitura (username ou email)",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado - Token JWT ausente, inválido ou expirado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<UserProfileResponse> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        // Extrair usuário do SecurityContext
        User currentUser = getCurrentAuthenticatedUser();

        // Atualizar apenas name e avatarUrl (username e email são readonly)
        User updatedUser = userService.updateProfile(
            currentUser.getId(),
            request.name(),
            request.avatarUrl()
        );

        UserProfileResponse response = userMapper.toUserProfileResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca usuários por username
     *
     * @param query Termo de busca (case-insensitive, parcial)
     * @return Lista de UserSearchResponse (sem email ou password)
     */
    @GetMapping("/search")
    @Operation(
        summary = "Buscar usuários por username",
        description = "Busca case-insensitive e parcial por username. Retorna apenas informações públicas (username, name, avatar) sem email. Query mínima: 2 caracteres."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso (pode retornar array vazio se não encontrar resultados)",
            content = @Content(schema = @Schema(implementation = UserSearchResponse.class))),
        @ApiResponse(responseCode = "400", description = "Query parameter 'q' ausente, vazio ou menor que 2 caracteres",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado - Token JWT ausente, inválido ou expirado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
        @RequestParam(name = "q") String query
    ) {
        // Validar query
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'q' é obrigatório");
        }
        if (query.length() < 2) {
            throw new IllegalArgumentException("Query deve ter no mínimo 2 caracteres");
        }

        // Buscar usuários
        List<User> users = userService.searchByUsername(query);

        // Converter para response (sem email)
        List<UserSearchResponse> response = users.stream()
            .map(userMapper::toUserSearchResponse)
            .toList();

        return ResponseEntity.ok(response);
    }
}
