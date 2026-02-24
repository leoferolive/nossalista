package br.com.leoferolive.nossalista.member.service;

import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.dto.InviteByUsernameResponse;
import br.com.leoferolive.nossalista.member.exception.MemberInvitationConflictException;
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

    public MemberService(
        ListRepository listRepository,
        UserRepository userRepository,
        ListMemberRepository listMemberRepository
    ) {
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.listMemberRepository = listMemberRepository;
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

        return new InviteByUsernameResponse(
            userToInvite.getUsername(),
            userToInvite.getUsername() + " adicionado!"
        );
    }
}
