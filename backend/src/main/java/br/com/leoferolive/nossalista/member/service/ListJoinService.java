package br.com.leoferolive.nossalista.member.service;

import br.com.leoferolive.nossalista.activity.service.ActivityLogService;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.dto.JoinListResponse;
import br.com.leoferolive.nossalista.list.dto.ListResponse;
import br.com.leoferolive.nossalista.list.util.ListTypeMapper;
import br.com.leoferolive.nossalista.list.exception.InviteExpiredException;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.dto.ListJoinedResponse;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.notification.NotificationService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import br.com.leoferolive.nossalista.websocket.WebSocketEventPublisher;
import br.com.leoferolive.nossalista.websocket.dto.MemberJoinedPayload;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço para operações de join em listas via convite
 */
@Service
public class ListJoinService {

    private final ListRepository listRepository;
    private final ListItemRepository listItemRepository;
    private final ListMemberRepository listMemberRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final WebSocketEventPublisher eventPublisher;
    private final NotificationService notificationService;

    public ListJoinService(ListRepository listRepository,
                           ListItemRepository listItemRepository,
                           ListMemberRepository listMemberRepository,
                           UserRepository userRepository,
                           ActivityLogService activityLogService,
                           WebSocketEventPublisher eventPublisher,
                           NotificationService notificationService) {
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
        this.listMemberRepository = listMemberRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
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
        String typeSlug = ListTypeMapper.resolveSlug(list.getTypeEntity(), list.getTypeId());
        String typeName = ListTypeMapper.resolveName(list.getTypeEntity(), list.getTypeId());

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

    /**
     * Faz com que um usuário entre em uma lista via código de convite.
     * Endpoint autenticado - requer JWT válido.
     *
     * @param userId     ID do usuário autenticado
     * @param inviteCode código de convite
     * @return ListJoinedResponse com dados da lista e mensagem de boas-vindas
     * @throws ListNotFoundException  se o código de convite não existe (404)
     * @throws InviteExpiredException se o link de convite expirou (410)
     */
    @Transactional
    public ListJoinedResponse joinList(UUID userId, String inviteCode) {
        // 1. Buscar lista por invite_code
        List list = listRepository.findByInviteCodeWithDetails(inviteCode)
            .orElseThrow(() -> new ListNotFoundException("Convite não encontrado"));

        // 2. Validar expiração
        if (list.getInviteExpiresAt() == null ||
            list.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InviteExpiredException("Link expirado");
        }

        // 3. Carregar usuário (userId vem do JWT válido; ausência indica inconsistência de estado)
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new br.com.leoferolive.nossalista.user.exception.NotAuthenticatedException(
                "Usuário autenticado não encontrado. Faça login novamente."));

        // 4. Verificar se usuário é o dono
        if (list.getOwner().getId().equals(userId)) {
            return buildListJoinedResponse(list, MemberRole.OWNER,
                "Você é o dono desta lista", false);
        }

        // 5. Verificar se já é membro (idempotência)
        Optional<ListMember> existingMember = listMemberRepository
            .findByListIdAndUserId(list.getId(), userId);

        if (existingMember.isPresent()) {
            return buildListJoinedResponse(list, existingMember.get().getRole(),
                "Você já é membro desta lista", false);
        }

        // 6. Criar novo membro (com tratamento de race condition)
        try {
            ListMember newMember = new ListMember();
            newMember.setList(list);
            newMember.setUser(user);
            newMember.setRole(MemberRole.MEMBER);
            listMemberRepository.save(newMember);
            activityLogService.logMemberJoinedViaLink(list, user);

            MemberJoinedPayload joinedPayload = new MemberJoinedPayload(
                user.getId().toString(), user.getUsername(),
                user.getName(), user.getAvatarUrl(),
                list.getId().toString(), list.getName()
            );
            eventPublisher.publishEvent(list.getId(), "MEMBER_JOINED", joinedPayload, user, null);
            notificationService.notifyListMembers(list.getId(), user.getId(), "MEMBER_JOINED", joinedPayload, user);

            return buildListJoinedResponse(list, MemberRole.MEMBER,
                "Bem-vindo à lista " + list.getName() + "!", true);
        } catch (DataIntegrityViolationException e) {
            // Race condition: outro request criou o member no meio tempo
            ListMember member = listMemberRepository
                .findByListIdAndUserId(list.getId(), userId)
                .orElseThrow(() -> new IllegalStateException("Erro inesperado ao recuperar membro"));

            return buildListJoinedResponse(list, member.getRole(),
                "Você já é membro desta lista", false);
        }
    }

    /**
     * Constrói o response de join com dados completos da lista.
     */
    private ListJoinedResponse buildListJoinedResponse(List list, MemberRole role, String message, boolean created) {
        String typeSlug = ListTypeMapper.resolveSlug(list.getTypeEntity(), list.getTypeId());
        String typeName = ListTypeMapper.resolveName(list.getTypeEntity(), list.getTypeId());

        // Construir ListResponse completo
        ListResponse.TypeResponse typeResponse = new ListResponse.TypeResponse(
            ListTypeMapper.resolveId(list.getTypeEntity(), list.getTypeId()),
            typeName,
            typeSlug
        );

        ListResponse.OwnerResponse ownerResponse = new ListResponse.OwnerResponse(
            list.getOwner().getId(),
            list.getOwner().getUsername(),
            list.getOwner().getName(),
            list.getOwner().getAvatarUrl()
        );

        // Contar itens da lista
        int itemsCount = listItemRepository.countByListId(list.getId()).intValue();

        ListResponse listResponse = new ListResponse(
            list.getId(),
            list.getName(),
            typeResponse,
            ownerResponse,
            list.getInviteCode(),
            role == MemberRole.OWNER,
            itemsCount,
            list.getCreatedAt(),
            list.getUpdatedAt()
        );

        return new ListJoinedResponse(
            list.getId(),
            list.getName(),
            typeSlug,
            typeName,
            role.name(),
            message,
            created,
            listResponse
        );
    }
}
