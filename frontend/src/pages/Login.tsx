import { useState, useEffect, useMemo } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import client from '../api/client'
import { AuthLayout } from '../components/AuthLayout'

interface LoginResponse {
  id: string
  username: string
  email: string
  name: string
  avatarUrl?: string
  token: string
}

/**
 * Página de Login
 * Permite autenticação com email/senha ou Google OAuth2
 */
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

  // Salvar redirect parameter no sessionStorage se presente
  useEffect(() => {
    const redirectPath = searchParams.get('redirect')
    if (redirectPath) {
      sessionStorage.setItem('postLoginRedirect', redirectPath)
    }
  }, [searchParams])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const { data } = await client.post<LoginResponse>('/api/auth/login', {
        email,
        password,
      })

      login(data.token, {
        id: data.id,
        username: data.username,
        email: data.email,
        displayName: data.name,
        avatarUrl: data.avatarUrl,
      })

      // Verificar se há redirect pendente
      const redirectPath = sessionStorage.getItem('postLoginRedirect')
      if (redirectPath) {
        sessionStorage.removeItem('postLoginRedirect')
        navigate(redirectPath)
      } else {
        navigate('/')
      }
    } catch {
      setError('Email ou senha inválidos')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleLogin = () => {
    // Salvar invite code no sessionStorage se o redirect apontar para uma página de join
    const redirectPath = searchParams.get('redirect')
    if (redirectPath?.startsWith('/join/')) {
      const inviteCode = redirectPath.slice('/join/'.length)
      if (inviteCode) {
        sessionStorage.setItem('pendingInviteCode', inviteCode)
      }
    }

    window.location.href = `${window.location.origin}/api/auth/google`
  }

  return (
    <AuthLayout
      badge="Login"
      title="Entre na Sua Conta"
      description="Continue suas listas em segundos com login por email ou Google."
      footer={
        <div className="rounded-3xl border border-orange-200 bg-orange-50/70 p-4 text-sm text-slate-700">
          Nao tem conta ainda?{' '}
          <Link
            className="font-semibold text-teal-800 underline decoration-orange-400 underline-offset-4"
            to={registerHref}
          >
            Criar conta
          </Link>
        </div>
      }
    >
      {registered && (
        <div
          className="mb-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
          role="status"
          aria-live="polite"
        >
          Conta criada com sucesso. Agora e so entrar para continuar.
        </div>
      )}

      {error && (
        <div
          className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
          role="alert"
          aria-live="polite"
        >
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-slate-700">
            Email
          </label>
          <input
            type="email"
            id="email"
            name="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full rounded-2xl border border-orange-200 bg-white px-4 py-3 text-slate-900 transition-colors focus:border-orange-400 focus-visible:ring-2 focus-visible:ring-orange-300"
            autoComplete="email"
            inputMode="email"
            spellCheck={false}
            required
          />
        </div>

        <div>
          <div className="mb-1.5 flex items-center justify-between gap-3">
            <label htmlFor="password" className="block text-sm font-medium text-slate-700">
              Senha
            </label>
            <Link
              to={forgotPasswordHref}
              className="text-sm font-medium text-teal-700 underline decoration-orange-400 underline-offset-4 hover:text-teal-900"
            >
              Esqueci minha senha
            </Link>
          </div>
          <input
            type="password"
            id="password"
            name="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded-2xl border border-orange-200 bg-white px-4 py-3 text-slate-900 transition-colors focus:border-orange-400 focus-visible:ring-2 focus-visible:ring-orange-300"
            autoComplete="current-password"
            required
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-2xl bg-gradient-to-r from-teal-700 to-teal-600 px-5 py-3.5 text-sm font-semibold text-white shadow-lg shadow-teal-800/20 transition-transform hover:-translate-y-0.5 hover:from-teal-800 hover:to-teal-700 focus-visible:ring-2 focus-visible:ring-orange-300 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? 'Entrando…' : 'Entrar'}
        </button>
      </form>

      <div className="my-6 flex items-center gap-3">
        <div className="h-px flex-1 bg-slate-200" />
        <span className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">ou</span>
        <div className="h-px flex-1 bg-slate-200" />
      </div>

      <button
        className="flex w-full items-center justify-center gap-3 rounded-2xl border border-orange-200 bg-white px-5 py-3.5 text-sm font-semibold text-slate-700 transition-colors hover:border-orange-300 hover:bg-orange-50 focus-visible:ring-2 focus-visible:ring-orange-300"
        onClick={handleGoogleLogin}
      >
        <span
          className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-xs font-bold text-slate-700"
          aria-hidden="true"
        >
          G
        </span>
        Continuar com Google
      </button>

      <div className="mt-5 text-sm text-slate-600">
        Quer entrar com email e ainda nao criou conta?{' '}
        <Link
          className="font-semibold text-teal-800 underline decoration-orange-400 underline-offset-4"
          to={registerHref}
        >
          Criar conta
        </Link>
      </div>
    </AuthLayout>
  )
}
