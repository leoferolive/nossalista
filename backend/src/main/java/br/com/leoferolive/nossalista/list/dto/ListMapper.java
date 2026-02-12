package br.com.leoferolive.nossalista.list.dto;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.domain.ListTypeEntity;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper para converter entidade List em DTOs
 */
@Component
public class ListMapper {

    /**
     * Converte entidade List em DTO ListResponse
     * Este método assume que o usuário atual é o dono (isOwner = true)
     *
     * @param list a entidade list
     * @return DTO ListResponse com dados completos
     */
    public ListResponse toListResponse(List list) {
        // Para compatibilidade com código existente, assume isOwner = true
        return toListResponse(list, list.getOwner().getId());
    }

    /**
     * Converte entidade List em DTO ListResponse
     *
     * @param list a entidade list
     * @param currentUserId ID do usuário atual para calcular isOwner
     * @return DTO ListResponse com dados completos
     */
    public ListResponse toListResponse(List list, UUID currentUserId) {
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

        // Calcular isOwner comparando owner.id com currentUserId
        boolean isOwner = owner.getId().equals(currentUserId);

        return new ListResponse(
            list.getId(),
            list.getName(),
            typeResponse,
            ownerResponse,
            list.getInviteCode(),
            isOwner,
            0, // itemsCount - placeholder, será implementado em story futura
            list.getCreatedAt(),
            list.getUpdatedAt()
        );
    }
}
