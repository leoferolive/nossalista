package br.com.leoferolive.nossalista.mcp.tool;

import br.com.leoferolive.nossalista.common.exception.InvalidInputException;
import br.com.leoferolive.nossalista.list.domain.SharedList;
import br.com.leoferolive.nossalista.list.service.ListService;
import br.com.leoferolive.nossalista.member.dto.InviteByUsernameResponse;
import br.com.leoferolive.nossalista.member.dto.ListMemberResponse;
import br.com.leoferolive.nossalista.member.service.MemberService;
import br.com.leoferolive.nossalista.mcp.dto.ListMembersResult;
import br.com.leoferolive.nossalista.mcp.dto.MemberSummary;
import br.com.leoferolive.nossalista.mcp.dto.RemoveMemberResult;
import br.com.leoferolive.nossalista.mcp.dto.ShareListResult;
import br.com.leoferolive.nossalista.mcp.security.McpSecurityContext;
import br.com.leoferolive.nossalista.mcp.support.McpIds;
import br.com.leoferolive.nossalista.user.domain.User;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Tools MCP para compartilhamento e gestão de membros de uma lista. Reusa
 * {@link ListService#generateInviteLink} e {@link MemberService} integralmente
 * — nenhuma regra de owner-only ou de auto-remoção é duplicada aqui.
 */
@Component
public class MemberMcpTools {

    private final ListService listService;
    private final MemberService memberService;
    private final McpSecurityContext security;

    @Value("${frontend.url}")
    private String frontendBaseUrl;

    public MemberMcpTools(ListService listService, MemberService memberService, McpSecurityContext security) {
        this.listService = listService;
        this.memberService = memberService;
        this.security = security;
    }

    @McpTool(
        name = "share_list",
        description = "Shares a list with another user. mode=\"username\" invites a specific user by "
            + "username (owner only). mode=\"link\" generates (or reuses, if still valid) an invite "
            + "link that expires in 24 hours and can be shared with anyone (owner only).",
        generateOutputSchema = true
    )
    public ShareListResult shareList(
        @McpToolParam(description = "UUID of the list to share.")
        String listId,
        @McpToolParam(description = "Sharing mode: \"username\" or \"link\".")
        String mode,
        @McpToolParam(required = false, description = "Username to invite. Required when mode=\"username\".")
        String username
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();

        UUID id = McpIds.parseUuid(listId, "listId");
        if ("username".equalsIgnoreCase(mode)) {
            return shareByUsername(id, username, user);
        }
        if ("link".equalsIgnoreCase(mode)) {
            return shareByLink(id, user);
        }
        throw new InvalidInputException("mode inválido: \"" + mode + "\". Use \"username\" ou \"link\"");
    }

    @McpTool(
        name = "list_members",
        description = "Lists the members of a list, including the owner, with their role and join date.",
        generateOutputSchema = true
    )
    public ListMembersResult listMembers(
        @McpToolParam(description = "UUID of the list.")
        String listId
    ) {
        User user = security.currentUser();
        UUID id = McpIds.parseUuid(listId, "listId");

        List<MemberSummary> members = memberService.getMembers(id, user.getId()).stream()
            .map(this::toMemberSummary)
            .toList();
        return new ListMembersResult(members);
    }

    @McpTool(
        name = "remove_member",
        description = "Removes a member from a list. If the target user is the caller, this leaves "
            + "the list instead (owners cannot leave; they must delete or transfer the list first). "
            + "If the target is another user, only the list owner can remove them. Confirm with the "
            + "user before calling this tool when removing someone else.",
        generateOutputSchema = true
    )
    public RemoveMemberResult removeMember(
        @McpToolParam(description = "UUID of the list.")
        String listId,
        @McpToolParam(description = "UUID of the user to remove.")
        String userId
    ) {
        User user = security.currentUser();
        security.requireWriteAccess();

        UUID id = McpIds.parseUuid(listId, "listId");
        UUID targetUserId = McpIds.parseUuid(userId, "userId");

        if (targetUserId.equals(user.getId())) {
            memberService.leaveList(id, user.getId());
            return new RemoveMemberResult(targetUserId.toString(), "LEFT");
        }
        memberService.removeMember(id, user.getId(), targetUserId);
        return new RemoveMemberResult(targetUserId.toString(), "REMOVED");
    }

    private ShareListResult shareByUsername(UUID listId, String username, User user) {
        if (username == null || username.isBlank()) {
            throw new InvalidInputException("username é obrigatório quando mode=\"username\"");
        }
        InviteByUsernameResponse response = memberService.inviteByUsername(listId, user.getId(), username);
        return new ShareListResult("username", response.invitedUsername(), response.message(), null, null, null);
    }

    private ShareListResult shareByLink(UUID listId, User user) {
        SharedList list = listService.generateInviteLink(listId, user.getId());
        String inviteLink = frontendBaseUrl + "/join/" + list.getInviteCode();
        String expiresAt = list.getInviteExpiresAt() == null ? null : list.getInviteExpiresAt().toString();
        return new ShareListResult("link", null, "Invite link ready", list.getInviteCode(), inviteLink, expiresAt);
    }

    private MemberSummary toMemberSummary(ListMemberResponse response) {
        return new MemberSummary(
            response.user().id().toString(),
            response.user().username(),
            response.user().name(),
            response.role(),
            response.joinedAt() == null ? null : response.joinedAt().toString()
        );
    }
}
