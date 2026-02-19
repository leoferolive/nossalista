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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ListService
 */
@ExtendWith(MockitoExtension.class)
class ListServiceTest {

    @Mock
    private ListRepository listRepository;

    @Mock
    private ListTypeRepository listTypeRepository;

    @Mock
    private ListMemberRepository listMemberRepository;

    @InjectMocks
    private ListService listService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
    }

    @Test
    @DisplayName("Deve criar lista com dados válidos e membro OWNER automaticamente")
    void shouldCreateListWithValidDataAndOwnerMember() {
        // Arrange
        CreateListRequest request = new CreateListRequest("Mercado Semanal", 1);
        when(listTypeRepository.existsById(1)).thenReturn(true);
        when(listRepository.save(any(List.class))).thenAnswer(invocation -> {
            List list = invocation.getArgument(0);
            return list;
        });
        when(listMemberRepository.save(any(ListMember.class))).thenAnswer(invocation -> {
            ListMember member = invocation.getArgument(0);
            return member;
        });

        // Act
        List result = listService.createList(request, testUser);

        // Assert - Lista
        assertNotNull(result.getId(), "ID deve ser gerado");
        assertEquals("Mercado Semanal", result.getName(), "Nome deve ser o mesmo da request");
        assertEquals(1, result.getTypeId(), "typeId deve ser o mesmo da request");
        assertEquals(testUser.getId(), result.getOwner().getId(), "owner deve ser o usuário fornecido");
        assertNotNull(result.getInviteCode(), "inviteCode deve ser gerado");
        assertEquals(12, result.getInviteCode().length(), "inviteCode deve ter 12 caracteres");
        assertTrue(result.getInviteCode().matches("[A-Z0-9]+"), "inviteCode deve ser alfanumérico uppercase");

        // Assert - Membro OWNER criado automaticamente (Story 4.1)
        ArgumentCaptor<ListMember> memberCaptor = ArgumentCaptor.forClass(ListMember.class);
        verify(listMemberRepository).save(memberCaptor.capture());
        ListMember savedMember = memberCaptor.getValue();
        assertEquals(result.getId(), savedMember.getList().getId(), "Membro deve estar associado à lista criada");
        assertEquals(testUser.getId(), savedMember.getUser().getId(), "Membro deve ser o usuário owner");
        assertEquals(MemberRole.OWNER, savedMember.getRole(), "Role deve ser OWNER");

        verify(listTypeRepository).existsById(1);
        verify(listRepository).save(any(List.class));
    }

    @Test
    @DisplayName("Deve lançar InvalidListTypeException quando typeId não existe")
    void shouldThrowExceptionWhenTypeIdInvalid() {
        // Arrange
        CreateListRequest request = new CreateListRequest("Test List", 999);
        when(listTypeRepository.existsById(999)).thenReturn(false);

        // Act & Assert
        InvalidListTypeException exception = assertThrows(
            InvalidListTypeException.class,
            () -> listService.createList(request, testUser)
        );

        assertEquals(999, exception.getInvalidTypeId());
        assertTrue(exception.getMessage().contains("Tipo de lista inválido"));
        assertTrue(exception.getMessage().contains("999"));

        verify(listTypeRepository).existsById(999);
        verify(listRepository, never()).save(any(List.class));
    }

    @Test
    @DisplayName("Deve gerar códigos de convite únicos")
    void shouldGenerateUniqueInviteCodes() {
        // Arrange
        CreateListRequest request = new CreateListRequest("Lista 1", 1);
        when(listTypeRepository.existsById(1)).thenReturn(true);
        when(listRepository.save(any(List.class))).thenAnswer(invocation -> {
            List list = invocation.getArgument(0);
            return list;
        });
        when(listMemberRepository.save(any(ListMember.class))).thenAnswer(invocation -> {
            ListMember member = invocation.getArgument(0);
            return member;
        });

        // Act
        List list1 = listService.createList(request, testUser);
        List list2 = listService.createList(request, testUser);

        // Assert
        assertNotEquals(list1.getInviteCode(), list2.getInviteCode(),
            "Códigos de convite devem ser únicos entre criações consecutivas");
    }

    @Test
    @DisplayName("Deve fazer retry quando houver colisão de invite code")
    void shouldRetryWhenInviteCodeCollides() {
        // Arrange
        CreateListRequest request = new CreateListRequest("Lista com Colisão", 1);
        when(listTypeRepository.existsById(1)).thenReturn(true);
        // Simula colisão no primeiro código, aceita no segundo
        when(listRepository.existsByInviteCode(any())).thenReturn(true, false);
        when(listRepository.save(any(List.class))).thenAnswer(invocation -> {
            List list = invocation.getArgument(0);
            return list;
        });
        when(listMemberRepository.save(any(ListMember.class))).thenAnswer(invocation -> {
            ListMember member = invocation.getArgument(0);
            return member;
        });

        // Act
        List result = listService.createList(request, testUser);

        // Assert - verifica que o retry foi executado (existsByInviteCode chamado 2x)
        assertNotNull(result.getInviteCode());
        verify(listRepository, times(2)).existsByInviteCode(any());
        verify(listRepository).save(any(List.class));
        verify(listMemberRepository).save(any(ListMember.class));
    }

    @Test
    @DisplayName("Deve lançar InviteCodeGenerationException após máximo de tentativas")
    void shouldThrowExceptionAfterMaxRetries() {
        // Arrange
        CreateListRequest request = new CreateListRequest("Lista Colisão Máxima", 1);
        when(listTypeRepository.existsById(1)).thenReturn(true);
        // Simula colisão nas primeiras 9 tentativas (a 10ª verificação não ocorre pois o throw acontece antes)
        when(listRepository.existsByInviteCode(any())).thenReturn(
            true, true, true, true, true, true, true, true, true
        );

        // Act & Assert
        InviteCodeGenerationException exception = assertThrows(
            InviteCodeGenerationException.class,
            () -> listService.createList(request, testUser)
        );

        assertEquals(10, exception.getMaxAttempts());
        verify(listRepository, times(9)).existsByInviteCode(any());
        verify(listRepository, never()).save(any(List.class));
    }

    @Nested
    @DisplayName("getListById - Buscar Lista por ID")
    class GetListByIdTests {

        private List testList;
        private UUID listId;

        @BeforeEach
        void setUpList() {
            listId = UUID.randomUUID();
            testList = new List();
            testList.setId(listId);
            testList.setName("Minha Lista de Teste");
            testList.setTypeId(1);
            testList.setOwner(testUser);
            testList.setInviteCode("TEST12345678");
        }

        @Test
        @DisplayName("Deve retornar lista quando usuário é owner")
        void shouldReturnListWhenUserIsOwner() {
            // Arrange
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));

            // Act
            List result = listService.getListById(listId, testUser.getId());

            // Assert
            assertNotNull(result);
            assertEquals(listId, result.getId());
            assertEquals("Minha Lista de Teste", result.getName());
            assertEquals(testUser.getId(), result.getOwner().getId());
            verify(listRepository).findByIdWithDetails(listId);
        }

        @Test
        @DisplayName("Deve lançar ListNotFoundException quando lista não existe")
        void shouldThrowListNotFoundExceptionWhenListDoesNotExist() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(listRepository.findByIdWithDetails(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            ListNotFoundException exception = assertThrows(
                ListNotFoundException.class,
                () -> listService.getListById(nonExistentId, testUser.getId())
            );

            assertEquals("Lista não encontrada", exception.getMessage());
            verify(listRepository).findByIdWithDetails(nonExistentId);
        }

        @Test
        @DisplayName("Deve lançar ForbiddenException quando usuário não é owner")
        void shouldThrowForbiddenExceptionWhenUserIsNotOwner() {
            // Arrange
            UUID otherUserId = UUID.randomUUID();
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));

            // Act & Assert
            ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> listService.getListById(listId, otherUserId)
            );

            assertEquals("Você não tem permissão para acessar esta lista", exception.getMessage());
            verify(listRepository).findByIdWithDetails(listId);
        }
    }

    @Nested
    @DisplayName("updateListName - Atualizar Nome da Lista")
    class UpdateListNameTests {

        private List testList;
        private UUID listId;

        @BeforeEach
        void setUpList() {
            listId = UUID.randomUUID();
            testList = new List();
            testList.setId(listId);
            testList.setName("Nome Original");
            testList.setTypeId(1);
            testList.setOwner(testUser);
            testList.setInviteCode("TEST12345678");
        }

        @Test
        @DisplayName("Deve atualizar nome quando usuário é dono")
        void shouldUpdateNameWhenUserIsOwner() {
            // Arrange
            UpdateListNameRequest request = new UpdateListNameRequest("Novo Nome da Lista");
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));
            when(listRepository.save(any(List.class))).thenAnswer(invocation -> {
                List list = invocation.getArgument(0);
                return list;
            });

            // Act
            List result = listService.updateListName(listId, testUser.getId(), request);

            // Assert
            assertNotNull(result);
            assertEquals("Novo Nome da Lista", result.getName());
            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository).save(any(List.class));
        }

        @Test
        @DisplayName("Deve lançar ListNotFoundException quando lista não existe")
        void shouldThrowListNotFoundExceptionWhenListDoesNotExist() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            UpdateListNameRequest request = new UpdateListNameRequest("Novo Nome");
            when(listRepository.findByIdWithDetails(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            ListNotFoundException exception = assertThrows(
                ListNotFoundException.class,
                () -> listService.updateListName(nonExistentId, testUser.getId(), request)
            );

            assertEquals("Lista não encontrada", exception.getMessage());
            verify(listRepository).findByIdWithDetails(nonExistentId);
            verify(listRepository, never()).save(any(List.class));
        }

        @Test
        @DisplayName("Deve lançar ForbiddenException quando usuário não é dono")
        void shouldThrowForbiddenExceptionWhenUserIsNotOwner() {
            // Arrange
            UUID otherUserId = UUID.randomUUID();
            UpdateListNameRequest request = new UpdateListNameRequest("Novo Nome");
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));

            // Act & Assert
            ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> listService.updateListName(listId, otherUserId, request)
            );

            assertEquals("Apenas o dono pode editar esta lista", exception.getMessage());
            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository, never()).save(any(List.class));
        }

        @Test
        @DisplayName("Deve fazer trim no nome antes de salvar")
        void shouldTrimNameBeforeSaving() {
            // Arrange
            UpdateListNameRequest request = new UpdateListNameRequest("  Nome com espaços  ");
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));
            when(listRepository.save(any(List.class))).thenAnswer(invocation -> {
                List list = invocation.getArgument(0);
                return list;
            });

            // Act
            List result = listService.updateListName(listId, testUser.getId(), request);

            // Assert
            assertEquals("Nome com espaços", result.getName());
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando nome pós-trim tem menos de 3 caracteres")
        void shouldThrowExceptionWhenTrimmedNameTooShort() {
            // Arrange
            UpdateListNameRequest request = new UpdateListNameRequest("  AB  "); // 4 chars original, 2 pós-trim
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> listService.updateListName(listId, testUser.getId(), request)
            );

            assertEquals("Nome da lista deve ter pelo menos 3 caracteres", exception.getMessage());
            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository, never()).save(any(List.class));
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando nome pós-trim tem mais de 100 caracteres")
        void shouldThrowExceptionWhenTrimmedNameTooLong() {
            // Arrange - 102 chars com espaços, 101 pós-trim
            String longName = " " + "A".repeat(101) + " ";
            UpdateListNameRequest request = new UpdateListNameRequest(longName);
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> listService.updateListName(listId, testUser.getId(), request)
            );

            assertEquals("Nome da lista deve ter no máximo 100 caracteres", exception.getMessage());
            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository, never()).save(any(List.class));
        }
    }

    @Nested
    @DisplayName("deleteList - Excluir Lista")
    class DeleteListTests {

        private List testList;
        private UUID listId;

        @BeforeEach
        void setUpList() {
            listId = UUID.randomUUID();
            testList = new List();
            testList.setId(listId);
            testList.setName("Lista para Excluir");
            testList.setTypeId(1);
            testList.setOwner(testUser);
            testList.setInviteCode("TEST12345678");
        }

        @Test
        @DisplayName("Deve excluir lista quando usuário é dono")
        void shouldDeleteListWhenUserIsOwner() {
            // Arrange
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));
            doNothing().when(listRepository).delete(testList);

            // Act
            listService.deleteList(listId, testUser.getId());

            // Assert
            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository).delete(testList);
        }

        @Test
        @DisplayName("Deve lançar ListNotFoundException quando lista não existe")
        void shouldThrowListNotFoundExceptionWhenListDoesNotExist() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(listRepository.findByIdWithDetails(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            ListNotFoundException exception = assertThrows(
                ListNotFoundException.class,
                () -> listService.deleteList(nonExistentId, testUser.getId())
            );

            assertEquals("Lista não encontrada", exception.getMessage());
            verify(listRepository).findByIdWithDetails(nonExistentId);
            verify(listRepository, never()).delete(any(List.class));
        }

        @Test
        @DisplayName("Deve lançar ForbiddenException quando usuário não é dono")
        void shouldThrowForbiddenExceptionWhenUserIsNotOwner() {
            // Arrange
            UUID otherUserId = UUID.randomUUID();
            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));

            // Act & Assert
            ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> listService.deleteList(listId, otherUserId)
            );

            assertEquals("Apenas o dono pode excluir esta lista", exception.getMessage());
            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository, never()).delete(any(List.class));
        }
    }

    @Nested
    @DisplayName("generateInviteLink - Gerar Link de Convite")
    class GenerateInviteLinkTests {

        private List testList;
        private UUID listId;

        @BeforeEach
        void setUpList() {
            listId = UUID.randomUUID();
            testList = new List();
            testList.setId(listId);
            testList.setName("Lista de Teste");
            testList.setTypeId(1);
            testList.setOwner(testUser);
        }

        @Test
        @DisplayName("Deve gerar novo invite link quando não existe código")
        void shouldGenerateNewInviteLinkWhenNoCodeExists() {
            // Arrange
            testList.setInviteCode(null);
            testList.setInviteExpiresAt(null);

            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));
            when(listRepository.existsByInviteCode(any())).thenReturn(false);
            when(listRepository.save(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            List result = listService.generateInviteLink(listId, testUser.getId());

            // Assert
            assertNotNull(result.getInviteCode(), "inviteCode deve ser gerado");
            assertEquals(12, result.getInviteCode().length(), "inviteCode deve ter 12 caracteres");
            assertNotNull(result.getInviteExpiresAt(), "expiresAt deve ser definido");

            // Verificar expiração é aproximadamente 24h no futuro
            LocalDateTime expectedExpiry = LocalDateTime.now().plusHours(24);
            long minutesDiff = java.time.Duration.between(result.getInviteExpiresAt(), expectedExpiry).toMinutes();
            assertTrue(Math.abs(minutesDiff) < 1, "expiresAt deve ser aproximadamente 24h no futuro");

            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository).save(any(List.class));
        }

        @Test
        @DisplayName("Deve reutilizar código válido existente quando não expirou")
        void shouldReuseValidInviteCode() {
            // Arrange
            String existingCode = "ABC123XYZ789";
            LocalDateTime futureExpiry = LocalDateTime.now().plusHours(12); // Ainda válido por 12h

            testList.setInviteCode(existingCode);
            testList.setInviteExpiresAt(futureExpiry);

            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));

            // Act
            List result = listService.generateInviteLink(listId, testUser.getId());

            // Assert
            assertEquals(existingCode, result.getInviteCode(), "Deve reutilizar o código existente");
            assertEquals(futureExpiry, result.getInviteExpiresAt(), "expiry não deve mudar");

            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository, never()).save(any(List.class)); // Não deve salvar nada
        }

        @Test
        @DisplayName("Deve regenerar código quando invite expirou")
        void shouldRegenerateExpiredInviteCode() {
            // Arrange
            String expiredCode = "OLD123CODE99";
            LocalDateTime pastExpiry = LocalDateTime.now().minusHours(1); // Expirado há 1h

            testList.setInviteCode(expiredCode);
            testList.setInviteExpiresAt(pastExpiry);

            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));
            when(listRepository.existsByInviteCode(any())).thenReturn(false);
            when(listRepository.save(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            List result = listService.generateInviteLink(listId, testUser.getId());

            // Assert
            assertNotNull(result.getInviteCode());
            assertNotEquals(expiredCode, result.getInviteCode(), "Deve gerar novo código, não reusar o expirado");
            assertEquals(12, result.getInviteCode().length());

            // Nova expiração deve ser no futuro
            assertTrue(result.getInviteExpiresAt().isAfter(LocalDateTime.now()),
                    "Nova expiração deve ser no futuro");

            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository).save(any(List.class));
        }

        @Test
        @DisplayName("Deve lançar ForbiddenException quando usuário não é owner")
        void shouldThrowForbiddenWhenUserIsNotOwner() {
            // Arrange
            UUID otherUserId = UUID.randomUUID();
            testList.setInviteCode("TEST12345678");
            testList.setInviteExpiresAt(LocalDateTime.now().plusHours(24));

            when(listRepository.findByIdWithDetails(listId)).thenReturn(Optional.of(testList));

            // Act & Assert
            ForbiddenException exception = assertThrows(
                    ForbiddenException.class,
                    () -> listService.generateInviteLink(listId, otherUserId)
            );

            assertEquals("Apenas o dono pode gerar link de convite", exception.getMessage());
            verify(listRepository).findByIdWithDetails(listId);
            verify(listRepository, never()).save(any(List.class));
        }

        @Test
        @DisplayName("Deve lançar ListNotFoundException quando lista não existe")
        void shouldThrowListNotFoundWhenListDoesNotExist() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(listRepository.findByIdWithDetails(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            ListNotFoundException exception = assertThrows(
                    ListNotFoundException.class,
                    () -> listService.generateInviteLink(nonExistentId, testUser.getId())
            );

            assertEquals("Lista não encontrada", exception.getMessage());
            verify(listRepository).findByIdWithDetails(nonExistentId);
            verify(listRepository, never()).save(any(List.class));
        }
    }
}
