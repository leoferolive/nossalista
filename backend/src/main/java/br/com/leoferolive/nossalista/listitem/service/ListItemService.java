package br.com.leoferolive.nossalista.listitem.service;

import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.common.exception.ValidationException;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.domain.ListType;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.listitem.dto.CreateItemRequestDTO;
import br.com.leoferolive.nossalista.listitem.dto.ListItemMapper;
import br.com.leoferolive.nossalista.listitem.dto.ListItemResponseDTO;
import br.com.leoferolive.nossalista.listitem.dto.UpdateItemRequest;
import br.com.leoferolive.nossalista.listitem.exception.ItemNotFoundException;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para lógica de negócio de itens de lista
 */
@Service
public class ListItemService {

    private static final Logger log = LoggerFactory.getLogger(ListItemService.class);

    private final ListItemRepository listItemRepository;
    private final ListRepository listRepository;
    private final ListItemMapper listItemMapper;

    public ListItemService(ListItemRepository listItemRepository,
                           ListRepository listRepository,
                           ListItemMapper listItemMapper) {
        this.listItemRepository = listItemRepository;
        this.listRepository = listRepository;
        this.listItemMapper = listItemMapper;
    }

    /**
     * Adiciona um novo item à lista
     * Valida permissões (usuário deve ser participante da lista)
     * Calcula position automaticamente
     *
     * @param listId  ID da lista onde o item será adicionado
     * @param dto     DTO com dados do item
     * @param creator Usuário que está criando o item
     * @return DTO com dados completos do item criado
     * @throws ListNotFoundException se a lista não existir
     * @throws ForbiddenException    se o usuário não for participante da lista
     */
    @Transactional
    public ListItemResponseDTO addItem(UUID listId, CreateItemRequestDTO dto, User creator) {
        // 1. Verificar se lista existe
        // Usa findById em vez de findByIdWithDetails para evitar problemas com JOIN FETCH
        List list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // 2. Verificar se usuário é participante (dono ou membro)
        if (!isParticipant(list, creator)) {
            throw new ForbiddenException("Você não tem permissão para adicionar itens nesta lista");
        }

        // 3. Validar campos dinâmicos por tipo de lista
        validateDynamicFieldsByType(list.getType(), dto);

        // 4. Calcular position automaticamente (max + 1)
        // Usa findMaxPositionByListId que retorna -1 se vazio (próximo será 0)
        Integer maxPosition = listItemRepository.findMaxPositionByListId(listId);
        int nextPosition = maxPosition + 1;

        // 5. Criar entidade ListItem
        ListItem item = new ListItem();
        item.setName(dto.name().trim());
        item.setList(list);
        item.setCreatedBy(creator);
        item.setPosition(nextPosition);
        item.setChecked(false);

        // 6. Campos dinâmicos por tipo de lista
        if (dto.quantity() != null) {
            item.setQuantity(dto.quantity());
        }
        if (dto.dueDate() != null) {
            item.setDueDate(dto.dueDate());
        }
        if (dto.url() != null) {
            item.setUrl(dto.url().trim());
        }

        // 7. Salvar item
        ListItem saved = listItemRepository.save(item);

        // 8. Log de auditoria
        log.info("Item added: itemId={}, itemName='{}', listId={}, createdBy={}, position={}",
                saved.getId(), saved.getName(), listId, creator.getId(), nextPosition);

        // 9. TODO: WebSocket broadcast (Story 5.2)
        // broadcastItemAdded(saved, creator);

        // 10. Retornar DTO
        return listItemMapper.toListItemResponseDTO(saved);
    }

