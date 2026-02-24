package br.com.leoferolive.nossalista.member.service;

import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.dto.InviteByUsernameResponse;
import br.com.leoferolive.nossalista.member.dto.ListMemberResponse;
import br.com.leoferolive.nossalista.member.exception.MemberInvitationConflictException;
import br.com.leoferolive.nossalista.member.exception.UserNotFoundForInviteException;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService Tests")
class MemberServiceTest {

    @Mock
    private ListRepository listRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListMemberRepository listMemberRepository;

    @InjectMocks
    private MemberService memberService;

    private List testList;
    private User owner;
    private User targetUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("leo");

        targetUser = new User();
        targetUser.setId(UUID.randomUUID());
        targetUser.setUsername("pedro");

        memberUser = new User();
        memberUser.setId(UUID.randomUUID());
        memberUser.setUsername("member");

        testList = new List();
        testList.setId(UUID.randomUUID());
        testList.setOwner(owner);
        testList.setName("Lista compartilhada");
    }

    @Test
    @DisplayName("Deve convidar usuário com sucesso")
    void shouldInviteUserSuccessfully() {
        when(listRepository.findByIdWithDetails(testList.getId())).thenReturn(Optional.of(testList));
        when(userRepository.findByUsernameIgnoreCase("pedro")).thenReturn(Optional.of(targetUser));
        when(listMemberRepository.existsByListIdAndUserId(testList.getId(), targetUser.getId())).thenReturn(false);

        InviteByUsernameResponse response = memberService.inviteByUsername(testList.getId(), owner.getId(), "pedro");

        assertThat(response.invited_username()).isEqualTo("pedro");
        assertThat(response.message()).isEqualTo("pedro adicionado!");
        verify(listMemberRepository).save(any(ListMember.class));
    }

    @Test
    @DisplayName("Deve lançar 404 quando usuário não existe")
    void shouldThrow404WhenUserNotFound() {
        when(listRepository.findByIdWithDetails(testList.getId())).thenReturn(Optional.of(testList));
        when(userRepository.findByUsernameIgnoreCase("pedro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.inviteByUsername(testList.getId(), owner.getId(), "pedro"))
            .isInstanceOf(UserNotFoundForInviteException.class)
            .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    @DisplayName("Deve lançar 409 quando usuário já é membro")
    void shouldThrow409WhenAlreadyMember() {
        when(listRepository.findByIdWithDetails(testList.getId())).thenReturn(Optional.of(testList));
        when(userRepository.findByUsernameIgnoreCase("pedro")).thenReturn(Optional.of(targetUser));
        when(listMemberRepository.existsByListIdAndUserId(testList.getId(), targetUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> memberService.inviteByUsername(testList.getId(), owner.getId(), "pedro"))
            .isInstanceOf(MemberInvitationConflictException.class)
            .hasMessageContaining("Usuário já é membro");
    }

    @Test
    @DisplayName("Deve lançar 409 quando owner tenta convidar a si mesmo")
    void shouldThrow409WhenOwnerInvitesSelf() {
        when(listRepository.findByIdWithDetails(testList.getId())).thenReturn(Optional.of(testList));
        when(userRepository.findByUsernameIgnoreCase("leo")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> memberService.inviteByUsername(testList.getId(), owner.getId(), "leo"))
            .isInstanceOf(MemberInvitationConflictException.class)
            .hasMessageContaining("Você não pode convidar a si mesmo");
    }

    @Test
    @DisplayName("Deve lançar 403 quando solicitante não é owner")
    void shouldThrow403WhenRequesterIsNotOwner() {
        UUID memberId = UUID.randomUUID();
        when(listRepository.findByIdWithDetails(testList.getId())).thenReturn(Optional.of(testList));

        assertThatThrownBy(() -> memberService.inviteByUsername(testList.getId(), memberId, "pedro"))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Apenas o dono pode convidar usuários");
    }

    @Test
    @DisplayName("Deve lançar 404 quando lista não existe")
    void shouldThrow404WhenListNotFound() {
        when(listRepository.findByIdWithDetails(testList.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.inviteByUsername(testList.getId(), owner.getId(), "pedro"))
            .isInstanceOf(ListNotFoundException.class)
            .hasMessageContaining("Lista não encontrada");
    }

    @Test
    @DisplayName("Deve listar membros para usuário participante")
    void shouldListMembersWhenRequesterIsParticipant() {
        ListMember ownerMembership = new ListMember();
        ownerMembership.setList(testList);
        ownerMembership.setUser(owner);
        ownerMembership.setRole(MemberRole.OWNER);

        ListMember requesterMembership = new ListMember();
        requesterMembership.setList(testList);
        requesterMembership.setUser(memberUser);
        requesterMembership.setRole(MemberRole.MEMBER);

        when(listRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
        when(listMemberRepository.findByListIdAndUserId(testList.getId(), memberUser.getId()))
            .thenReturn(Optional.of(requesterMembership));
        when(listMemberRepository.findMembersForListOrdered(testList.getId()))
            .thenReturn(java.util.List.of(ownerMembership, requesterMembership));

        java.util.List<ListMemberResponse> result = memberService.getMembers(testList.getId(), memberUser.getId());

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().role()).isEqualTo("OWNER");
        assertThat(result.get(1).user().username()).isEqualTo("member");
    }

    @Test
    @DisplayName("Deve remover membership quando membro sai da lista")
    void shouldRemoveMembershipWhenMemberLeaves() {
        ListMember requesterMembership = new ListMember();
        requesterMembership.setList(testList);
        requesterMembership.setUser(memberUser);
        requesterMembership.setRole(MemberRole.MEMBER);

        when(listRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
        when(listMemberRepository.findByListIdAndUserId(testList.getId(), memberUser.getId()))
            .thenReturn(Optional.of(requesterMembership));

        memberService.leaveList(testList.getId(), memberUser.getId());

        verify(listMemberRepository).delete(requesterMembership);
    }

    @Test
    @DisplayName("Deve bloquear saída do dono com 403")
    void shouldBlockOwnerFromLeavingList() {
        ListMember ownerMembership = new ListMember();
        ownerMembership.setList(testList);
        ownerMembership.setUser(owner);
        ownerMembership.setRole(MemberRole.OWNER);

        when(listRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
        when(listMemberRepository.findByListIdAndUserId(testList.getId(), owner.getId()))
            .thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> memberService.leaveList(testList.getId(), owner.getId()))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("O dono nao pode sair");
    }

    @Test
    @DisplayName("Nao membro nao consegue listar membros")
    void shouldDenyListMembersForNonMember() {
        User outsider = new User();
        outsider.setId(UUID.randomUUID());
        outsider.setUsername("outsider");

        when(listRepository.findById(testList.getId())).thenReturn(Optional.of(testList));
        when(listMemberRepository.findByListIdAndUserId(testList.getId(), outsider.getId()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMembers(testList.getId(), outsider.getId()))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Você não tem permissão para acessar esta lista");
    }
}
