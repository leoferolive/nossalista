package br.com.leoferolive.nossalista.listitem.dto;

import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para converter entidade ListItem em DTOs
 */
@Component
public class ListItemMapper {

    /**
     * Converte entidade ListItem em DTO ListItemResponseDTO
     *
     * @param item a entidade ListItem
     * @return DTO ListItemResponseDTO com dados completos
     */
    public ListItemResponseDTO toListItemResponseDTO(ListItem item) {
        if (item == null) {
            return null;
        }

        User creator = item.getCreatedBy();
        ListItemResponseDTO.CreatorResponse creatorResponse = null;

        if (creator != null) {
            creatorResponse = new ListItemResponseDTO.CreatorResponse(
                creator.getId(),
                creator.getUsername(),
                creator.getName(),
                creator.getAvatarUrl()
            );
        }

        return new ListItemResponseDTO(
            item.getId(),
            item.getName(),
            item.isChecked(),
            item.getQuantity(),
            item.getDueDate(),
            item.getUrl(),
            item.getPosition(),
            creatorResponse,
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }

    /**
     * Converte uma lista de entidades ListItem em lista de DTOs
     *
     * @param items lista de entidades
     * @return lista de DTOs
     */
    public List<ListItemResponseDTO> toListItemResponseDTOList(List<ListItem> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
            .map(this::toListItemResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Converte CreateItemRequestDTO para entidade ListItem
     * Campos nulos usam valores padrão
     *
     * @param dto o DTO de criação
     * @return nova entidade ListItem (não persistida)
     */
    public ListItem toEntity(CreateItemRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        ListItem item = new ListItem();
        item.setName(dto.name());
        item.setQuantity(dto.quantity());
        item.setDueDate(dto.dueDate());
        item.setUrl(dto.url());

        // Position usa 0 como padrão se não especificado
        item.setPosition(dto.position() != null ? dto.position() : 0);

        return item;
    }

}
