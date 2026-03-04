package br.com.leoferolive.nossalista.member.service;

import br.com.leoferolive.nossalista.activity.service.ActivityLogService;
import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.dto.InviteByUsernameResponse;
import br.com.leoferolive.nossalista.member.dto.ListMemberResponse;
import br.com.leoferolive.nossalista.member.exception.MemberInvitationConflictException;
import br.com.leoferolive.nossalista.member.exception.MemberNotFoundException;
import br.com.leoferolive.nossalista.member.exception.UserNotFoundForInviteException;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MemberService {

    private final ListRepository listRepository;
    private final UserRepository userRepository;
    private final ListMemberRepository listMemberRepository;
    private final ActivityLogService activityLogService;

    public MemberService(
        ListRepository listRepository,
        UserRepository userRepository,
        ListMemberRepository listMemberRepository,
        ActivityLogService activityLogService
    ) {
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.listMemberRepository = listMemberRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public InviteByUsernameResponse inviteByUsername(UUID listId, UUID requesterId, String username) {
        List list = listRepository.findByIdWithDetails(listId)
            .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        if (!list.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenException("Apenas o dono pode convidar usuários");
        }

        String normalizedUsername = username == null ? "" : username.trim();
        User userToInvite = userRepository.findByUsernameIgnoreCase(normalizedUsername)
            .orElseThrow(() -> new UserNotFoundForInviteException("Usuário não encontrado"));

        if (userToInvite.getId().equals(requesterId)) {
            throw new MemberInvitationConflictException("Você não pode convidar a si mesmo");
        }

        if (listMemberRepository.existsByListIdAndUserId(listId, userToInvite.getId())) {
            throw new MemberInvitationConflictException("Usuário já é membro");
        }

        ListMember member = new ListMember();
        member.setList(list);
        member.setUser(userToInvite);
        member.setRole(MemberRole.MEMBER);

        try {
            listMemberRepository.save(member);
        } catch (DataIntegrityViolationException ex) {
            throw new MemberInvitationConflictException("Usuário já é membro");
        }

        activityLogService.logMemberInvitedByUsername(list, userToInvite, list.getOwner());

        return new InviteByUsernameResponse(
            userToInvite.getUsername(),
            userToInvite.getUsername() + " adicionado!"
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<ListMemberResponse> getMembers(UUID listId, UUID requesterId) {
        ensureUserIsMember(listId, requesterId);

        return listMemberRepository.findMembersForListOrdered(listId)
            .stream()
            .map(member -> new ListMemberResponse(
                new ListMemberResponse.UserSummaryResponse(
                    member.getUser().getId(),
                    member.getUser().getUsername(),
                    member.getUser().getName(),
                    member.getUser().getAvatarUrl()
                ),
                member.getRole().name(),
                member.getCreatedAt()
            ))
            .toList();
    }

    @Transactional
    public void removeMember(UUID listId, UUID requesterId, UUID targetUserId) {
        List list = listRepository.findByIdWithDetails(listId)
            .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        // AC3: Apenas OWNER pode remover participantes
        if (!list.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenException("Apenas o dono pode remover participantes");
        }

        // AC2: O dono não pode ser removido
        if (targetUserId.equals(list.getOwner().getId())) {
            throw new ForbiddenException("O dono não pode ser removido");
        }

        // AC1: Verificar se targetUser é membro (404 para não vazar informações)
        ListMember targetMembership = listMemberRepository.findByListIdAndUserId(listId, targetUserId)
            .orElseThrow(() -> new MemberNotFoundException("Usuário não é membro desta lista"));

        activityLogService.logMemberRemoved(list, targetMembership.getUser(), list.getOwner());
        listMemberRepository.delete(targetMembership);
    }

    @Transactional
    public void leaveList(UUID listId, UUID requesterId) {
        ListMember membership = ensureUserIsMember(listId, requesterId);

        if (membership.getRole() == MemberRole.OWNER) {
            throw new ForbiddenException("O dono nao pode sair. Transfira ou exclua a lista.");
        }

        activityLogService.logMemberLeft(membership.getList(), membership.getUser());
        listMemberRepository.delete(membership);
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID listId, UUID userId) {
        return listMemberRepository.existsByListIdAndUserId(listId, userId);
    }

    private ListMember ensureUserIsMember(UUID listId, UUID requesterId) {
        listRepository.findById(listId)
            .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

        return listMemberRepository.findByListIdAndUserId(listId, requesterId)
            .orElseThrow(() -> new ForbiddenException("Você não tem permissão para acessar esta lista"));
    }
}
