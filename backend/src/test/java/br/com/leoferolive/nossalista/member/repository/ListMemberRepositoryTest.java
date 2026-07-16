package br.com.leoferolive.nossalista.member.repository;

import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.support.AbstractPostgresIT;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para ListMemberRepository
 * Valida persistência, constraints e queries.
 *
 * <p>Estende {@link AbstractPostgresIT}: roda contra PostgreSQL real
 * (Testcontainers), não H2 — ver T1 da Onda 2 (honestidade de métrica). O
 * {@code @TestPropertySource} que antes forçava um H2 dedicado
 * ({@code jdbc:h2:mem:testdb}) foi removido: o {@code @ServiceConnection} da
 * base class já configura o DataSource para o container.</p>
 */
@SpringBootTest
@Transactional
class ListMemberRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private ListMemberRepository listMemberRepository;

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User anotherUser;
    private SharedList testList;

    @BeforeEach
    void setUp() {
        // Criar usuários de teste (role obrigatório)
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setAuthProvider(AuthProvider.GOOGLE);
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);

        anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@test.com");
        anotherUser.setAuthProvider(AuthProvider.EMAIL);
        anotherUser.setRole(Role.USER);
        anotherUser.setPassword("password123"); // Required for EMAIL auth provider
        anotherUser = userRepository.save(anotherUser);

        // Criar lista de teste
        testList = new SharedList();
        testList.setName("Lista de Teste");
        testList.setTypeId(1);
        testList.setOwner(testUser);
        testList.setInviteCode("TEST12345678");
        testList = listRepository.save(testList);
    }

    @Test
    @DisplayName("Deve salvar membro OWNER com sucesso")
    void shouldSaveOwnerMemberSuccessfully() {
        // Arrange
        ListMember member = new ListMember();
        member.setList(testList);
        member.setUser(testUser);
        member.setRole(MemberRole.OWNER);

        // Act
        ListMember saved = listMemberRepository.save(member);

        // Assert
        assertNotNull(saved.getId(), "ID deve ser gerado via @PrePersist");
        assertEquals(testList.getId(), saved.getList().getId());
        assertEquals(testUser.getId(), saved.getUser().getId());
        assertEquals(MemberRole.OWNER, saved.getRole());
    }

    @Test
    @DisplayName("Deve salvar membro MEMBER com sucesso")
    void shouldSaveMemberSuccessfully() {
        // Arrange
        ListMember member = new ListMember();
        member.setList(testList);
        member.setUser(anotherUser);
        member.setRole(MemberRole.MEMBER);

        // Act
        ListMember saved = listMemberRepository.save(member);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(MemberRole.MEMBER, saved.getRole());
    }

    @Test
    @DisplayName("Deve lançar exceção ao violar constraint UNIQUE (list_id, user_id)")
    void shouldThrowExceptionWhenDuplicateListUserPair() {
        // Arrange - Criar primeiro membro
        ListMember member1 = new ListMember();
        member1.setList(testList);
        member1.setUser(testUser);
        member1.setRole(MemberRole.OWNER);
        listMemberRepository.save(member1);
        listMemberRepository.flush();

        // Arrange - Tentar criar segundo membro com mesma lista e usuário
        ListMember member2 = new ListMember();
        member2.setList(testList);
        member2.setUser(testUser);
        member2.setRole(MemberRole.MEMBER);

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            listMemberRepository.save(member2);
            listMemberRepository.flush();
        }, "Deve violar constraint UNIQUE(list_id, user_id)");
    }

    @Test
    @DisplayName("Deve permitir mesmo usuário em listas diferentes")
    void shouldAllowSameUserInDifferentLists() {
        // Arrange - Segunda lista
        SharedList anotherList = new SharedList();
        anotherList.setName("Outra Lista");
        anotherList.setTypeId(2);
        anotherList.setOwner(anotherUser);
        anotherList.setInviteCode("TEST87654321");
        anotherList = listRepository.save(anotherList);

        // Arrange - Membro na primeira lista
        ListMember member1 = new ListMember();
        member1.setList(testList);
        member1.setUser(testUser);
        member1.setRole(MemberRole.OWNER);
        listMemberRepository.save(member1);

        // Arrange - Membro na segunda lista (mesmo usuário)
        ListMember member2 = new ListMember();
        member2.setList(anotherList);
        member2.setUser(testUser);
        member2.setRole(MemberRole.MEMBER);

        // Act
        ListMember saved = listMemberRepository.save(member2);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(2, listMemberRepository.count());
    }

    @Test
    @DisplayName("Deve buscar membro por listId e userId")
    void shouldFindMemberByListIdAndUserId() {
        // Arrange
        ListMember member = new ListMember();
        member.setList(testList);
        member.setUser(testUser);
        member.setRole(MemberRole.OWNER);
        listMemberRepository.save(member);

        // Act
        Optional<ListMember> found = listMemberRepository.findByListIdAndUserId(testList.getId(), testUser.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(MemberRole.OWNER, found.get().getRole());
    }

    @Test
    @DisplayName("Deve retornar empty quando membro não existe")
    void shouldReturnEmptyWhenMemberNotFound() {
        // Act
        Optional<ListMember> found = listMemberRepository.findByListIdAndUserId(
            UUID.randomUUID(), testUser.getId());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Deve verificar se usuário é membro da lista")
    void shouldCheckIfUserIsMemberOfList() {
        // Arrange
        ListMember member = new ListMember();
        member.setList(testList);
        member.setUser(testUser);
        member.setRole(MemberRole.OWNER);
        listMemberRepository.save(member);

        // Act & Assert
        assertTrue(listMemberRepository.existsByListIdAndUserId(testList.getId(), testUser.getId()));
        assertFalse(listMemberRepository.existsByListIdAndUserId(testList.getId(), anotherUser.getId()));
    }

    @Test
    @DisplayName("Deve buscar todos os membros de uma lista")
    void shouldFindAllMembersByListId() {
        // Arrange - Adicionar dois membros à lista
        ListMember owner = new ListMember();
        owner.setList(testList);
        owner.setUser(testUser);
        owner.setRole(MemberRole.OWNER);
        listMemberRepository.save(owner);

        ListMember member = new ListMember();
        member.setList(testList);
        member.setUser(anotherUser);
        member.setRole(MemberRole.MEMBER);
        listMemberRepository.save(member);

        // Act
        java.util.List<ListMember> members = listMemberRepository.findByListId(testList.getId());

        // Assert
        assertEquals(2, members.size());
    }

    @Test
    @DisplayName("Deve buscar todas as listas onde usuário é membro")
    void shouldFindAllListsByUserId() {
        // Arrange
        ListMember member = new ListMember();
        member.setList(testList);
        member.setUser(testUser);
        member.setRole(MemberRole.OWNER);
        listMemberRepository.save(member);

        // Act
        java.util.List<ListMember> memberships = listMemberRepository.findByUserId(testUser.getId());

        // Assert
        assertEquals(1, memberships.size());
        assertEquals(testList.getId(), memberships.get(0).getList().getId());
    }

    @Test
    @DisplayName("Deve persistir membro e permitir busca")
    void shouldPersistAndFindMember() {
        // Arrange
        ListMember member = new ListMember();
        member.setList(testList);
        member.setUser(testUser);
        member.setRole(MemberRole.OWNER);
        listMemberRepository.save(member);
        listMemberRepository.flush();

        // Act - Buscar do banco
        java.util.List<ListMember> members = listMemberRepository.findByListId(testList.getId());

        // Assert
        assertEquals(1, members.size());
        assertEquals(testUser.getId(), members.get(0).getUser().getId());
        assertEquals(MemberRole.OWNER, members.get(0).getRole());
    }

    @Test
    @DisplayName("Role padrão deve ser MEMBER quando não especificado")
    void defaultRoleShouldBeMember() {
        // Arrange
        ListMember member = new ListMember();
        member.setList(testList);
        member.setUser(testUser);
        // Não setar role - deve usar default

        // Act
        ListMember saved = listMemberRepository.save(member);

        // Assert
        assertEquals(MemberRole.MEMBER, saved.getRole());
    }
}
