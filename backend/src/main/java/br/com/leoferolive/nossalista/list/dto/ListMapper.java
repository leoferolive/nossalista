package br.com.leoferolive.nossalista.list.dto;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.domain.ListTypeEntity;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.stereotype.Component;

/**
 * Mapper para converter entidade List em DTOs
 */
@Component
public class ListMapper {

    /**
     * Converte entidade List em DTO ListResponse
     *
     * @param list a entidade list
     * @return DTO ListResponse com dados completos
     */
    public ListResponse toListResponse(List list) {
        ListTypeEntity typeEntity = list.getTypeEntity();
        ListResponse.TypeResponse typeResponse = null;
        if (typeEntity != null) {
            typeResponse = new ListResponse.TypeResponse(
                typeEntity.getId(),
                typeEntity.getName(),
                typeEntity.getSlug()
            );
        } else {
            // Fallback se typeEntity não carregado (LAZY)
            typeResponse = new ListResponse.TypeResponse(
                list.getTypeId(),
                list.getType().name(),
                list.getType().getSlug()
            );
        }

        User owner = list.getOwner();
        if (owner == null) {
            throw new IllegalStateException(
                "Lista " + list.getId() + " não possui owner associado. " +
                "Verifique se o relacionamento foi carregado corretamente."
            );
        }

        ListResponse.OwnerResponse ownerResponse = new ListResponse.OwnerResponse(
            owner.getId(),
            owner.getUsername(),
            owner.getAvatarUrl()
        );

        return new ListResponse(
            list.getId(),
            list.getName(),
            typeResponse,
            ownerResponse,
            list.getInviteCode(),
            list.getCreatedAt(),
            list.getUpdatedAt()
        );
    }
}
