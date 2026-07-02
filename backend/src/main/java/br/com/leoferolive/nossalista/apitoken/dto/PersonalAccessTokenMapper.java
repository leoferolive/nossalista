package br.com.leoferolive.nossalista.apitoken.dto;

import br.com.leoferolive.nossalista.apitoken.domain.PersonalAccessToken;
import org.springframework.stereotype.Component;

/**
 * Converte a entidade {@link PersonalAccessToken} para os DTOs de resposta.
 */
@Component
public class PersonalAccessTokenMapper {

    public PersonalAccessTokenResponse toResponse(PersonalAccessToken token) {
        return new PersonalAccessTokenResponse(
            token.getId(),
            token.getName(),
            token.getPrefix(),
            token.getScope(),
            token.getExpiresAt(),
            token.getLastUsedAt(),
            token.getCreatedAt()
        );
    }

    public PersonalAccessTokenCreatedResponse toCreatedResponse(PersonalAccessToken token, String rawToken) {
        return new PersonalAccessTokenCreatedResponse(
            token.getId(),
            token.getName(),
            rawToken,
            token.getPrefix(),
            token.getScope(),
            token.getExpiresAt(),
            token.getCreatedAt()
        );
    }
}
