package br.com.leoferolive.nossalista.member.service;

import br.com.leoferolive.nossalista.activity.service.ActivityLogService;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.domain.ListTypeEntity;
import br.com.leoferolive.nossalista.list.dto.JoinListResponse;
import br.com.leoferolive.nossalista.list.exception.InviteExpiredException;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.listitem.repository.ListItemRepository;
import br.com.leoferolive.nossalista.member.domain.ListMember;
import br.com.leoferolive.nossalista.member.domain.MemberRole;
import br.com.leoferolive.nossalista.member.dto.ListJoinedResponse;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import br.com.leoferolive.nossalista.notification.NotificationService;
import br.com.leoferolive.nossalista.websocket.WebSocketEventPublisher;
import br.com.leoferolive.nossalista.websocket.dto.MemberJoinedPayload;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListJoinService Tests")
class ListJoinServiceTest {

    @Mock
    private ListRepository listRepository;

    @Mock
    private ListItemRepository listItemRepository;

    @Mock
    private ListMemberRepository listMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private WebSocketEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ListJoinService listJoinService;

    private List testList;
    private User testOwner;
    private User testUser;
    private ListTypeEntity testTypeEntity;
    private static final String VALID_INVITE_CODE = "ABC123XYZ789";
    private static final String INVALID_INVITE_CODE = "INVALIDCODE";

