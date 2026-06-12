import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { authApi } from '../api/authApi'
import { listsApi } from '../api/listsApi'
import { ApiError } from '../types/ApiError'
import { clearStoredSession, getStoredAuthToken, persistAuthToken } from '../auth/session'

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

    // Q2.3: o OAuth2 não devolve mais o JWT na URL. Recebemos um one-time code
    // opaco e o trocamos pelo JWT via endpoint seguro.
    const code = searchParams.get('code')

    if (!code) {
      setError('Código de autenticação não encontrado.')
      return
    }

    // Idempotência: o login Google pode gerar callbacks/redirects duplicados
    // (browser, reload do service worker). Cada one-time code é single-use, então
    // só pode ser trocado UMA vez — uma 2ª troca do mesmo code retorna 400 e NÃO
    // deve invalidar a sessão já autenticada pela 1ª. O guard sobrevive a
    // remontagem/reload do AuthCallback no mesmo tab.
    const exchangeGuardKey = `oauth_exchange:${code}`
    if (sessionStorage.getItem(exchangeGuardKey)) {
      if (getStoredAuthToken()) {
        navigate('/home', { replace: true })
      }
      return
    }
    sessionStorage.setItem(exchangeGuardKey, '1')

    const finishAuth = async () => {
      try {
        const data = await authApi.exchangeOAuthCode(code)

        // Persiste o JWT no localStorage (mesma arquitetura de hoje; WebSocket lê daqui).
        persistAuthToken(data.token)

        login(data.token, {
          id: data.id,
          username: data.username,
          email: data.email,
          displayName: data.name,
          avatarUrl: data.avatarUrl ?? undefined,
          onboardingCompletedAt: data.onboardingCompletedAt ?? null,
        })

        // Verificar se há um pending invite code para processar
        const pendingInviteCode = sessionStorage.getItem('pendingInviteCode')

        if (pendingInviteCode) {
          try {
            // Tentar entrar na lista automaticamente
            const joinResponse = await listsApi.joinList(pendingInviteCode)

            // Limpar o código pendente
            sessionStorage.removeItem('pendingInviteCode')

            // Redirecionar para a lista passando a mensagem de boas-vindas via state
            navigate(`/lists/${joinResponse.id}`, {
              replace: true,
              state: { toastMessage: joinResponse.message, toastType: 'success' },
            })
            return
          } catch (error) {
            // Limpar o código pendente mesmo em caso de erro
            sessionStorage.removeItem('pendingInviteCode')

            if (error instanceof ApiError && error.status === 410) {
              setError('Link de convite expirou. Peça um novo link.')
            } else {
              // Redirecionar para home com mensagem de erro via state
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

        // Se não há pending invite, redirecionar para home normalmente
        navigate('/home', { replace: true })
      } catch (err) {
        // Se uma troca concorrente/duplicada do MESMO code já autenticou (token
        // presente), o 400 "code já usado" é esperado e benigno: NÃO limpar a
        // sessão — apenas seguir para a home. Sem isso, a 2ª troca apagaria o
        // token que a 1ª acabou de salvar (causa do 401 nas listas após o login).
        if (getStoredAuthToken()) {
          navigate('/home', { replace: true })
          return
        }
        clearStoredSession()
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
