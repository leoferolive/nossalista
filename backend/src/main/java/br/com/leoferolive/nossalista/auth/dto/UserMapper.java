package br.com.leoferolive.nossalista.auth.dto;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.dto.UserProfileResponse;
import br.com.leoferolive.nossalista.user.dto.UserSearchResponse;
import org.springframework.stereotype.Component;



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
     * Converte entidade User em DTO de login sem expor o JWT. O token de
     * sessão é emitido exclusivamente no cookie HttpOnly da resposta.
     *
     * @param user a entidade user
     * @return DTO LoginResponse com dados do usuário
     */
    public LoginResponse toLoginResponse(User user) {
        return new LoginResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getName(),
            user.getAvatarUrl(),
            user.getOnboardingCompletedAt(),
            user.getAuthProvider(),
            user.getCreatedAt()
        );
    }

    /**
     * Converte entidade User em DTO UserProfileResponse
     * Campo password NÃO é incluído na resposta por segurança
     *
     * @param user a entidade user
     * @return DTO UserProfileResponse com dados completos do próprio usuário
     */
    public UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getName(),
            user.getAvatarUrl(),
            user.getAuthProvider().toString(),
            user.getOnboardingCompletedAt(),
            user.getCreatedAt()
        );
    }

    /**
     * Converte entidade User em DTO UserSearchResponse
     * Campos password e email NÃO são incluídos na resposta por segurança e privacidade.
     * Este DTO é usado para busca pública de usuários (ex: convites para listas).
     *
     * @param user a entidade user
     * @return DTO UserSearchResponse com apenas dados públicos (username, name, avatarUrl)
     */
    public UserSearchResponse toUserSearchResponse(User user) {
        return new UserSearchResponse(
            user.getUsername(),
            user.getName(),
            user.getAvatarUrl()
        );
    }
}
