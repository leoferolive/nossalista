package br.com.leoferolive.nossalista.mcp.support;

import br.com.leoferolive.nossalista.common.exception.InvalidInputException;
import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.list.service.ListService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolve qual lista uma tool MCP deve operar quando o modelo pode informar
 * {@code listId} OU {@code name} (tool {@code get_list}).
 *
 * <p>Reusa {@link ListService#getListById} (que já garante que o usuário é
 * owner/membro da lista) e {@link ListService#getAllListsForUser} (que já
 * restringe o universo de busca por nome às listas do próprio usuário) — não
 * duplica nenhuma regra de autorização do service.</p>
 */
@Component
public class ListNameResolver {

    private final ListService listService;

    public ListNameResolver(ListService listService) {
        this.listService = listService;
    }

    public SharedList resolve(String listId, String name, UUID currentUserId) {
        boolean hasId = listId != null && !listId.isBlank();
        boolean hasName = name != null && !name.isBlank();

        if (!hasId && !hasName) {
            throw new InvalidInputException("Provide listId or name to identify the list");
        }
        if (hasId) {
            return listService.getListById(McpIds.parseUuid(listId, "listId"), currentUserId);
        }
        return resolveByName(name.trim(), currentUserId);
    }

    private SharedList resolveByName(String name, UUID currentUserId) {
        List<SharedList> candidates = listService.getAllListsForUser(currentUserId);

        List<SharedList> exactMatches = candidates.stream()
            .filter(list -> list.getName().equalsIgnoreCase(name))
            .toList();
        if (exactMatches.size() == 1) {
            return exactMatches.get(0);
        }
        if (exactMatches.size() > 1) {
            throw ambiguous(name, exactMatches);
        }

        List<SharedList> partialMatches = candidates.stream()
            .filter(list -> list.getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
            .toList();
        if (partialMatches.isEmpty()) {
            throw new ListNotFoundException("No list found with name \"" + name + "\"");
        }
        if (partialMatches.size() > 1) {
            throw ambiguous(name, partialMatches);
        }
        return partialMatches.get(0);
    }

    private InvalidInputException ambiguous(String name, List<SharedList> matches) {
        String candidateList = matches.stream()
            .map(list -> list.getId() + " (\"" + list.getName() + "\")")
            .collect(Collectors.joining(", "));
        return new InvalidInputException(
            "More than one list matches \"" + name + "\". Use listId to disambiguate. Candidates: "
                + candidateList);
    }
}
