package br.com.leoferolive.nossalista.member.service;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.dto.JoinListResponse;
import br.com.leoferolive.nossalista.list.exception.InviteExpiredException;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Serviço para operações de join em listas via convite (endpoints públicos)
 */
@Service
public class ListJoinService {

    private final ListRepository listRepository;
    private final ListItemRepository listItemRepository;

    public ListJoinService(ListRepository listRepository, ListItemRepository listItemRepository) {
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
    }

    /**
     * Busca uma lista pelo código de convite (endpoint público, modo read-only).
     * Não requer autenticação.
     *
     * @param inviteCode código de convite
     * @return JoinListResponse com dados da lista em modo read-only
     * @throws ListNotFoundException  se o código de convite não existe
     * @throws InviteExpiredException se o link de convite expirou
     */
    @Transactional(readOnly = true)
    public JoinListResponse getListByInviteCode(String inviteCode) {
        List list = listRepository.findByInviteCodeWithDetails(inviteCode)
            .orElseThrow(() -> new ListNotFoundException("Convite não encontrado"));

        // Validar expiração
        if (list.getInviteExpiresAt() == null ||
            list.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InviteExpiredException(
                "Este link de convite expirou. Peça um novo link ao dono da lista."
            );
        }

        // Carregar itens ordenados por position
        java.util.List<ListItem> items = listItemRepository.findByListIdOrderByPositionAsc(list.getId());

        // Mapear para DTO
        return mapToJoinListResponse(list, items);
    }

    private JoinListResponse mapToJoinListResponse(List list, java.util.List<ListItem> items) {
        // Defensive: get type info from entity or fallback to getType() method
        String typeSlug;
        String typeName;
        if (list.getTypeEntity() != null) {
            typeSlug = list.getTypeEntity().getSlug();
            typeName = list.getTypeEntity().getName();
        } else {
            // Fallback: use getType() which has embedded fallback logic
            var listType = list.getType();
            typeSlug = listType.getSlug();
            // Capitalize first letter for name
            typeName = listType.name().charAt(0) + listType.name().substring(1).toLowerCase();
            // Fix special cases
            typeName = switch (listType) {
                case SHOPPING -> "Compras";
                case TASK -> "Tarefas";
                case WISHLIST -> "Wishlist";
                case GENERIC -> "Genérica";
            };
        }

        return new JoinListResponse(
            list.getId(),
            list.getName(),
            typeSlug,
            typeName,
            list.getOwner().getUsername(),
            list.getOwner().getName(),
            list.getOwner().getAvatarUrl(),
            items.stream()
                .map(this::mapToJoinListItemResponse)
                .collect(Collectors.toList()),
            list.getInviteCode(),
            list.getInviteExpiresAt(),
            "READ_ONLY"
        );
    }

    private JoinListResponse.JoinListItemResponse mapToJoinListItemResponse(ListItem item) {
        return new JoinListResponse.JoinListItemResponse(
            item.getId(),
            item.getName(),
            item.isChecked(),
            item.getQuantity(),
            item.getDueDate(),
            item.getUrl(),
            item.getPosition()
        );
    }
}
