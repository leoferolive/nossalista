package br.com.leoferolive.nossalista.auth.dto;

import br.com.leoferolive.nossalista.auth.domain.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper para converter entidade User em DTOs
 */
@Component
public class UserMapper {

    /**
     * Converte entidade User em DTO RegisterResponse
     * Campo password NÃO é incluído na resposta por segurança
     *
     * @param user a entidade user
     * @return DTO RegisterResponse sem password
     */
    public RegisterResponse toRegisterResponse(User user) {
        return new RegisterResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getName(),
            user.getAvatarUrl(),
            user.getAuthProvider(),
            user.getCreatedAt()
        );
    }

    /**
     * Converte entidade User em DTO LoginResponse com token JWT
     * Campo password NÃO é incluído na resposta por segurança
     *
     * @param user      a entidade user
     * @param token     JWT token gerado para o usuário
     * @param expiresAt data/hora de expiração do token
     * @return DTO LoginResponse com token e dados do usuário
     */
    public LoginResponse toLoginResponse(User user, String token, LocalDateTime expiresAt) {
        return new LoginResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getName(),
            user.getAvatarUrl(),
            user.getAuthProvider(),
            user.getCreatedAt(),
            token,
            expiresAt
        );
    }
}
