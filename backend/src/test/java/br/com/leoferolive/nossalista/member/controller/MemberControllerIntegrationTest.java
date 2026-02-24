package br.com.leoferolive.nossalista.member.controller;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
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

    private MockMvc mockMvc;
    private User owner;
    private User member;
    private User target;
    private List list;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        owner = userService.createUser("owner", "owner@example.com", "hashed", "Owner", AuthProvider.EMAIL);
        member = userService.createUser("member", "member@example.com", "hashed", "Member", AuthProvider.EMAIL);
        target = userService.createUser("pedro", "pedro@example.com", "hashed", "Pedro", AuthProvider.EMAIL);

        list = new List();
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

    @Test
    @DisplayName("Deve retornar 201 quando owner convida usuário existente")
    void shouldReturn201WhenOwnerInvitesExistingUser() throws Exception {
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .with(user(owner.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"username\":\"pedro\"" +
                    "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invited_username").value("pedro"))
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
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .with(user(member.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"pedro\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Apenas o dono pode convidar usuários"));
    }

    @Test
    @DisplayName("Deve retornar 404 quando usuário não existe")
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .with(user(owner.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"inexistente\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("Deve retornar 409 quando usuário já é membro")
    void shouldReturn409WhenUserAlreadyMember() throws Exception {
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .with(user(owner.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"member\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Usuário já é membro"));
    }

    @Test
    @DisplayName("Deve retornar 409 quando owner tenta convidar a si mesmo")
    void shouldReturn409WhenOwnerInvitesSelf() throws Exception {
        mockMvc.perform(post("/api/lists/{id}/invite", list.getId())
                .with(user(owner.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"owner\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Você não pode convidar a si mesmo"));
    }

    @Test
    @DisplayName("Deve retornar membros ordenados com OWNER primeiro")
    void shouldReturnMembersOrderedWithOwnerFirst() throws Exception {
        mockMvc.perform(get("/api/lists/{id}/members", list.getId())
                .with(user(member.getId().toString())))
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
        mockMvc.perform(post("/api/lists/{id}/leave", list.getId())
                .with(user(member.getId().toString())))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/{id}/members", list.getId())
                .with(user(member.getId().toString())))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 403 quando OWNER tenta sair da lista")
    void shouldReturn403WhenOwnerTriesToLeave() throws Exception {
        mockMvc.perform(post("/api/lists/{id}/leave", list.getId())
                .with(user(owner.getId().toString())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("O dono nao pode sair. Transfira ou exclua a lista."));
    }

    @Test
    @DisplayName("Deve retornar 401 no POST leave sem autenticação")
    void shouldReturn401OnLeaveWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/lists/{id}/leave", list.getId()))
            .andExpect(status().isUnauthorized());
    }
}
