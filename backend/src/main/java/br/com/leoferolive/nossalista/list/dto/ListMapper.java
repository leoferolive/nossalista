package br.com.leoferolive.nossalista.list.dto;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.util.ListTypeMapper;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper para converter entidade List em DTOs.
 *
 * <p>Este mapper é uma transformação pura — não acessa repositórios nem
 * realiza I/O. O caller é responsável por fornecer dados derivados como
 * {@code itemsCount}.
 */
@Component
public class ListMapper {

    /**
     * Converte entidade List em DTO ListResponse.
     * Este método assume que o usuário atual é o dono (isOwner = true).
     *
     * @param list       a entidade list
     * @param itemsCount número de itens na lista
     * @return DTO ListResponse com dados completos
     */
    public ListResponse toListResponse(List list, int itemsCount) {
        return toListResponse(list, list.getOwner().getId(), itemsCount);
    }

    /**
     * Converte entidade List em DTO ListResponse.
     *
     * @param list          a entidade list
     * @param currentUserId ID do usuário atual para calcular isOwner
     * @param itemsCount    número de itens na lista
     * @return DTO ListResponse com dados completos
     */
    public ListResponse toListResponse(List list, UUID currentUserId, int itemsCount) {
        ListResponse.TypeResponse typeResponse = new ListResponse.TypeResponse(
            ListTypeMapper.resolveId(list.getTypeEntity(), list.getTypeId()),
            ListTypeMapper.resolveName(list.getTypeEntity(), list.getTypeId()),
            ListTypeMapper.resolveSlug(list.getTypeEntity(), list.getTypeId())
        );

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
