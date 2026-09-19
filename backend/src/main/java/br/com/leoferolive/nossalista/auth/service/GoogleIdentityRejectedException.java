package br.com.leoferolive.nossalista.auth.service;

/**
 * Generic rejection for an unsafe or unbindable Google callback.
 *
 * <p>The message intentionally carries no account or matching information so
 * callback integration can expose one generic error to the SPA.</p>
 */
public class GoogleIdentityRejectedException extends RuntimeException {

    public GoogleIdentityRejectedException() {
        super("Google authentication could not be completed");
    }
}
