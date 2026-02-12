package br.com.leoferolive.nossalista.list.service;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.dto.CreateListRequest;
import br.com.leoferolive.nossalista.list.exception.InvalidListTypeException;
import br.com.leoferolive.nossalista.list.exception.InviteCodeGenerationException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.list.repository.ListTypeRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @DisplayName("Deve criar lista com dados válidos")
    void shouldCreateListWithValidData() {
        // Arrange
        CreateListRequest request = new CreateListRequest("Mercado Semanal", 1);
        when(listTypeRepository.existsById(1)).thenReturn(true);
        when(listRepository.save(any(List.class))).thenAnswer(invocation -> {
            List list = invocation.getArgument(0);
            return list;
        });

        // Act
        List result = listService.createList(request, testUser);

        // Assert
        assertNotNull(result.getId(), "ID deve ser gerado");
        assertEquals("Mercado Semanal", result.getName(), "Nome deve ser o mesmo da request");
        assertEquals(1, result.getTypeId(), "typeId deve ser o mesmo da request");
        assertEquals(testUser.getId(), result.getOwner().getId(), "owner deve ser o usuário fornecido");
        assertNotNull(result.getInviteCode(), "inviteCode deve ser gerado");
        assertEquals(12, result.getInviteCode().length(), "inviteCode deve ter 12 caracteres");
        assertTrue(result.getInviteCode().matches("[A-Z0-9]+"), "inviteCode deve ser alfanumérico uppercase");

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

        // Act
        List result = listService.createList(request, testUser);

        // Assert - verifica que o retry foi executado (existsByInviteCode chamado 2x)
        assertNotNull(result.getInviteCode());
        verify(listRepository, times(2)).existsByInviteCode(any());
        verify(listRepository).save(any(List.class));
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
}
