package br.com.leoferolive.nossalista.list.dto;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.domain.ListTypeEntity;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper para converter entidade List em DTOs
 */
@Component
public class ListMapper {

    private final ListItemRepository listItemRepository;

    public ListMapper(ListItemRepository listItemRepository) {
        this.listItemRepository = listItemRepository;
    }

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
            // Fallback seguro se typeEntity não carregado (LAZY)
            Integer typeId = list.getTypeId();
            String typeName = "Unknown";
            String typeSlug = "unknown";

            // Mapeamento seguro dos tipos conhecidos
            switch (typeId) {
                case 1:
                    typeName = "Compras";
                    typeSlug = "compras";
                    break;
                case 2:
                    typeName = "Tarefas";
                    typeSlug = "tarefas";
                    break;
                case 3:
                    typeName = "Wishlist";
                    typeSlug = "wishlist";
                    break;
                case 4:
                    typeName = "Genérica";
                    typeSlug = "generica";
                    break;
            }

            typeResponse = new ListResponse.TypeResponse(typeId, typeName, typeSlug);
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
            owner.getName(),
            owner.getAvatarUrl()
        );

        // Calcular isOwner comparando owner.id com currentUserId
        boolean isOwner = owner.getId().equals(currentUserId);
        int itemsCount = listItemRepository.countByListId(list.getId()).intValue();

        return new ListResponse(
            list.getId(),
            list.getName(),
            typeResponse,
            ownerResponse,
            list.getInviteCode(),
            isOwner,
            itemsCount,
            list.getCreatedAt(),
            list.getUpdatedAt()
        );
    }
}
