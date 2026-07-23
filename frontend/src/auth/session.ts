const LEGACY_AUTH_TOKEN_KEY = 'authToken'
const LEGACY_USER_KEY = 'user'

/**
 * Remove artefatos da arquitetura anterior. A sessão atual existe somente no
 * cookie HttpOnly e, por isso, nunca é lida ou gravada pelo JavaScript.
 */
export function clearLegacyAuthStorage(): void {
  localStorage.removeItem(LEGACY_AUTH_TOKEN_KEY)
  localStorage.removeItem(LEGACY_USER_KEY)
}
