package br.com.leoferolive.nossalista.auth.dto;

import br.com.leoferolive.nossalista.auth.domain.User;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert User entity to DTOs
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to RegisterResponse DTO
     * Password field is NOT included in the response for security
     *
     * @param user the user entity
     * @return RegisterResponse DTO without password
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
