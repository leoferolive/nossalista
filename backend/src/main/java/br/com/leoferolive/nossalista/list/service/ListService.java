package br.com.leoferolive.nossalista.list.service;

import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.dto.UpdateListNameRequest;
import br.com.leoferolive.nossalista.list.exception.InvalidListTypeException;
import br.com.leoferolive.nossalista.list.exception.InviteCodeGenerationException;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.list.repository.ListTypeRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Service para lógica de negócio de listas
 */
@Service
public class ListService {

    private static final Logger log = LoggerFactory.getLogger(ListService.class);
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 12;
    private final SecureRandom random = new SecureRandom();

    private final ListRepository listRepository;
    private final ListTypeRepository listTypeRepository;
    private final ListMemberRepository listMemberRepository;

    public ListService(ListRepository listRepository,
                       ListTypeRepository listTypeRepository,
                       ListMemberRepository listMemberRepository) {
        this.listRepository = listRepository;
        this.listTypeRepository = listTypeRepository;
        this.listMemberRepository = listMemberRepository;
    }

    /**
     * Cria uma nova lista para o usuário
     *
     * @param request Requisição com nome e tipo da lista
     * @param owner   Usuário dono da lista
     * @return A lista criada com ID, inviteCode e timestamps gerados
     * @throws InvalidListTypeException se o typeId não existir no database
     */
    @Transactional
    public List createList(CreateListRequest request, User owner) {
        // Validar se o typeId existe
        if (!listTypeRepository.existsById(request.typeId())) {
            throw new InvalidListTypeException(request.typeId());
        }

        // Criar entidade e setar UUID manualmente (CRÍTICO para PostgreSQL)
        List list = new List();
        list.setId(java.util.UUID.randomUUID());
        list.setName(request.name());
        list.setTypeId(request.typeId());
        list.setOwner(owner);
        list.setInviteCode(generateInviteCode());

        // @PrePersist vai preencher createdAt e updatedAt

        List savedList = listRepository.save(list);

        // Criar membro OWNER automaticamente (Story 4.1)
        ListMember ownerMember = new ListMember();
        ownerMember.setList(savedList);
        ownerMember.setUser(owner);
        ownerMember.setRole(MemberRole.OWNER);
        listMemberRepository.save(ownerMember);

        return savedList;
    }

    /**
     * Gera um código de convite aleatório de 12 caracteres alfanuméricos uppercase
     * Garante unicidade verificando se código já existe no banco
     *
     * @return String com 12 caracteres (A-Z0-9) único
     */
    private String generateInviteCode() {
        String code;
        int maxAttempts = 10;
        int attempt = 0;

        do {
            StringBuilder codeBuilder = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                int index = random.nextInt(INVITE_CODE_CHARS.length());
                codeBuilder.append(INVITE_CODE_CHARS.charAt(index));
            }
            code = codeBuilder.toString();
            attempt++;

            if (attempt >= maxAttempts) {
                throw new InviteCodeGenerationException(maxAttempts);
            }
        } while (listRepository.existsByInviteCode(code));

        return code;
    }

    /**
     * Busca todas as listas de um usuário
     *
     * @param owner Usuário dono das listas
     * @return Lista de listas pertencentes ao usuário
     */
    public java.util.List<List> getAllListsByOwner(User owner) {
        return listRepository.findByOwnerId(owner.getId());
    }

    /**
     * Busca todas as listas onde o usuário é owner OU member
     * Retorna listas ordenadas por updatedAt DESC
     *
     * @param userId ID do usuário
     * @return Lista de listas pertencentes ao usuário ou onde ele é membro
     */
    public java.util.List<List> getAllListsForUser(UUID userId) {
        return listRepository.findAllByOwnerOrMemberOrderByUpdatedAtDesc(userId);
    }

    /**
     * Busca uma lista específica por ID, validando permissões do usuário
     * Usuário deve ser owner OU member da lista para acessar
     *
     * @param listId        ID da lista
     * @param currentUserId ID do usuário autenticado
     * @return A lista se encontrada e usuário tem permissão
     * @throws ListNotFoundException se a lista não existir
     * @throws ForbiddenException    se o usuário não for owner nem member
     */
    public List getListById(UUID listId, UUID currentUserId) {
        List list = listRepository.findByIdWithDetails(listId)
                .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // Verificar se usuário é owner
        boolean isOwner = list.getOwner().getId().equals(currentUserId);

        // TODO Story 4.x: Validar se usuário é MEMBER além de OWNER
        if (!isOwner) {
            throw new ForbiddenException("Você não tem permissão para acessar esta lista");
        }

        return list;
    }

    /**
     * Atualiza o nome de uma lista existente
     * Apenas o dono da lista pode editar o nome
     *
     * @param listId        ID da lista a ser atualizada
     * @param currentUserId ID do usuário autenticado
     * @param request       DTO com o novo nome
     * @return A lista atualizada com novo nome e updated_at
     * @throws ListNotFoundException se a lista não existir
     * @throws ForbiddenException    se o usuário não for o dono da lista
     */
    @Transactional
    public List updateListName(UUID listId, UUID currentUserId, UpdateListNameRequest request) {
        // Buscar a lista com JOIN FETCH para evitar LazyInitializationException
        List list = listRepository.findByIdWithDetails(listId)
                .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // Verificar se usuário é dono
        if (!list.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("Apenas o dono pode editar esta lista");
        }

        // Validar nome após trim (CRÍTICO: @Size valida antes do trim)
        String trimmedName = request.name().trim();
        if (trimmedName.length() < 3) {
            throw new IllegalArgumentException("Nome da lista deve ter pelo menos 3 caracteres");
        }
        if (trimmedName.length() > 100) {
            throw new IllegalArgumentException("Nome da lista deve ter no máximo 100 caracteres");
        }

        list.setName(trimmedName);

        // @PreUpdate vai atualizar o updatedAt automaticamente
        return listRepository.save(list);
    }

    /**
     * Exclui uma lista existente
     * Apenas o dono da lista pode excluí-la
     * CASCADE automático via database deleta itens e membros associados
     *
     * @param listId        ID da lista a ser excluída
     * @param currentUserId ID do usuário autenticado
     * @throws ListNotFoundException se a lista não existir
     * @throws ForbiddenException    se o usuário não for o dono da lista
     */
    @Transactional
    public void deleteList(UUID listId, UUID currentUserId) {
        // Buscar a lista com JOIN FETCH para evitar LazyInitializationException
        List list = listRepository.findByIdWithDetails(listId)
                .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // Verificar se usuário é dono
        if (!list.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("Apenas o dono pode excluir esta lista");
        }

        // Audit log (SECURITY/COMPLIANCE)
        log.info("List deletion: listId={}, listName='{}', ownerId={}, deletedBy={}, typeId={}",
                list.getId(), list.getName(), list.getOwner().getId(), currentUserId, list.getTypeId());

        // Deletar a lista (CASCADE automático via database deleta itens e membros)
        listRepository.delete(list);
    }
}
