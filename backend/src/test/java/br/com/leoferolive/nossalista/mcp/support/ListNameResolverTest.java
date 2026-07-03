package br.com.leoferolive.nossalista.mcp.support;

import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.common.exception.InvalidInputException;
import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.service.ListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListNameResolver")
class ListNameResolverTest {

    @Mock
    private ListService listService;

    private ListNameResolver resolver;
    private UUID userId;

    @BeforeEach
    void setUp() {
        resolver = new ListNameResolver(listService);
        userId = UUID.randomUUID();
    }

    private SharedList listNamed(String name) {
        SharedList list = new SharedList();
        list.setId(UUID.randomUUID());
        list.setName(name);
        return list;
    }

    @Test
    @DisplayName("resolve por listId delega diretamente para ListService.getListById")
    void resolvesByIdWhenProvided() {
        SharedList list = listNamed("Compras");
        when(listService.getListById(list.getId(), userId)).thenReturn(list);

        SharedList result = resolver.resolve(list.getId().toString(), null, userId);

        assertThat(result).isSameAs(list);
    }

    @Test
    @DisplayName("resolve por listId inválido lança InvalidInputException, não deixa a exceção técnica escapar")
    void invalidListIdRaisesActionableException() {
        assertThatThrownBy(() -> resolver.resolve("not-a-uuid", null, userId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessageContaining("listId");
    }

    @Test
    @DisplayName("resolve por nome faz match exato, case-insensitive, com prioridade sobre contains")
    void resolvesByExactNameCaseInsensitive() {
        SharedList exact = listNamed("Compras da Semana");
        SharedList other = listNamed("Compras da Semana Passada");
        lenient().when(listService.getAllListsForUser(userId)).thenReturn(List.of(exact, other));

        SharedList result = resolver.resolve(null, "compras da semana", userId);

        assertThat(result).isSameAs(exact);
    }

    @Test
    @DisplayName("resolve por nome cai para contains quando não há match exato")
    void resolvesByContainsWhenNoExactMatch() {
        SharedList list = listNamed("Lista de Compras do Mês");
        lenient().when(listService.getAllListsForUser(userId)).thenReturn(List.of(list));

        SharedList result = resolver.resolve(null, "compras", userId);

        assertThat(result).isSameAs(list);
    }

    @Test
    @DisplayName("resolve por nome ambíguo (múltiplos contains) lança InvalidInputException listando candidatos")
    void ambiguousNameListsCandidates() {
        SharedList first = listNamed("Compras Casa");
        SharedList second = listNamed("Compras Trabalho");
        lenient().when(listService.getAllListsForUser(userId)).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> resolver.resolve(null, "compras", userId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessageContaining(first.getId().toString())
            .hasMessageContaining(second.getId().toString());
    }

    @Test
    @DisplayName("resolve por nome sem nenhum match lança ListNotFoundException")
    void noMatchRaisesListNotFound() {
        lenient().when(listService.getAllListsForUser(userId)).thenReturn(List.of(listNamed("Tarefas")));

        assertThatThrownBy(() -> resolver.resolve(null, "compras", userId))
            .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    @DisplayName("resolve sem listId nem name lança InvalidInputException")
    void missingBothIdentifiersRaisesException() {
        assertThatThrownBy(() -> resolver.resolve(null, null, userId))
            .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("resolve por id propaga ForbiddenException do ListService sem alterar o tipo")
    void propagatesForbiddenFromService() {
        UUID listId = UUID.randomUUID();
        when(listService.getListById(listId, userId)).thenThrow(new ForbiddenException("sem permissão"));

        assertThatThrownBy(() -> resolver.resolve(listId.toString(), null, userId))
            .isInstanceOf(ForbiddenException.class);
    }
}