    @BeforeEach
    void setUp() {
        // Setup test owner
        testOwner = new User();
        testOwner.setId(UUID.randomUUID());
        testOwner.setUsername("mariana");
        testOwner.setName("Mariana Silva");
        testOwner.setAvatarUrl("https://example.com/avatar.jpg");

        // Setup test user (non-owner)
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("pedro");
        testUser.setName("Pedro Santos");
        testUser.setAvatarUrl("https://example.com/pedro.jpg");

        // Setup test type entity (mock since it's @Immutable)
        testTypeEntity = mock(ListTypeEntity.class);
        lenient().when(testTypeEntity.getId()).thenReturn(1);
        lenient().when(testTypeEntity.getName()).thenReturn("Compras");
        lenient().when(testTypeEntity.getSlug()).thenReturn("compras");

        // Setup test list
        testList = new List();
        testList.setId(UUID.randomUUID());
        testList.setName("Mercado Semanal");
        testList.setTypeId(1);
        testList.setTypeEntity(testTypeEntity);
        testList.setOwner(testOwner);
        testList.setInviteCode(VALID_INVITE_CODE);
        testList.setInviteExpiresAt(LocalDateTime.now().plusHours(24));
        testList.setCreatedAt(LocalDateTime.now());
        testList.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve retornar JoinListResponse quando invite code é válido")
    void shouldReturnJoinListResponseWhenInviteCodeValid() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(listItemRepository.findByListIdOrderByPositionAsc(testList.getId()))
            .thenReturn(java.util.List.of());

        // Act
        JoinListResponse response = listJoinService.getListByInviteCode(VALID_INVITE_CODE);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testList.getId());
        assertThat(response.name()).isEqualTo("Mercado Semanal");
        assertThat(response.mode()).isEqualTo("READ_ONLY");
    }

    @Test
    @DisplayName("Deve lançar ListNotFoundException quando invite code não existe")
    void shouldThrowListNotFoundWhenInviteCodeDoesNotExist() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(INVALID_INVITE_CODE))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listJoinService.getListByInviteCode(INVALID_INVITE_CODE))
            .isInstanceOf(ListNotFoundException.class)
            .hasMessageContaining("Convite não encontrado");
    }

    @Test
    @DisplayName("Deve lançar InviteExpiredException quando link está expirado")
    void shouldThrowInviteExpiredExceptionWhenLinkIsExpired() {
        // Arrange
        testList.setInviteExpiresAt(LocalDateTime.now().minusHours(1));
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));

        // Act & Assert
        assertThatThrownBy(() -> listJoinService.getListByInviteCode(VALID_INVITE_CODE))
            .isInstanceOf(InviteExpiredException.class)
            .hasMessageContaining("expirou");
    }

    @Test
    @DisplayName("Deve lançar InviteExpiredException quando expires_at é nulo")
    void shouldThrowInviteExpiredExceptionWhenExpiresAtIsNull() {
        // Arrange
        testList.setInviteExpiresAt(null);
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));

        // Act & Assert
        assertThatThrownBy(() -> listJoinService.getListByInviteCode(VALID_INVITE_CODE))
            .isInstanceOf(InviteExpiredException.class)
            .hasMessageContaining("expirou");
    }

    @Test
    @DisplayName("Deve retornar itens ordenados por position")
    void shouldReturnItemsOrderedByPosition() {
        // Arrange
        ListItem item1 = createListItem("Arroz", 2);
        ListItem item2 = createListItem("Feijão", 0);
        ListItem item3 = createListItem("Leite", 1);

        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(listItemRepository.findByListIdOrderByPositionAsc(testList.getId()))
            .thenReturn(java.util.List.of(item2, item3, item1)); // Já ordenados pelo repository

        // Act
        JoinListResponse response = listJoinService.getListByInviteCode(VALID_INVITE_CODE);

        // Assert
        assertThat(response.items()).hasSize(3);
        assertThat(response.items().get(0).position()).isEqualTo(0);
        assertThat(response.items().get(1).position()).isEqualTo(1);
        assertThat(response.items().get(2).position()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve retornar modo READ_ONLY")
    void shouldReturnReadOnlyMode() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(listItemRepository.findByListIdOrderByPositionAsc(testList.getId()))
            .thenReturn(java.util.List.of());

        // Act
        JoinListResponse response = listJoinService.getListByInviteCode(VALID_INVITE_CODE);

        // Assert
        assertThat(response.mode()).isEqualTo("READ_ONLY");
    }

    @Test
    @DisplayName("Deve mapear dados do dono corretamente")
    void shouldMapOwnerDataCorrectly() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(listItemRepository.findByListIdOrderByPositionAsc(testList.getId()))
            .thenReturn(java.util.List.of());

        // Act
        JoinListResponse response = listJoinService.getListByInviteCode(VALID_INVITE_CODE);

        // Assert
        assertThat(response.owner_username()).isEqualTo("mariana");
        assertThat(response.owner_name()).isEqualTo("Mariana Silva");
        assertThat(response.owner_avatar_url()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    @DisplayName("Deve mapear dados do tipo corretamente")
    void shouldMapTypeDataCorrectly() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(listItemRepository.findByListIdOrderByPositionAsc(testList.getId()))
            .thenReturn(java.util.List.of());

        // Act
        JoinListResponse response = listJoinService.getListByInviteCode(VALID_INVITE_CODE);

        // Assert
        assertThat(response.type_slug()).isEqualTo("compras");
        assertThat(response.type_name()).isEqualTo("Compras");
    }

    @Test
    @DisplayName("Deve mapear dados dos itens corretamente")
    void shouldMapItemDataCorrectly() {
        // Arrange
        ListItem item = createListItemWithAllFields("Arroz", 0, 2, false);

        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(listItemRepository.findByListIdOrderByPositionAsc(testList.getId()))
            .thenReturn(java.util.List.of(item));

        // Act
        JoinListResponse response = listJoinService.getListByInviteCode(VALID_INVITE_CODE);

        // Assert
        assertThat(response.items()).hasSize(1);
        JoinListResponse.JoinListItemResponse itemResponse = response.items().get(0);
        assertThat(itemResponse.name()).isEqualTo("Arroz");
        assertThat(itemResponse.quantity()).isEqualTo(2);
        assertThat(itemResponse.checked()).isFalse();
        assertThat(itemResponse.position()).isEqualTo(0);
    }

    private ListItem createListItem(String name, int position) {
        ListItem item = new ListItem();
        item.setId(UUID.randomUUID());
        item.setName(name);
        item.setPosition(position);
        item.setChecked(false);
        return item;
    }

    private ListItem createListItemWithAllFields(String name, int position, int quantity, boolean checked) {
        ListItem item = new ListItem();
        item.setId(UUID.randomUUID());
        item.setName(name);
        item.setPosition(position);
        item.setQuantity(quantity);
        item.setChecked(checked);
        return item;
    }

    // ==================== TESTES PARA joinList() ====================

    @Test
    @DisplayName("joinList: Deve criar membro quando usuário entra em nova lista")
    void shouldCreateMemberWhenUserJoinsNewList() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(listMemberRepository.findByListIdAndUserId(testList.getId(), testUser.getId()))
            .thenReturn(Optional.empty());
        when(listItemRepository.countByListId(testList.getId()))
            .thenReturn(0L);

        // Act
        ListJoinedResponse response = listJoinService.joinList(testUser.getId(), VALID_INVITE_CODE);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testList.getId());
        assertThat(response.name()).isEqualTo("Mercado Semanal");
        assertThat(response.role()).isEqualTo("MEMBER");
        assertThat(response.message()).isEqualTo("Bem-vindo à lista Mercado Semanal!");
        assertThat(response.type_slug()).isEqualTo("compras");
        assertThat(response.created()).isTrue();
        assertThat(response.list()).isNotNull();
        // Verificar que o membro foi de fato persistido
        verify(listMemberRepository).save(any(ListMember.class));
        verify(activityLogService).logMemberJoinedViaLink(testList, testUser);
    }

    @Test
    @DisplayName("joinList: Deve retornar OK quando usuário já é membro")
    void shouldReturnOkWhenUserAlreadyMember() {
        // Arrange
        ListMember existingMember = new ListMember();
        existingMember.setId(UUID.randomUUID());
        existingMember.setList(testList);
        existingMember.setUser(testUser);
        existingMember.setRole(MemberRole.MEMBER);

        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(listMemberRepository.findByListIdAndUserId(testList.getId(), testUser.getId()))
            .thenReturn(Optional.of(existingMember));
        when(listItemRepository.countByListId(testList.getId()))
            .thenReturn(0L);

        // Act
        ListJoinedResponse response = listJoinService.joinList(testUser.getId(), VALID_INVITE_CODE);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo("MEMBER");
        assertThat(response.message()).isEqualTo("Você já é membro desta lista");
    }

    @Test
    @DisplayName("joinList: Deve retornar OK quando usuário é o dono")
    void shouldReturnOkWhenUserIsOwner() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(userRepository.findById(testOwner.getId()))
            .thenReturn(Optional.of(testOwner));
        when(listItemRepository.countByListId(testList.getId()))
            .thenReturn(0L);

        // Act
        ListJoinedResponse response = listJoinService.joinList(testOwner.getId(), VALID_INVITE_CODE);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo("OWNER");
        assertThat(response.message()).isEqualTo("Você é o dono desta lista");
    }

    @Test
    @DisplayName("joinList: Deve lançar ListNotFoundException quando invite code não existe")
    void shouldThrowListNotFoundWhenJoiningWithInvalidCode() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(INVALID_INVITE_CODE))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listJoinService.joinList(testUser.getId(), INVALID_INVITE_CODE))
            .isInstanceOf(ListNotFoundException.class)
            .hasMessageContaining("Convite não encontrado");
    }

    @Test
    @DisplayName("joinList: Deve publicar evento MEMBER_JOINED via WebSocket ao entrar em lista nova")
    void shouldPublishMemberJoinedEventWhenNewMember() {
        // Arrange
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));
        when(userRepository.findById(testUser.getId()))
            .thenReturn(Optional.of(testUser));
        when(listMemberRepository.findByListIdAndUserId(testList.getId(), testUser.getId()))
            .thenReturn(Optional.empty());
        when(listItemRepository.countByListId(testList.getId()))
            .thenReturn(0L);

        // Act
        listJoinService.joinList(testUser.getId(), VALID_INVITE_CODE);

        // Assert
        ArgumentCaptor<MemberJoinedPayload> payloadCaptor =
            ArgumentCaptor.forClass(MemberJoinedPayload.class);
        verify(eventPublisher).publishItemsEvent(
            eq(testList.getId()), eq("MEMBER_JOINED"), payloadCaptor.capture(), eq(testUser), isNull()
        );

        MemberJoinedPayload payload = payloadCaptor.getValue();
        assertThat(payload.userId()).isEqualTo(testUser.getId().toString());
        assertThat(payload.username()).isEqualTo(testUser.getUsername());
        assertThat(payload.listId()).isEqualTo(testList.getId().toString());
        assertThat(payload.listName()).isEqualTo(testList.getName());
    }

    @Test
    @DisplayName("joinList: Deve lançar InviteExpiredException quando link está expirado")
    void shouldThrowInviteExpiredExceptionWhenLinkExpired() {
        // Arrange
        testList.setInviteExpiresAt(LocalDateTime.now().minusHours(1));
        when(listRepository.findByInviteCodeWithDetails(VALID_INVITE_CODE))
            .thenReturn(Optional.of(testList));

        // Act & Assert
        assertThatThrownBy(() -> listJoinService.joinList(testUser.getId(), VALID_INVITE_CODE))
            .isInstanceOf(InviteExpiredException.class)
            .hasMessageContaining("Link expirado");
    }
}
