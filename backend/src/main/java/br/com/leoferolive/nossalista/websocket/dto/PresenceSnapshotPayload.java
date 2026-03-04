package br.com.leoferolive.nossalista.websocket.dto;

import java.util.List;

public record PresenceSnapshotPayload(List<MemberOnlinePayload> members) {
}
