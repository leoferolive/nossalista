import axios, { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { clearLegacyAuthStorage } from '../auth/session'

const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_ENDPOINT = '/api/auth/csrf'
const UNSAFE_METHODS = new Set(['post', 'put', 'patch', 'delete'])
let csrfRequest: Promise<void> | null = null

export const preserveSessionOnUnauthorizedConfig: AxiosRequestConfig = {
  preserveSessionOnUnauthorized: true,
}

/**
 * Instância configurada para enviar a sessão HttpOnly automaticamente. O
 * token CSRF não autentica o usuário; ele apenas acompanha mutações da SPA.
 */
const client: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 10000,
  withCredentials: true,
  xsrfCookieName: CSRF_COOKIE_NAME,
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: {
    'Content-Type': 'application/json',
  },
})

function hasCsrfCookie(): boolean {
  return document.cookie
    .split(';')
    .some((cookie) => cookie.trim().startsWith(`${CSRF_COOKIE_NAME}=`))
}

async function ensureCsrfToken(): Promise<void> {
  if (typeof document === 'undefined' || hasCsrfCookie()) {
    return
  }

  if (!csrfRequest) {
    csrfRequest = client
      .get(CSRF_ENDPOINT, preserveSessionOnUnauthorizedConfig)
      .then(() => undefined)
      .finally(() => {
        csrfRequest = null
      })
  }

  await csrfRequest
}

client.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const method = config.method?.toLowerCase()
    if (method && UNSAFE_METHODS.has(method) && config.url !== CSRF_ENDPOINT) {
      await ensureCsrfToken()
    }

    return config
  },
  (error) => Promise.reject(error)
)

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.preserveSessionOnUnauthorized) {
      clearLegacyAuthStorage()

      if (window.location.pathname !== '/') {
        window.location.href = '/'
      }
    }

    return Promise.reject(error)
  }
)

export default client
