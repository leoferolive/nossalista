package br.com.leoferolive.nossalista.listitem.service;

import br.com.leoferolive.nossalista.activity.service.ActivityLogService;
import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.common.exception.ValidationException;
import br.com.leoferolive.nossalista.list.domain.SharedList;
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
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.notification.ListItemNotificationEvent;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.websocket.WebSocketEventPublisher;
import br.com.leoferolive.nossalista.websocket.dto.ListLayoutUpdatedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service para lógica de negócio de itens de lista
 */
@Service
public class ListItemService {

    private static final Logger log = LoggerFactory.getLogger(ListItemService.class);

    /**
     * Teto de tentativas para o retry manual de {@code position} (ver
     * {@link #addItem}). Cada colisão de constraint gasta uma tentativa;
     * acima disso o erro é propagado de forma explícita em vez de tentar
     * indefinidamente.
     */
    private static final int MAX_POSITION_RETRIES = 8;

    /**
     * Base (ms) do backoff exponencial com jitter entre tentativas — ver
     * {@link #backoffBeforeRetry}.
     */
    private static final long POSITION_RETRY_BACKOFF_BASE_MILLIS = 8;

    /**
     * Teto (ms) do backoff entre tentativas, para não deixar a última
     * tentativa esperar tempo demais.
     */
    private static final long POSITION_RETRY_BACKOFF_CAP_MILLIS = 300;

    /**
     * Nome da constraint criada pela migration V18. Usado para diferenciar
     * uma colisão de position (retentável) de qualquer outra violação de
     * integridade (não retentável).
     */
    private static final String POSITION_CONSTRAINT_NAME = "uq_list_items_list_position";

    private final ListItemRepository listItemRepository;
    private final ListRepository listRepository;
    private final ListItemMapper listItemMapper;
    private final WebSocketEventPublisher eventPublisher;
    private final ListMemberRepository listMemberRepository;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PlatformTransactionManager transactionManager;

    public ListItemService(ListItemRepository listItemRepository,
                           ListRepository listRepository,
                           ListItemMapper listItemMapper,
                           WebSocketEventPublisher eventPublisher,
                           ListMemberRepository listMemberRepository,
                           ActivityLogService activityLogService,
                           ApplicationEventPublisher applicationEventPublisher,
                           PlatformTransactionManager transactionManager) {
        this.listItemRepository = listItemRepository;
        this.listRepository = listRepository;
        this.listItemMapper = listItemMapper;
        this.eventPublisher = eventPublisher;
        this.listMemberRepository = listMemberRepository;
        this.activityLogService = activityLogService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.transactionManager = transactionManager;
    }

