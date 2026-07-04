import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { authApi } from '../api/authApi'
import { persistAuthToken } from '../auth/session'

interface ConsumeMagicLinkDeps {
  login: ReturnType<typeof useAuth>['login']
  navigate: ReturnType<typeof useNavigate>
  setError: (message: string) => void
}

async function consumeMagicLinkToken(
  token: string,
  { login, navigate, setError }: ConsumeMagicLinkDeps
) {
  try {
    const data = await authApi.magicLogin(token)
    persistAuthToken(data.token)
    login(data.token, {
      id: data.id,
      username: data.username,
      email: data.email,
      displayName: data.name,
      avatarUrl: data.avatarUrl ?? undefined,
      onboardingCompletedAt: data.onboardingCompletedAt ?? null,
    })
    navigate('/home', { replace: true })
  } catch (err) {
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

    void consumeMagicLinkToken(token, { login, navigate, setError })
  }, [searchParams, login, navigate])

  if (error) {
    return <MagicLoginError message={error} onBack={() => navigate('/', { replace: true })} />
  }

  return <MagicLoginPending />
}
