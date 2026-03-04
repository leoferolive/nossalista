package br.com.leoferolive.nossalista.websocket.dto;

import java.util.List;

public record ListLayoutUpdatedPayload(List<ItemPositionPayload> positions) {

    public record ItemPositionPayload(String itemId, int position) {
    }
}
