import { useState, useEffect, useMemo } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import client from '../api/client'
import { listsApi } from '../api/listsApi'
import { ApiError } from '../types/ApiError'
import { AuthLayout } from '../components/AuthLayout'
import { GoogleAuthButton } from '../components/GoogleAuthButton'

interface LoginResponse {
  id: string
  username: string
  email: string
  name: string
  avatarUrl?: string
  onboardingCompletedAt?: string | null
}

export default function Login() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { login } = useAuth()
  const redirectPath = searchParams.get('redirect')
  const [email, setEmail] = useState(searchParams.get('email') ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const registered = searchParams.get('registered') === '1'

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
    if (redirectPath) {
      sessionStorage.setItem('postLoginRedirect', redirectPath)
    }
  }, [redirectPath])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const { data } = await client.post<LoginResponse>('/api/auth/login', {
        email,
        password,
      })

      login({
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
    const redirect = searchParams.get('redirect')
    if (redirect?.startsWith('/join/')) {
      const inviteCode = redirect.slice('/join/'.length)
      if (inviteCode) {
        sessionStorage.setItem('pendingInviteCode', inviteCode)
      }
    }

    window.location.href = `${window.location.origin}/api/auth/google`
  }

  return (
    <AuthLayout
      badge="Login"
      title="Entrar no NossaLista"
      description="Retome suas listas, convites e checklists com uma interface clara, leve e pronta para compartilhar."
      footer={
        <div className="rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
          Nao tem conta ainda?{' '}
          <Link className="font-semibold text-nl-accent" to={registerHref}>
            Criar conta
          </Link>
        </div>
      }
    >
      {registered && (
        <div className="mb-5 rounded-[1.2rem] border border-nl-primary/30 bg-nl-primary/10 px-4 py-3 text-sm text-nl-text">
          Conta criada com sucesso. Agora e so entrar.
        </div>
      )}

      {error && (
        <div className="nl-alert mb-5" role="alert" aria-live="polite">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="nl-auth-grid">
        <div>
          <label htmlFor="email" className="nl-label">
            Email
          </label>
          <input
            type="email"
            id="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="nl-input"
            autoComplete="email"
            inputMode="email"
            spellCheck={false}
            required
          />
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between gap-3">
            <label htmlFor="password" className="nl-label mb-0">
              Senha
            </label>
            <Link to={forgotPasswordHref} className="text-sm font-semibold text-nl-accent">
              Esqueci minha senha
            </Link>
          </div>
          <input
            type="password"
            id="password"
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
      </form>

      <div className="mt-6 nl-or">ou</div>

      <GoogleAuthButton onClick={handleGoogleLogin} className="mt-6 w-full" />

      <div className="mt-5 text-sm text-nl-muted">
        Quer entrar com email e ainda nao criou conta?{' '}
        <Link className="font-semibold text-nl-accent" to={registerHref}>
          Criar conta
        </Link>
      </div>
    </AuthLayout>
  )
}
