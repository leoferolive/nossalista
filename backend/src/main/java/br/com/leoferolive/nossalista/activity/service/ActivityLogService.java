package br.com.leoferolive.nossalista.activity.service;

import br.com.leoferolive.nossalista.activity.domain.ActivityLogEntry;
import br.com.leoferolive.nossalista.activity.dto.ActivityLogResponse;
import br.com.leoferolive.nossalista.activity.dto.ActivityPageResponse;
import br.com.leoferolive.nossalista.activity.repository.ActivityLogRepository;
import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import br.com.leoferolive.nossalista.user.domain.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ActivityLogService {

    private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final ActivityLogRepository activityLogRepository;
    private final ListService listService;

    public ActivityLogService(
        ActivityLogRepository activityLogRepository,
        ListService listService
    ) {
        this.activityLogRepository = activityLogRepository;
        this.listService = listService;
    }

    @Transactional
    public void logItemAdded(List list, User actor, ListItem item) {
        save(list, actor, "ITEM_ADDED", "ITEM", item.getId(), item.getName(), null);
    }

    @Transactional
    public void logItemChecked(List list, User actor, ListItem item) {
        save(list, actor, "ITEM_CHECKED", "ITEM", item.getId(), item.getName(), null);
    }

    @Transactional
    public void logItemUnchecked(List list, User actor, ListItem item) {
        save(list, actor, "ITEM_UNCHECKED", "ITEM", item.getId(), item.getName(), null);
    }

    @Transactional
    public void logItemUpdated(List list, User actor, ListItem item, java.util.List<String> changedFields) {
        save(list, actor, "ITEM_UPDATED", "ITEM", item.getId(), item.getName(), Map.of("changedFields", changedFields));
    }

    @Transactional
    public void logItemRemoved(List list, User actor, ListItem item) {
        save(list, actor, "ITEM_REMOVED", "ITEM", item.getId(), item.getName(), null);
    }

    @Transactional
    public void logMemberJoinedViaLink(List list, User member) {
        save(list, member, "MEMBER_JOINED", "MEMBER", member.getId(), displayName(member), Map.of("method", "LINK"));
    }

    @Transactional
    public void logMemberInvitedByUsername(List list, User member, User invitedBy) {
        save(
            list,
            member,
            "MEMBER_JOINED",
            "MEMBER",
            member.getId(),
            displayName(member),
            Map.of("method", "USERNAME", "invitedBy", invitedBy.getUsername())
        );
    }

    @Transactional
    public void logMemberLeft(List list, User member) {
        save(list, member, "MEMBER_LEFT", "MEMBER", member.getId(), displayName(member), null);
    }

    @Transactional
    public void logMemberRemoved(List list, User member, User removedBy) {
        save(
            list,
            member,
            "MEMBER_REMOVED",
            "MEMBER",
            member.getId(),
            displayName(member),
            Map.of("removedBy", displayName(removedBy), "wasKicked", true)
        );
    }

    @Transactional(readOnly = true)
    public ActivityPageResponse getActivities(UUID listId, UUID currentUserId, int page, int size) {
        listService.getListById(listId, currentUserId);

        var resultPage = activityLogRepository
            .findByList_IdOrderByCreatedAtDesc(listId, PageRequest.of(page, size))
            .map(this::toResponse);

        return ActivityPageResponse.from(resultPage);
    }

    private void save(
        List list,
        User actor,
        String action,
        String targetType,
        UUID targetId,
        String targetName,
        Map<String, Object> details
    ) {
        ActivityLogEntry entry = new ActivityLogEntry();
        entry.setList(list);
        entry.setUser(actor);
        entry.setUserName(displayName(actor));
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setTargetName(targetName);
        entry.setDetails(toJson(details));
        activityLogRepository.save(entry);
    }

    private ActivityLogResponse toResponse(ActivityLogEntry entry) {
        return new ActivityLogResponse(
            entry.getId(),
            entry.getList().getId(),
            entry.getUser().getId(),
            entry.getUserName(),
            entry.getAction(),
            entry.getTargetType(),
            entry.getTargetId(),
            entry.getTargetName(),
            fromJson(entry.getDetails()),
            entry.getCreatedAt()
        );
    }

    private String displayName(User user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return user.getUsername();
    }

    private String toJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar detalhes da atividade", ex);
        }
    }

    private Map<String, Object> fromJson(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(details, DETAILS_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao desserializar detalhes da atividade", ex);
        }
    }
}