    /**
     * Adiciona um novo item à lista.
     * Valida permissões (usuário deve ser participante da lista), calcula
     * {@code position} automaticamente e insere o item, com retry manual
     * quando a constraint {@code uq_list_items_list_position} (migration V18)
     * detecta uma colisão concorrente: dois {@code addItem} simultâneos na
     * mesma lista podem ler o mesmo {@code maxPosition} e tentar gravar a
     * mesma position.
     *
     * <p>Deliberadamente <b>não</b> é {@code @Transactional}: cada tentativa
     * roda inteira (leitura + insert + activity log + broadcast) em sua
     * própria transação de topo, via {@link TransactionTemplate}, e não
     * dentro de uma transação nova aninhada (ex.: {@code REQUIRES_NEW}) sobre
     * uma transação externa já aberta. Isso importa por dois motivos:
     *
     * <ol>
     *   <li>Depois que um {@code flush} falha por violação de constraint, a
     *   especificação JPA marca a transação para rollback e a
     *   EntityManager/Session correntes ficam em estado não confiável para
     *   novas operações — não é seguro tentar de novo na mesma transação.
     *   Cada tentativa aqui usa uma transação (e persistence context) nova.</li>
     *   <li>Uma transação aninhada ({@code REQUIRES_NEW}) sobre uma transação
     *   externa já aberta mantém DUAS conexões do pool ocupadas
     *   simultaneamente por chamada (a externa suspensa + a nova); sob carga
     *   concorrente isso esgota o pool de conexões (padrão 10, Hikari) bem
     *   antes do limite real de concorrência da constraint. Como cada
     *   tentativa aqui é uma transação de topo sequencial (nunca aninhada),
     *   o custo por chamada continua sendo de no máximo uma conexão por vez —
     *   igual a qualquer outra requisição.</li>
     * </ol>
     *
     * <p>O efeito colateral é que, numa colisão, a tentativa inteira é
     * refeita (inclusive a checagem de permissão e o log de auditoria), não
     * apenas o cálculo de position — um custo aceitável porque colisões são
     * raras (só acontecem sob escrita concorrente real na mesma lista) e o
     * teto de tentativas é baixo.
     *
     * @param listId  ID da lista onde o item será adicionado
     * @param dto     DTO com dados do item
     * @param creator Usuário que está criando o item
     * @return DTO com dados completos do item criado
     * @throws ListNotFoundException se a lista não existir
     * @throws ForbiddenException    se o usuário não for participante da lista
     * @throws IllegalStateException se o teto de tentativas de retry for excedido
     */
    public ListItemResponseDTO addItem(UUID listId, CreateItemRequestDTO dto, User creator) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        DataIntegrityViolationException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_POSITION_RETRIES; attempt++) {
            try {
                return txTemplate.execute(status -> addItemInOwnTransaction(listId, dto, creator));
            } catch (DataIntegrityViolationException e) {
                // Só absorve e tenta de novo se a violação for especificamente da
                // constraint de position (V18) — qualquer outra DataIntegrityViolationException
                // (ex.: bug não relacionado gerando outra violação de constraint) deve
                // propagar imediatamente, sem ser mascarada por retries e por uma
                // mensagem de erro que sugeriria (erradamente) colisão de position.
                if (!isPositionConstraintViolation(e)) {
                    throw e;
                }
                lastFailure = e;
                log.warn("Colisão de position ao adicionar item (tentativa {}/{}, listId={}): {}",
                        attempt, MAX_POSITION_RETRIES, listId, e.getMessage());
                if (attempt < MAX_POSITION_RETRIES) {
                    backoffBeforeRetry(attempt);
                }
            }
        }

        throw new IllegalStateException(
                "Não foi possível inserir o item na lista " + listId
                        + " após " + MAX_POSITION_RETRIES
                        + " tentativas (colisão concorrente de position)",
                lastFailure);
    }

    /**
     * Verifica se a {@link DataIntegrityViolationException} corresponde à
     * constraint de position (V18), percorrendo a cadeia de causas — o driver
     * JDBC costuma expor o nome da constraint só na exceção raiz, não na
     * exceção Spring de mais alto nível.
     */
    private boolean isPositionConstraintViolation(DataIntegrityViolationException e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains(POSITION_CONSTRAINT_NAME)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Pausa curta e aleatória (jitter) antes da próxima tentativa de
     * {@link #addItem}.
     *
     * <p>Sem isso, várias threads que colidem no mesmo instante tendem a
     * recalcular {@code maxPosition} e reinserir de novo praticamente ao
     * mesmo tempo — ficando "em lockstep" e colidindo repetidamente ENTRE SI
     * a cada nova tentativa (efeito manada), em vez de se espalharem no
     * tempo. Usa "full jitter" (AWS Architecture Blog): espera um valor
     * aleatório entre 0 e um teto que cresce exponencialmente com a
     * tentativa, o que se mostrou mais eficaz que jitter proporcional para
     * desfazer o lockstep entre múltiplos concorrentes.
     */
    private void backoffBeforeRetry(int attempt) {
        try {
            long exponentialCap = POSITION_RETRY_BACKOFF_BASE_MILLIS * (1L << attempt);
            long cappedMillis = Math.min(POSITION_RETRY_BACKOFF_CAP_MILLIS, exponentialCap);
            long backoffMillis = ThreadLocalRandom.current().nextLong(0, cappedMillis + 1);
            Thread.sleep(backoffMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Corpo de uma única tentativa de {@link #addItem}, executado dentro de
     * uma transação própria pelo {@link TransactionTemplate} do chamador.
     */
    private ListItemResponseDTO addItemInOwnTransaction(UUID listId, CreateItemRequestDTO dto, User creator) {
        // 1. Verificar se lista existe
        // Usa findById em vez de findByIdWithDetails para evitar problemas com JOIN FETCH
        SharedList list = listRepository.findById(listId)
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

        // 7. Salvar item — saveAndFlush força o INSERT já aqui (em vez de
        // adiar para o commit no fim de txTemplate.execute()), permitindo que
        // uma violação da constraint uq_list_items_list_position (V18) seja
        // capturada de forma previsível pelo laço de retry em addItem().
        ListItem saved = listItemRepository.saveAndFlush(item);

        // 8. Log de auditoria
        log.info("Item added: itemId={}, itemName='{}', listId={}, createdBy={}, position={}",
                saved.getId(), saved.getName(), listId, creator.getId(), nextPosition);
        activityLogService.logItemAdded(list, creator, saved);

        // 9. Broadcast WebSocket
        ListItemResponseDTO result = listItemMapper.toListItemResponseDTO(saved);
        Long revision = touchListRevision(list);
        broadcastItemEvent("ITEM_ADDED", result, creator, listId, revision);
        publishNotificationEvent(listId, creator.getId(), "ITEM_ADDED", result, creator);

        // 10. Retornar DTO
        return result;
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
     * Verifica se o usuário é participante da lista (dono ou membro ativo).
     * Realiza o check de dono em memória antes de consultar o banco,
     * evitando queries desnecessárias quando o dono acessa sua própria lista.
     *
     * @param list Lista a verificar
     * @param user Usuário a verificar
     * @return true se o usuário for dono ou membro da lista
     */
    private boolean isParticipant(SharedList list, User user) {
        // Verificar se é o dono (tratando lazy loading)
        User owner = list.getOwner();
        if (owner != null && owner.getId().equals(user.getId())) {
            return true;
        }

        return listMemberRepository.existsByListIdAndUserId(list.getId(), user.getId());
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
        SharedList list = listRepository.findById(listId)
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
        SharedList list = listRepository.findById(listId)
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
        if (saved.isChecked()) {
            activityLogService.logItemChecked(list, user, saved);
        } else {
            activityLogService.logItemUnchecked(list, user, saved);
        }

        // 8. Broadcast WebSocket e retornar DTO
        ListItemResponseDTO result = listItemMapper.toListItemResponseDTO(saved);
        Long revision = touchListRevision(list);
        broadcastItemEvent("ITEM_CHECKED", result, user, listId, revision);
        publishNotificationEvent(listId, user.getId(), "ITEM_CHECKED", result, user);
        return result;
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
        SharedList list = listRepository.findById(listId)
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
        java.util.List<String> changed = new java.util.ArrayList<>();
        if (request.getName() != null) {
            changed.add("name");
        }
        if (request.getQuantity() != null) {
            changed.add("quantity");
        }
        if (request.getDueDate() != null) {
            changed.add("dueDate");
        }
        if (request.getUrl() != null) {
            changed.add("url");
        }
        String fieldsChanged = String.join(", ", changed);
        log.info("Item updated: itemId={}, listId={}, user={}, fieldsChanged={}",
                itemId, listId, user.getId(), fieldsChanged);
        activityLogService.logItemUpdated(list, user, saved, changed);

        // 9. Broadcast WebSocket e retornar DTO
        ListItemResponseDTO result = listItemMapper.toListItemResponseDTO(saved);
        Long revision = touchListRevision(list);
        broadcastItemEvent("ITEM_UPDATED", result, user, listId, revision);
        publishNotificationEvent(listId, user.getId(), "ITEM_UPDATED", result, user);
        return result;
    }

    /**
     * Remove um item da lista
     * Valida permissões, deleta item e reordena positions
     *
     * @param listId ID da lista
     * @param itemId ID do item
     * @param user   Usuário solicitante
     * @throws ListNotFoundException se a lista não existir
     * @throws ItemNotFoundException se o item não existir
     * @throws ForbiddenException    se o usuário não for participante
     */
    @Transactional
    public void deleteItem(UUID listId, UUID itemId, User user) {
        // 1. Verificar se lista existe
        SharedList list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // 2. Verificar se usuário é participante
        if (!isParticipant(list, user)) {
            throw new ForbiddenException("Você não tem permissão para remover itens desta lista");
        }

        // 3. Buscar item
        ListItem item = listItemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item não encontrado"));

        // 4. Verificar se item pertence à lista
        if (!item.getList().getId().equals(listId)) {
            throw new ItemNotFoundException("Item não encontrado nesta lista");
        }

        // 5. Registrar activity log ANTES de deletar (para auditoria)
        activityLogService.logItemRemoved(list, user, item);

        // 6. Capturar DTO ANTES de deletar (para broadcast ITEM_REMOVED)
        ListItemResponseDTO itemDTO = listItemMapper.toListItemResponseDTO(item);

        // 7. Deletar item
        Integer deletedPosition = item.getPosition();
        listItemRepository.delete(item);

        // 8. Reordenar positions dos itens restantes
        java.util.List<ListItem> reorderedItems = reorderPositions(listId, deletedPosition);
        Long revision = touchListRevision(list);

        // 9. Log
        log.info("Item deleted: itemId={}, listId={}, user={}", itemId, listId, user.getId());

        // 10. Broadcast WebSocket
        broadcastItemEvent("ITEM_REMOVED", itemDTO, user, listId, revision);
        broadcastLayoutUpdatedEvent(listId, reorderedItems, user, revision);
        publishNotificationEvent(listId, user.getId(), "ITEM_REMOVED", itemDTO, user);
    }

    /**
     * Publica mensagem broadcast no tópico WebSocket da lista
     */
    private void broadcastItemEvent(String type, ListItemResponseDTO dto, User actor, UUID listId, Long revision) {
        eventPublisher.publishEvent(listId, type, dto, actor, revision);
    }

    /**
     * Publica um {@link ListItemNotificationEvent} para notificar membros da lista
     * (push + WebSocket por usuário). O evento só é consumido APÓS o commit da
     * transação atual — ver {@code notification.ListItemNotificationEventListener}.
     * Assim, nenhuma notificação é disparada para uma mudança que sofre rollback,
     * e o I/O de notificação roda fora da thread da requisição.
     */
    private void publishNotificationEvent(UUID listId, UUID actorId, String type, Object payload, User actor) {
        applicationEventPublisher.publishEvent(new ListItemNotificationEvent(listId, actorId, type, payload, actor));
    }

    private void broadcastLayoutUpdatedEvent(UUID listId, java.util.List<ListItem> reorderedItems, User actor, Long revision) {
        java.util.List<ListLayoutUpdatedPayload.ItemPositionPayload> positions = reorderedItems.stream()
            .map(item -> new ListLayoutUpdatedPayload.ItemPositionPayload(item.getId().toString(), item.getPosition()))
            .collect(Collectors.toList());

        eventPublisher.publishEvent(listId, "LIST_LAYOUT_UPDATED", new ListLayoutUpdatedPayload(positions), actor, revision);
    }

    private Long touchListRevision(SharedList list) {
        SharedList savedList = listRepository.save(list);
        return savedList.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * Reordena positions após deletar item
     * Items com position > deletedPosition têm position decrementada em 1
     */
    private java.util.List<ListItem> reorderPositions(UUID listId, Integer deletedPosition) {
        // Buscar todos os itens com position > deletedPosition
        java.util.List<ListItem> itemsToReorder = listItemRepository
                .findByListIdAndPositionGreaterThanOrderByPositionAsc(listId, deletedPosition);

        // Decrementar position de cada item
        for (ListItem item : itemsToReorder) {
            item.setPosition(item.getPosition() - 1);
        }

        // Salvar todos (batch update)
        return listItemRepository.saveAll(itemsToReorder);
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
