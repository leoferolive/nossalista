package br.com.leoferolive.nossalista.push;

public record PushNotificationPayload(
    String title,
    String body,
    String icon,
    String tag,
    String url
) {}
