# Authentication hardening

## Task 1: Additive schema

Create V19 with external Google identities, `users.session_version INT NOT NULL DEFAULT 0`, and additive OAuth handoff-code columns. The new identity table has `provider`, `issuer`, `subject`, `user_id`, `provider_email`, `provider_email_verified`, timestamps, unique `(provider, issuer, subject)`, and one Google identity per user/issuer. OAuth rows gain nullable `code_hash`, `user_id` FK, and `consumed_at`; preserve `code` and `jwt` for rolling compatibility. Add migration tests.

## Task 2: Google identity binding

Authenticate Google users by `(provider, issuer, subject)`, where issuer is the configured canonical `https://accounts.google.com`. Require a nonblank `sub` and `email_verified=true`; never use e-mail as an identity key. For a known identity, update display data and last use. For an unbound legacy `GOOGLE` user only, bind once by matching verified e-mail until `2026-12-18T03:00:00Z`. Reject an `EMAIL` account e-mail collision without a session. New Google users get a new identity and an e-mail verification status equal to the provider claim. Redirect rejected provider callbacks to the existing SPA callback with a generic error code; do not expose account existence.

This parallel task owns the identity domain, repository/service and focused tests only. Shared callback/controller integration is owned by Task 5 to keep parallel branches non-overlapping.

## Task 3: Atomic OAuth handoff

Use a 256-bit Base64url code and persist only `SHA-256(code)`, `user_id`, expiration and `consumed_at` for new rows. Atomically claim an unexpired, unconsumed code and issue a fresh JWT from the user after winning; no new JWT is stored in the database. During rolling compatibility, issue/read both legacy and new forms; retain the public `POST /api/auth/oauth/exchange { code }` request and response. Remove `OAUTHDBG` and never log a code, token or hash. Add real concurrent redemption coverage that permits exactly one successful exchange.

This parallel task owns store/repository/exchange services and focused tests only. Shared success-handler and controller integration is owned by Task 5.

## Task 4: Session lifecycle and OAuth request integrity

Add integer JWT claim `sv` and require it to equal `users.session_version` on every cookie-authenticated request; use a direct version lookup so the 60-second user cache cannot delay revocation. Login, Google exchange and magic login issue current versions. Authenticated logout and successful password reset increment the version and clear the local cookie, invalidating every browser session. PATs and MCP OAuth tokens remain unaffected.

Replace the OAuth authorization-request cookie with a bounded `base64url(payload).base64url(HMAC-SHA-256)` envelope. Production uses `__Host-nl_oauth2_request`; dev/test use `nl_oauth2_request`; it is HttpOnly, profile-controlled Secure, SameSite=Lax, Path=/ and lasts 180 seconds. Verify HMAC in constant time and expiry before deserializing, clear invalid values and fail closed. Use required `OAUTH2_REQUEST_SIGNING_KEY` (32+ bytes, distinct from `JWT_SECRET`) plus optional previous key accepted for only 180 seconds. Add `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, restrictive permissions policy, minimal CSP (`base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'`), and no-store caching for auth/OAuth redirect responses.

This parallel task owns session-version primitives, JWT/filter support, signed authorization-request repository and focused tests. Shared auth controller integration is owned by Task 5.

## Task 5: Integration and documentation

Resolve cross-task integration, run complete quality gates, add V20 only after the V19 compatibility release has been live with no old pods for more than 60 seconds, and update README, ENVIRONMENT, auth endpoint matrix and decisions. Do not make V20 part of this branch because its safety depends on the completed production rollout.