    /**
     * Valida se os campos dinâmicos estão sendo usados no tipo de lista correto.
     *
     * Regras por tipo:
     * - SHOPPING: Permite quantity, rejeita dueDate e url
     * - TASK: Permite dueDate, rejeita quantity e url
     * - WISHLIST: Permite url, rejeita quantity e dueDate
     * - GENERIC: Rejeita todos os campos dinâmicos
     *
     * @param listType Tipo da lista
     * @param dto      DTO com os campos a validar
     * @throws ValidationException se campos inválidos para o tipo
     */
    private void validateDynamicFieldsByType(ListType listType, CreateItemRequestDTO dto) {
        switch (listType) {
            case SHOPPING:
                if (dto.dueDate() != null) {
                    throw new ValidationException("Campo 'dueDate' não é permitido em listas do tipo Compras");
                }
                if (dto.url() != null) {
                    throw new ValidationException("Campo 'url' não é permitido em listas do tipo Compras");
                }
                break;

            case TASK:
                if (dto.quantity() != null) {
                    throw new ValidationException("Campo 'quantity' não é permitido em listas do tipo Tarefas");
                }
                if (dto.url() != null) {
                    throw new ValidationException("Campo 'url' não é permitido em listas do tipo Tarefas");
                }
                break;

            case WISHLIST:
                if (dto.quantity() != null) {
                    throw new ValidationException("Campo 'quantity' não é permitido em listas do tipo Wishlist");
                }
                if (dto.dueDate() != null) {
                    throw new ValidationException("Campo 'dueDate' não é permitido em listas do tipo Wishlist");
                }
                break;

            case GENERIC:
                if (dto.quantity() != null) {
                    throw new ValidationException("Campo 'quantity' não é permitido em listas do tipo Genérica");
                }
                if (dto.dueDate() != null) {
                    throw new ValidationException("Campo 'dueDate' não é permitido em listas do tipo Genérica");
                }
                if (dto.url() != null) {
                    throw new ValidationException("Campo 'url' não é permitido em listas do tipo Genérica");
                }
                break;
        }
    }

    /**
     * Verifica se o usuário é participante da lista (dono ou membro)
     * Por enquanto, apenas verifica se é o dono (Story 4.1 adicionará membros)
     *
     * @param list Lista a verificar
     * @param user Usuário a verificar
     * @return true se o usuário for participante
     */
    private boolean isParticipant(List list, User user) {
        // Verificar se é o dono (tratando lazy loading)
        User owner = list.getOwner();
        if (owner != null && owner.getId().equals(user.getId())) {
            return true;
        }

        // TODO: Story 4.1 - Verificar se é membro da lista
        // Por enquanto, apenas donos são considerados participantes

        return false;
    }

    /**
     * Busca todos os itens de uma lista
     * Valida permissões (usuário deve ser participante)
     *
     * @param listId ID da lista
     * @param user   Usuário solicitante
     * @return Lista de DTOs com todos os itens ordenados por position
     * @throws ListNotFoundException se a lista não existir
     * @throws ForbiddenException    se o usuário não for participante
     */
    @Transactional(readOnly = true)
    public java.util.List<ListItemResponseDTO> getItemsByListId(UUID listId, User user) {
        // 1. Verificar se lista existe
        List list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // 2. Verificar se usuário é participante
        if (!isParticipant(list, user)) {
            throw new ForbiddenException("Você não tem permissão para ver os itens desta lista");
        }

        // 3. Buscar itens ordenados por position ASC
        java.util.List<ListItem> items = listItemRepository.findByListIdOrderByPositionAsc(listId);

        // 4. Mapear para DTOs
        return items.stream()
                .map(listItemMapper::toListItemResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Toggle do estado checked de um item
     * Valida permissões (usuário deve ser participante)
     *
     * @param listId ID da lista
     * @param itemId ID do item
     * @param user   Usuário solicitante
     * @return DTO com item atualizado
     * @throws ListNotFoundException se a lista não existir
     * @throws ItemNotFoundException se o item não existir
     * @throws ForbiddenException    se o usuário não for participante
     */
    @Transactional
    public ListItemResponseDTO toggleItemCheck(UUID listId, UUID itemId, User user) {
        // 1. Verificar se lista existe
        List list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // 2. Verificar se usuário é participante
        if (!isParticipant(list, user)) {
            throw new ForbiddenException("Você não tem permissão para modificar itens desta lista");
        }

        // 3. Buscar item
        ListItem item = listItemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item não encontrado"));

        // 4. Verificar se item pertence à lista
        if (!item.getList().getId().equals(listId)) {
            throw new ItemNotFoundException("Item não encontrado nesta lista");
        }

        // 5. Toggle checked
        item.setChecked(!item.isChecked());

        // 6. Salvar (updated_at atualizado automaticamente via @PreUpdate)
        ListItem saved = listItemRepository.save(item);

        // 7. Log
        log.info("Item toggled: itemId={}, checked={}, listId={}, user={}",
                itemId, saved.isChecked(), listId, user.getId());

        // 8. Retornar DTO
        return listItemMapper.toListItemResponseDTO(saved);
    }

    /**
     * Atualiza um item existente
     * Valida permissões e campos por tipo de lista
     *
     * @param listId  ID da lista
     * @param itemId  ID do item
     * @param request DTO com campos para atualizar (opcionais)
     * @param user    Usuário solicitante
     * @return DTO com item atualizado
     * @throws ListNotFoundException  se a lista não existir
     * @throws ItemNotFoundException  se o item não existir
     * @throws ForbiddenException     se o usuário não for participante
     * @throws ValidationException     se campos inválidos para o tipo
     */
    @Transactional
    public ListItemResponseDTO updateItem(
            UUID listId,
            UUID itemId,
            UpdateItemRequest request,
            User user) {

        // 1. Verificar se lista existe
        List list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // 2. Verificar se usuário é participante
        if (!isParticipant(list, user)) {
            throw new ForbiddenException("Você não tem permissão para modificar itens desta lista");
        }

        // 3. Buscar item
        ListItem item = listItemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item não encontrado"));

        // 4. Verificar se item pertence à lista
        if (!item.getList().getId().equals(listId)) {
            throw new ItemNotFoundException("Item não encontrado nesta lista");
        }

        // 5. Validar campos por tipo de lista
        validateUpdateFields(list.getType(), request);

        // 6. Atualizar campos fornecidos
        if (request.getName() != null && !request.getName().isBlank()) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new ValidationException("Nome não pode estar vazio");
            }
            item.setName(trimmedName);
        }

        // Atualizar campos específicos por tipo
        switch (list.getType()) {
            case SHOPPING:
                if (request.getQuantity() != null) {
                    item.setQuantity(request.getQuantity());
                }
                break;

            case TASK:
                if (request.getDueDate() != null) {
                    item.setDueDate(request.getDueDate());
                }
                break;

            case WISHLIST:
                if (request.getUrl() != null && !request.getUrl().isBlank()) {
                    item.setUrl(request.getUrl().trim());
                }
                break;

            case GENERIC:
                // Genérica: apenas name, outros campos ignorados
                break;
        }

        // 7. Salvar (updated_at atualizado automaticamente via @PreUpdate)
        ListItem saved = listItemRepository.save(item);

        // 8. Log com campos alterados
        log.info("Item updated: itemId={}, listId={}, user={}, fieldsChanged={}",
                itemId, listId, user.getId(), 
                request.getName() != null ? "name" : "",
                request.getQuantity() != null ? "quantity" : "",
                request.getDueDate() != null ? "dueDate" : "",
                request.getUrl() != null ? "url" : "");

        // 9. Retornar DTO
        return listItemMapper.toListItemResponseDTO(saved);
    }

