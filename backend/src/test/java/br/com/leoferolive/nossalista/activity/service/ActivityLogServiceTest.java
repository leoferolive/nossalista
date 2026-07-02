package br.com.leoferolive.nossalista.activity.service;

import br.com.leoferolive.nossalista.activity.repository.ActivityLogRepository;
import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.service.ListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link ActivityLogService}, com foco na checagem de
 * autorização de {@link #getActivities} — a tool MCP {@code get_list_activity}
 * e o endpoint REST equivalente dependem exclusivamente dela para recusar
 * acesso de quem não é dono nem membro da lista.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityLogService")
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ListService listService;

    @InjectMocks
    private ActivityLogService activityLogService;

    private UUID listId;
    private UUID strangerId;

    @BeforeEach
    void setUp() {
        listId = UUID.randomUUID();
        strangerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getActivities lança ForbiddenException quando o usuário não é dono nem membro da lista")
    void getActivitiesThrowsForbiddenForNonMember() {
        when(listService.getListById(listId, strangerId))
            .thenThrow(new ForbiddenException("Você não tem permissão para acessar esta lista"));

        assertThatThrownBy(() -> activityLogService.getActivities(listId, strangerId, 0, 50))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("permissão");

        verify(activityLogRepository, never()).findByList_IdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("getActivities lança ListNotFoundException quando a lista não existe, sem consultar o repositório de atividades")
    void getActivitiesThrowsListNotFoundWhenListDoesNotExist() {
        when(listService.getListById(listId, strangerId))
            .thenThrow(new ListNotFoundException("Lista não encontrada"));

        assertThatThrownBy(() -> activityLogService.getActivities(listId, strangerId, 0, 50))
            .isInstanceOf(ListNotFoundException.class);

        verify(activityLogRepository, never()).findByList_IdOrderByCreatedAtDesc(any(), any());
    }
}
