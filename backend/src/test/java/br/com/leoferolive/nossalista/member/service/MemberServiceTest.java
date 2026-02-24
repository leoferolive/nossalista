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

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("leo");

        targetUser = new User();
        targetUser.setId(UUID.randomUUID());
        targetUser.setUsername("pedro");

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
}
