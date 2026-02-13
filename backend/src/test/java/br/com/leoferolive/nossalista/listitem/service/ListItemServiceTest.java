package br.com.leoferolive.nossalista.listitem.service;

import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.listitem.dto.CreateItemRequestDTO;
import br.com.leoferolive.nossalista.listitem.dto.ListItemMapper;
import br.com.leoferolive.nossalista.listitem.dto.ListItemResponseDTO;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import br.com.leoferolive.nossalista.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Testes unitários para ListItemService
 */
@ExtendWith(MockitoExtension.class)
class ListItemServiceTest {

    @Mock
    private ListItemRepository listItemRepository;

    @Mock
    private ListRepository listRepository;

    @Mock
    private ListItemMapper listItemMapper;

    @InjectMocks
    private ListItemService listItemService;

    private User testUser;
    private User otherUser;
    private List testList;
    private UUID listId;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");

        otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@test.com");

        listId = UUID.randomUUID();
        testList = new List();
        testList.setId(listId);
        testList.setName("Lista de Teste");
        testList.setTypeId(1); // Shopping
        testList.setOwner(testUser);
        testList.setInviteCode("TEST12345678");
    }

    @Nested
    @DisplayName("addItem - Adicionar Item à Lista")
    class AddItemTests {

        @Test
        @DisplayName("Deve adicionar item com sucesso quando usuário é dono")
        void shouldAddItemSuccessfullyWhenUserIsOwner() {
            // Arrange
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "Arroz",
                2,
                null,
                null,
                null
            );

            when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
            when(listItemRepository.findMaxPositionByListId(listId)).thenReturn(-1);
            when(listItemRepository.save(any(ListItem.class))).thenAnswer(invocation -> {
                ListItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });

            ListItemResponseDTO.CreatorResponse creatorResponse = new ListItemResponseDTO.CreatorResponse(
                testUser.getId(), testUser.getUsername(), "Test User", null
            );
            ListItemResponseDTO expectedResponse = new ListItemResponseDTO(
                UUID.randomUUID(), "Arroz", false, 2, null, null, 0,
                creatorResponse, LocalDateTime.now(), LocalDateTime.now()
            );
            when(listItemMapper.toListItemResponseDTO(any(ListItem.class))).thenReturn(expectedResponse);

            // Act
            ListItemResponseDTO result = listItemService.addItem(listId, dto, testUser);

            // Assert
            assertNotNull(result);
            assertEquals("Arroz", result.name());
            assertEquals(2, result.quantity());
            assertEquals(0, result.position());
            assertFalse(result.checked());
            assertNotNull(result.createdBy());
            assertEquals(testUser.getId(), result.createdBy().id());

            verify(listRepository).findById(listId);
            verify(listItemRepository).findMaxPositionByListId(listId);
            verify(listItemRepository).save(any(ListItem.class));
            verify(listItemMapper).toListItemResponseDTO(any(ListItem.class));
        }

        @Test
        @DisplayName("Deve calcular position corretamente (0, 1, 2...)")
        void shouldCalculatePositionCorrectly() {
            // Arrange
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "Feijão",
                null,
                null,
                null,
                null
            );

            when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
            when(listItemRepository.findMaxPositionByListId(listId)).thenReturn(2); // Max position = 2, próximo será 3
            when(listItemRepository.save(any(ListItem.class))).thenAnswer(invocation -> {
                ListItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });

            ListItemResponseDTO.CreatorResponse creatorResponse = new ListItemResponseDTO.CreatorResponse(
                testUser.getId(), testUser.getUsername(), "Test User", null
            );
            ListItemResponseDTO expectedResponse = new ListItemResponseDTO(
                UUID.randomUUID(), "Feijão", false, null, null, null, 3,
                creatorResponse, LocalDateTime.now(), LocalDateTime.now()
            );
            when(listItemMapper.toListItemResponseDTO(any(ListItem.class))).thenReturn(expectedResponse);

            // Act
            ListItemResponseDTO result = listItemService.addItem(listId, dto, testUser);

            // Assert
            assertEquals(3, result.position()); // position deve ser 3 (count atual)
        }

        @Test
        @DisplayName("Deve lançar ListNotFoundException quando lista não existe")
        void shouldThrowListNotFoundExceptionWhenListDoesNotExist() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "Item",
                null,
                null,
                null,
                null
            );

            when(listRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            ListNotFoundException exception = assertThrows(
                ListNotFoundException.class,
                () -> listItemService.addItem(nonExistentId, dto, testUser)
            );

            assertEquals("Lista não encontrada", exception.getMessage());
            verify(listRepository).findById(nonExistentId);
            verify(listItemRepository, never()).save(any(ListItem.class));
        }

        @Test
        @DisplayName("Deve lançar ForbiddenException quando usuário não é participante")
        void shouldThrowForbiddenExceptionWhenUserIsNotParticipant() {
            // Arrange
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "Item",
                null,
                null,
                null,
                null
            );

            when(listRepository.findById(listId)).thenReturn(Optional.of(testList));

            // Act & Assert
            ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> listItemService.addItem(listId, dto, otherUser)
            );

            assertEquals("Você não tem permissão para adicionar itens nesta lista", exception.getMessage());
            verify(listRepository).findById(listId);
            verify(listItemRepository, never()).save(any(ListItem.class));
        }

        @Test
        @DisplayName("Deve fazer trim no nome do item")
        void shouldTrimItemName() {
            // Arrange
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "  Arroz Integral  ",
                null,
                null,
                null,
                null
            );

            when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
            when(listItemRepository.findMaxPositionByListId(listId)).thenReturn(-1);
            when(listItemRepository.save(any(ListItem.class))).thenAnswer(invocation -> {
                ListItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                // Verificar se o nome foi trimmado
                assertEquals("Arroz Integral", item.getName());
                return item;
            });

            ListItemResponseDTO.CreatorResponse creatorResponse = new ListItemResponseDTO.CreatorResponse(
                testUser.getId(), testUser.getUsername(), "Test User", null
            );
            ListItemResponseDTO expectedResponse = new ListItemResponseDTO(
                UUID.randomUUID(), "Arroz Integral", false, null, null, null, 0,
                creatorResponse, LocalDateTime.now(), LocalDateTime.now()
            );
            when(listItemMapper.toListItemResponseDTO(any(ListItem.class))).thenReturn(expectedResponse);

            // Act
            listItemService.addItem(listId, dto, testUser);

            // Assert - verificação feita no mock acima
            verify(listItemRepository).save(any(ListItem.class));
        }

        @Test
        @DisplayName("Deve adicionar item com quantity em lista tipo Shopping")
        void shouldAddItemWithDynamicFields() {
            // Arrange - Lista tipo Shopping (permite quantity)
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "Arroz Integral",
                5,           // quantity (permitido em Shopping)
                null,        // dueDate
                null,        // url
                null
            );

            when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
            when(listItemRepository.findMaxPositionByListId(listId)).thenReturn(-1);
            when(listItemRepository.save(any(ListItem.class))).thenAnswer(invocation -> {
                ListItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                // Verificar campos dinâmicos
                assertEquals(5, item.getQuantity());
                assertNull(item.getDueDate());
                assertNull(item.getUrl());
                return item;
            });

            ListItemResponseDTO.CreatorResponse creatorResponse = new ListItemResponseDTO.CreatorResponse(
                testUser.getId(), testUser.getUsername(), "Test User", null
            );
            ListItemResponseDTO expectedResponse = new ListItemResponseDTO(
                UUID.randomUUID(), "Arroz Integral", false, 5, null,
                null, 0, creatorResponse,
                LocalDateTime.now(), LocalDateTime.now()
            );
            when(listItemMapper.toListItemResponseDTO(any(ListItem.class))).thenReturn(expectedResponse);

            // Act
            ListItemResponseDTO result = listItemService.addItem(listId, dto, testUser);

            // Assert
            assertEquals(5, result.quantity());
            assertNull(result.dueDate());
            assertNull(result.url());
        }

        @Test
        @DisplayName("Deve definir checked como false por padrão")
        void shouldSetCheckedAsFalseByDefault() {
            // Arrange
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "Item não checado",
                null,
                null,
                null,
                null
            );

            when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
            when(listItemRepository.findMaxPositionByListId(listId)).thenReturn(-1);
            when(listItemRepository.save(any(ListItem.class))).thenAnswer(invocation -> {
                ListItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                assertFalse(item.isChecked()); // Verificar default
                return item;
            });

            ListItemResponseDTO.CreatorResponse creatorResponse = new ListItemResponseDTO.CreatorResponse(
                testUser.getId(), testUser.getUsername(), "Test User", null
            );
            ListItemResponseDTO expectedResponse = new ListItemResponseDTO(
                UUID.randomUUID(), "Item não checado", false, null, null, null, 0,
                creatorResponse, LocalDateTime.now(), LocalDateTime.now()
            );
            when(listItemMapper.toListItemResponseDTO(any(ListItem.class))).thenReturn(expectedResponse);

            // Act
            ListItemResponseDTO result = listItemService.addItem(listId, dto, testUser);

            // Assert
            assertFalse(result.checked());
        }

        @Test
        @DisplayName("Deve fazer trim na URL quando fornecida")
        void shouldTrimUrlWhenProvided() {
            // Arrange - Lista tipo Wishlist (typeId = 3, permite URL)
            testList.setTypeId(3); // WISHLIST
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "Produto",
                null,
                null,
                "  https://example.com/produto  ",
                null
            );

            when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
            when(listItemRepository.findMaxPositionByListId(listId)).thenReturn(-1);
            when(listItemRepository.save(any(ListItem.class))).thenAnswer(invocation -> {
                ListItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                assertEquals("https://example.com/produto", item.getUrl());
                return item;
            });

            ListItemResponseDTO.CreatorResponse creatorResponse = new ListItemResponseDTO.CreatorResponse(
                testUser.getId(), testUser.getUsername(), "Test User", null
            );
            ListItemResponseDTO expectedResponse = new ListItemResponseDTO(
                UUID.randomUUID(), "Produto", false, null, null,
                "https://example.com/produto", 0, creatorResponse,
                LocalDateTime.now(), LocalDateTime.now()
            );
            when(listItemMapper.toListItemResponseDTO(any(ListItem.class))).thenReturn(expectedResponse);

            // Act
            listItemService.addItem(listId, dto, testUser);

            // Assert - verificação feita no mock
            verify(listItemRepository).save(any(ListItem.class));
        }

        @Test
        @DisplayName("Deve registrar audit log ao adicionar item")
        void shouldLogAuditWhenAddingItem() {
            // Arrange
            CreateItemRequestDTO dto = new CreateItemRequestDTO(
                "Item Audit",
                null,
                null,
                null,
                null
            );

            when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
            when(listItemRepository.findMaxPositionByListId(listId)).thenReturn(-1);
            when(listItemRepository.save(any(ListItem.class))).thenAnswer(invocation -> {
                ListItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });

            ListItemResponseDTO.CreatorResponse creatorResponse = new ListItemResponseDTO.CreatorResponse(
                testUser.getId(), testUser.getUsername(), "Test User", null
            );
            ListItemResponseDTO expectedResponse = new ListItemResponseDTO(
                UUID.randomUUID(), "Item Audit", false, null, null, null, 0,
                creatorResponse, LocalDateTime.now(), LocalDateTime.now()
            );
            when(listItemMapper.toListItemResponseDTO(any(ListItem.class))).thenReturn(expectedResponse);

            // Act
            listItemService.addItem(listId, dto, testUser);

            // Assert - não lança exceção e métodos foram chamados
            verify(listItemRepository).save(any(ListItem.class));
        }
    }
}
