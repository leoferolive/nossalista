import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { authApi } from '../api/authApi'
import client, { preserveSessionOnUnauthorizedConfig } from '../api/client'

interface ConsumeMagicLinkDeps {
  guardKey: string
  login: ReturnType<typeof useAuth>['login']
  navigate: ReturnType<typeof useNavigate>
  setError: (message: string) => void
}

interface CurrentUserResponse {
  id: string
  username: string
  email: string
  name: string | null
  avatarUrl?: string | null
  onboardingCompletedAt?: string | null
}

function toAuthUser(data: CurrentUserResponse) {
  return {
    id: data.id,
    username: data.username,
    email: data.email,
    displayName: data.name,
    avatarUrl: data.avatarUrl ?? undefined,
    onboardingCompletedAt: data.onboardingCompletedAt ?? null,
  }
}

async function restoreExistingSession({
  login,
  navigate,
}: Pick<ConsumeMagicLinkDeps, 'login' | 'navigate'>): Promise<boolean> {
  try {
    const { data } = await client.get<CurrentUserResponse>(
      '/api/users/me',
      preserveSessionOnUnauthorizedConfig
    )
    login(toAuthUser(data))
    navigate('/home', { replace: true })
    return true
  } catch {
    return false
  }
}

async function consumeMagicLinkToken(
  token: string,
  { guardKey, login, navigate, setError }: ConsumeMagicLinkDeps
) {
  try {
    const data = await authApi.magicLogin(token)
    login(toAuthUser(data))
    navigate('/home', { replace: true })
  } catch (err) {
    if (await restoreExistingSession({ login, navigate })) {
      return
    }

    sessionStorage.removeItem(guardKey)
    setError(err instanceof Error ? err.message : 'Não foi possível entrar com o link mágico.')
  }
}

function MagicLoginError({ message, onBack }: { message: string; onBack: () => void }) {
  return (
    <div className="nl-page flex items-center justify-center p-4">
      <div className="nl-card w-full max-w-md p-6 text-center">
        <h1 className="mb-2 font-display text-xl font-bold text-nl-text">Falha no Login</h1>
        <p className="mb-4 text-nl-muted">{message}</p>
        <button type="button" onClick={onBack} className="nl-btn-primary w-full">
          Voltar para o início
        </button>
      </div>
    </div>
  )
}

function MagicLoginPending() {
  return (
    <div className="nl-page flex items-center justify-center p-4">
      <div className="nl-card w-full max-w-md p-6 text-center">
        <h1 className="mb-2 font-display text-xl font-bold text-nl-text">Entrando…</h1>
        <p className="text-nl-muted">Validando seu link de acesso.</p>
      </div>
    </div>
  )
}

export function MagicLogin() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { login } = useAuth()
  const [error, setError] = useState('')
  const hasProcessedRef = useRef(false)

  useEffect(() => {
    if (hasProcessedRef.current) {
      return
    }
    hasProcessedRef.current = true

    const token = searchParams.get('token')
    if (!token) {
      setError('Link inválido: token não encontrado.')
      return
    }

    // Idempotência: cada magic link é single-use. Um reload/revisita da mesma URL
    // no mesmo tab não pode reconsumir o token (o backend retornaria 400 e o
    // usuário — já autenticado neste navegador — veria um falso "Falha no Login").
    // O guard sobrevive a remontagem/reload; o hasProcessedRef cobre um único mount.
    const guardKey = `magic_login:${token}`
    if (sessionStorage.getItem(guardKey)) {
      void restoreExistingSession({ login, navigate }).then((restored) => {
        if (!restored) {
          sessionStorage.removeItem(guardKey)
          setError('Não foi possível restaurar sua sessão. Tente o link novamente.')
        }
      })
      return
    }
    sessionStorage.setItem(guardKey, '1')

    void consumeMagicLinkToken(token, { guardKey, login, navigate, setError })
  }, [searchParams, login, navigate])

  if (error) {
    return <MagicLoginError message={error} onBack={() => navigate('/', { replace: true })} />
  }

  return <MagicLoginPending />
}
