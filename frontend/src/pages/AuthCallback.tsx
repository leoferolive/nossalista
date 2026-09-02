import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/authApi'
import client, { preserveSessionOnUnauthorizedConfig } from '../api/client'
import { useAuth } from '../contexts/AuthContext'
import { listsApi } from '../api/listsApi'
import { ApiError } from '../types/ApiError'

interface CurrentUserResponse {
  id: string
  username: string
  email: string
  name: string | null
  avatarUrl?: string | null
  onboardingCompletedAt?: string | null
}

export function AuthCallback() {
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

    const code = searchParams.get('code')
    if (!code) {
      setError('Código de autenticação não encontrado.')
      return
    }

    const exchangeGuardKey = `oauth_exchange:${code}`
    const completeLogin = (data: CurrentUserResponse) => {
      login({
        id: data.id,
        username: data.username,
        email: data.email,
        displayName: data.name,
        avatarUrl: data.avatarUrl ?? undefined,
        onboardingCompletedAt: data.onboardingCompletedAt ?? null,
      })
    }

    const finishAuth = async () => {
      try {
        if (sessionStorage.getItem(exchangeGuardKey)) {
          const { data } = await client.get<CurrentUserResponse>(
            '/api/users/me',
            preserveSessionOnUnauthorizedConfig
          )
          completeLogin(data)
          navigate('/home', { replace: true })
          return
        }

        sessionStorage.setItem(exchangeGuardKey, '1')
        const data = await authApi.exchangeOAuthCode(code)
        completeLogin(data)

        const pendingInviteCode = sessionStorage.getItem('pendingInviteCode')
        if (pendingInviteCode) {
          try {
            const joinResponse = await listsApi.joinList(pendingInviteCode)
            sessionStorage.removeItem('pendingInviteCode')
            navigate(`/lists/${joinResponse.id}`, {
              replace: true,
              state: { toastMessage: joinResponse.message, toastType: 'success' },
            })
            return
          } catch (joinError) {
            sessionStorage.removeItem('pendingInviteCode')
            if (joinError instanceof ApiError && joinError.status === 410) {
              setError('Link de convite expirou. Peça um novo link.')
            } else {
              navigate('/home', {
                replace: true,
                state: {
                  toastMessage: 'Erro ao entrar na lista. Tente novamente.',
                  toastType: 'error',
                },
              })
            }
            return
          }
        }

        navigate('/home', { replace: true })
      } catch (err) {
        sessionStorage.removeItem(exchangeGuardKey)
        sessionStorage.removeItem('pendingInviteCode')
        setError(
          err instanceof Error ? err.message : 'Não foi possível concluir o login com Google.'
        )
      }
    }

    void finishAuth()
  }, [searchParams, login, navigate])

  if (error) {
    return (
      <div className="nl-page flex items-center justify-center p-4">
        <div className="nl-card w-full max-w-md p-6 text-center">
          <h1 className="mb-2 font-display text-xl font-bold text-nl-text">Falha no Login</h1>
          <p className="mb-4 text-nl-muted">{error}</p>
          <button
            type="button"
            onClick={() => navigate('/', { replace: true })}
            className="nl-btn-primary w-full"
          >
            Voltar Para Inicio
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="nl-page flex items-center justify-center p-4">
      <div className="nl-card w-full max-w-md p-6 text-center">
        <h1 className="mb-2 font-display text-xl font-bold text-nl-text">Concluindo Login…</h1>
        <p className="text-nl-muted">Aguarde um Instante.</p>
      </div>
    </div>
  )
}
