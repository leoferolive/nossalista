import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import client from '../api/client'
import { authApi } from '../api/authApi'
import { listsApi } from '../api/listsApi'
import { ApiError } from '../types/ApiError'
import { ModalShell } from './ModalShell'
import { GoogleAuthButton } from './GoogleAuthButton'

interface LoginResponse {
  id: string
  username: string
  email: string
  name: string
  avatarUrl?: string
  onboardingCompletedAt?: string | null
  token: string
}

interface Props {
  onClose: () => void
  onSwitchToRegister?: () => void
  showRegisteredMessage?: boolean
  showResetMessage?: boolean
  initialEmail?: string
}

export function LoginModal({
  onClose,
  onSwitchToRegister,
  showRegisteredMessage = false,
  showResetMessage = false,
  initialEmail = '',
}: Props) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { login } = useAuth()
  const redirectPath = searchParams.get('redirect')
  const [email, setEmail] = useState(initialEmail)
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [magicLoading, setMagicLoading] = useState(false)
  const [magicMessage, setMagicMessage] = useState('')

  const registerHref = useMemo(() => {
    if (!redirectPath) {
      return '/register'
    }

    return `/register?redirect=${encodeURIComponent(redirectPath)}`
  }, [redirectPath])

  const forgotPasswordHref = useMemo(() => {
    if (!redirectPath) {
      return '/forgot-password'
    }

    return `/forgot-password?redirect=${encodeURIComponent(redirectPath)}`
  }, [redirectPath])

  useEffect(() => {
    setEmail(initialEmail)
  }, [initialEmail])

  useEffect(() => {
    if (redirectPath) {
      sessionStorage.setItem('postLoginRedirect', redirectPath)
    }
  }, [redirectPath])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setMagicMessage('')
    setLoading(true)

    try {
      const { data } = await client.post<LoginResponse>('/api/auth/login', { email, password })
      login(data.token, {
        id: data.id,
        username: data.username,
        email: data.email,
        displayName: data.name,
        avatarUrl: data.avatarUrl,
        onboardingCompletedAt: data.onboardingCompletedAt ?? null,
      })

      const pendingInviteCode = sessionStorage.getItem('pendingInviteCode')
      if (pendingInviteCode) {
        try {
          const joinResponse = await listsApi.joinList(pendingInviteCode)
          sessionStorage.removeItem('pendingInviteCode')
          navigate(`/lists/${joinResponse.id}`, {
            state: {
              toastMessage: joinResponse.message,
              toastType: 'success',
            },
          })
          return
        } catch (joinError) {
          sessionStorage.removeItem('pendingInviteCode')
          const toastMessage =
            joinError instanceof ApiError && joinError.status === 410
              ? 'Link de convite expirou. Peça um novo link.'
              : 'Erro ao entrar na lista. Tente novamente.'
          navigate('/home', {
            state: {
              toastMessage,
              toastType: 'error',
            },
          })
          return
        }
      }

      const nextRedirect = sessionStorage.getItem('postLoginRedirect')
      if (nextRedirect) {
        sessionStorage.removeItem('postLoginRedirect')
        navigate(nextRedirect)
      } else {
        navigate('/home')
      }
    } catch {
      setError('Email ou senha inválidos')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleLogin = () => {
    window.location.href = `${window.location.origin}/api/auth/google`
  }

  const handleMagicLink = async () => {
    setError('')
    setMagicMessage('')
    if (!email.trim()) {
      setError('Informe seu e-mail para receber o link de acesso.')
      return
    }
    setMagicLoading(true)
    try {
      await authApi.requestMagicLink(email)
    } catch {
      // Anti-enumeração: a mensagem é sempre a mesma, mesmo em falha.
    } finally {
      setMagicMessage('Se existe uma conta com esse e-mail, enviamos um link de acesso.')
      setMagicLoading(false)
    }
  }

  return (
    <ModalShell
      title="Entrar no NossaLista"
      eyebrow="Login"
      description="Acesse suas listas em segundos e continue do ponto em que o grupo parou."
      onClose={onClose}
    >
      {showRegisteredMessage && (
        <div className="mb-5 rounded-[1.2rem] border border-nl-primary/30 bg-nl-primary/10 px-4 py-3 text-sm text-nl-text">
          Conta criada com sucesso. Agora e so entrar.
        </div>
      )}

      {showResetMessage && (
        <div className="mb-5 rounded-[1.2rem] border border-nl-primary/30 bg-nl-primary/10 px-4 py-3 text-sm text-nl-text">
          Senha redefinida com sucesso. Faca login com sua nova senha.
        </div>
      )}

      {magicMessage && (
        <div className="mb-5 rounded-[1.2rem] border border-nl-primary/30 bg-nl-primary/10 px-4 py-3 text-sm text-nl-text">
          {magicMessage}
        </div>
      )}

      {error && (
        <div className="nl-alert mb-5" role="alert" aria-live="polite">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="nl-auth-grid">
        <div>
          <label htmlFor="modal-email" className="nl-label">
            Email
          </label>
          <input
            type="email"
            id="modal-email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="nl-input"
            autoComplete="email"
            inputMode="email"
            required
          />
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between gap-3">
            <label htmlFor="modal-password" className="nl-label mb-0">
              Senha
            </label>
            <Link
              to={forgotPasswordHref}
              onClick={onClose}
              className="text-sm font-semibold text-nl-accent"
            >
              Esqueci minha senha
            </Link>
          </div>
          <input
            type="password"
            id="modal-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="nl-input"
            autoComplete="current-password"
            required
          />
        </div>

        <button type="submit" disabled={loading} className="nl-btn-primary w-full">
          {loading ? 'Entrando...' : 'Entrar'}
        </button>

        <button
          type="button"
          onClick={handleMagicLink}
          disabled={magicLoading}
          className="text-sm font-semibold text-nl-accent"
        >
          {magicLoading ? 'Enviando…' : 'Enviar link mágico'}
        </button>
      </form>

      <div className="mt-6 nl-or">ou</div>

      <GoogleAuthButton onClick={handleGoogleLogin} className="mt-6 w-full" />

      <div className="mt-6 rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
        Ainda nao tem conta?{' '}
        {onSwitchToRegister ? (
          <button
            type="button"
            className="font-semibold text-nl-accent"
            onClick={onSwitchToRegister}
          >
            Abrir cadastro
          </button>
        ) : (
          <Link to={registerHref} onClick={onClose} className="font-semibold text-nl-accent">
            Criar conta
          </Link>
        )}
      </div>
    </ModalShell>
  )
}
