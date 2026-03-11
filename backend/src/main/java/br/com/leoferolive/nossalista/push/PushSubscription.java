package br.com.leoferolive.nossalista.push;

public record PushSubscription(
    String endpoint,
    String p256dh,
    String auth
) {}