    /**
     * Valida campos da request conforme tipo de lista
     *
     * Regras por tipo:
     * - SHOPPING: Permite name, quantity; rejeita dueDate e url
     * - TASK: Permite name, dueDate; rejeita quantity e url
     * - WISHLIST: Permite name, url; rejeita quantity e dueDate
     * - GENERIC: Permite apenas name; rejeita todos os outros
     *
     * @param listType Tipo da lista
     * @param request  DTO com os campos a validar
     * @throws ValidationException se campos inválidos para o tipo
     */
    private void validateUpdateFields(ListType listType, UpdateItemRequest request) {
        // Validações de tipo - campos não permitidos devem ser null
        switch (listType) {
            case SHOPPING:
                if (request.getDueDate() != null) {
                    throw new ValidationException("Campo dueDate não é permitido para listas de Compras");
                }
                if (request.getUrl() != null) {
                    throw new ValidationException("Campo url não é permitido para listas de Compras");
                }
                // quantity pode ser null (mantém valor atual) ou positivo
                if (request.getQuantity() != null && request.getQuantity() < 1) {
                    throw new ValidationException("Quantity deve ser positivo para listas de Compras");
                }
                break;

            case TASK:
                if (request.getQuantity() != null) {
                    throw new ValidationException("Campo quantity não é permitido para listas de Tarefas");
                }
                if (request.getUrl() != null) {
                    throw new ValidationException("Campo url não é permitido para listas de Tarefas");
                }
                break;

            case WISHLIST:
                if (request.getQuantity() != null) {
                    throw new ValidationException("Campo quantity não é permitido para listas de Wishlist");
                }
                if (request.getDueDate() != null) {
                    throw new ValidationException("Campo dueDate não é permitido para listas de Wishlist");
                }
                break;

            case GENERIC:
                // Lança erro APENAS se ALGUM campo foi fornecido
                boolean anyFieldProvided = request.getQuantity() != null
                        || request.getDueDate() != null
                        || request.getUrl() != null;

                if (anyFieldProvided) {
                    throw new ValidationException("Listas Genéricas aceitam apenas o campo name");
                }
                break;
        }
    }
}
