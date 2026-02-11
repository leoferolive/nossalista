package br.com.leoferolive.nossalista.auth.dto;

import br.com.leoferolive.nossalista.auth.domain.User;
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
}
