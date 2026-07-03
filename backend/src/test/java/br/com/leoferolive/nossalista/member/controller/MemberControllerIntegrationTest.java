package br.com.leoferolive.nossalista.member.controller;

import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.support.RegressionTest;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@RegressionTest
@DisplayName("MemberController Integration Tests")
class MemberControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private ListMemberRepository listMemberRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private User owner;
    private User member;
    private User target;
    private SharedList list;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        SecurityContextHolder.clearContext();
        TestSecurityContextHolder.clearContext();

        owner = userService.createUser("owner", "owner@example.com", "hashed", "Owner", AuthProvider.EMAIL);
        member = userService.createUser("member", "member@example.com", "hashed", "Member", AuthProvider.EMAIL);
        target = userService.createUser("pedro", "pedro@example.com", "hashed", "Pedro", AuthProvider.EMAIL);

        list = new SharedList();
        list.setName("Lista Convite");
        list.setTypeId(1);
        list.setOwner(owner);
        list = listRepository.save(list);

        ListMember ownerMember = new ListMember();
        ownerMember.setList(list);
        ownerMember.setUser(owner);
        ownerMember.setRole(MemberRole.OWNER);
        listMemberRepository.save(ownerMember);

        ListMember regularMember = new ListMember();
        regularMember.setList(list);
        regularMember.setUser(member);
        regularMember.setRole(MemberRole.MEMBER);
        listMemberRepository.save(regularMember);
    }

    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        TestSecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("Deve retornar 201 quando owner convida usuário existente")
    void shouldReturn201WhenOwnerInvitesExistingUser() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"pedro\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invitedUsername").value("pedro"))
            .andExpect(jsonPath("$.message").value("pedro adicionado!"));
    }

    @Test
    @DisplayName("Deve retornar 201 com header Authorization Bearer JWT válido")
    void shouldReturn201WithValidJwtHeader() throws Exception {
        String jwt = jwtService.generateToken(owner);

        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"pedro\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invitedUsername").value("pedro"))
            .andExpect(jsonPath("$.message").value("pedro adicionado!"));
    }

    @Test
    @DisplayName("Deve retornar 401 sem token")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"pedro\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 403 quando membro tenta convidar")
    void shouldReturn403WhenMemberTriesInvite() throws Exception {
        authenticateUser(member);
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"pedro\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Apenas o dono pode convidar usuários"));
    }

    @Test
    @DisplayName("Deve retornar 404 quando usuário não existe")
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"inexistente\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("Deve retornar 409 quando usuário já é membro")
    void shouldReturn409WhenUserAlreadyMember() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"member\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Usuário já é membro"));
    }

    @Test
    @DisplayName("Deve retornar 409 quando owner tenta convidar a si mesmo")
    void shouldReturn409WhenOwnerInvitesSelf() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"owner\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Você não pode convidar a si mesmo"));
    }

    @Test
    @DisplayName("Deve retornar membros ordenados com OWNER primeiro")
    void shouldReturnMembersOrderedWithOwnerFirst() throws Exception {
        authenticateUser(member);
        mockMvc.perform(get("/api/lists/{id}/members", list.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].user.username").value("owner"))
            .andExpect(jsonPath("$[0].role").value("OWNER"))
            .andExpect(jsonPath("$[1].user.username").value("member"))
            .andExpect(jsonPath("$[1].role").value("MEMBER"));
    }

    @Test
    @DisplayName("Deve retornar 401 no GET members sem autenticação")
    void shouldReturn401OnGetMembersWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/lists/{id}/members", list.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve permitir MEMBER sair da lista com 204")
    void shouldAllowMemberToLeaveList() throws Exception {
        authenticateUser(member);
        mockMvc.perform(post("/api/lists/{id}/leave", list.getId()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/{id}/members", list.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 403 quando OWNER tenta sair da lista")
    void shouldReturn403WhenOwnerTriesToLeave() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(post("/api/lists/{id}/leave", list.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("O dono nao pode sair. Transfira ou exclua a lista."));
    }

    @Test
    @DisplayName("Deve retornar 401 no POST leave sem autenticação")
    void shouldReturn401OnLeaveWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/lists/{id}/leave", list.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /members/{userId} com OWNER removendo MEMBER → 204")
    void shouldReturn204WhenOwnerRemovesMember() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), member.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /members/{userId} sem autenticação → 401")
    void shouldReturn401OnDeleteMemberWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), member.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /members/{userId} por MEMBER (não OWNER) → 403")
    void shouldReturn403WhenMemberTriesToRemove() throws Exception {
        authenticateUser(member);
        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), target.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Apenas o dono pode remover participantes"));
    }

    @Test
    @DisplayName("DELETE /members/{ownerId} tentando remover o OWNER → 403")
    void shouldReturn403WhenTryingToRemoveOwner() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), owner.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("O dono não pode ser removido"));
    }

    @Test
    @DisplayName("DELETE /members/{userId} com userId não membro → 404")
    void shouldReturn404WhenTargetNotMember() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), target.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Usuário não é membro desta lista"));
    }

    @Test
    @DisplayName("Membro removido não aparece na lista de membros após remoção")
    void shouldRemoveMemberFromMembersListAfterDeletion() throws Exception {
        authenticateUser(owner);
        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), member.getId()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/{id}/members", list.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.user.username == 'member')]").doesNotExist())
            .andExpect(jsonPath("$[?(@.user.username == 'owner')]").exists());
    }
}
