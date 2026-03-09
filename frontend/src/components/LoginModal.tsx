import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import client from '../api/client'

interface LoginResponse {
  id: string
  username: string
  email: string
  name: string
  avatarUrl?: string
  token: string
}

interface Props {
  onClose: () => void
}

export function LoginModal({ onClose }: Props) {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [onClose])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await client.post<LoginResponse>('/api/auth/login', { email, password })
      login(data.token, {
        id: data.id,
        username: data.username,
        email: data.email,
        displayName: data.name,
        avatarUrl: data.avatarUrl,
      })
      navigate('/home')
    } catch {
      setError('Email ou senha inválidos')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleLogin = () => {
    window.location.href = `${window.location.origin}/api/auth/google`
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.65)', backdropFilter: 'blur(6px)' }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
      role="dialog"
      aria-modal="true"
      aria-label="Entrar na conta"
    >
      <div
        className="nl-card w-full max-w-md p-8 relative"
        style={{ animation: 'scaleIn 0.2s ease' }}
      >
        <button
          onClick={onClose}
          className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-xl text-nl-muted transition-colors hover:bg-nl-surface hover:text-nl-text"
          aria-label="Fechar"
        >
          ✕
        </button>

        <div className="mb-6">
          <h2 className="font-display text-2xl font-bold text-nl-text">Entre na sua conta</h2>
          <p className="mt-1 font-sans text-sm text-nl-muted">Continue suas listas em segundos.</p>
        </div>

        {error && (
          <div
            className="mb-5 rounded-2xl border border-nl-danger/30 bg-nl-danger/10 px-4 py-3 text-sm text-nl-text"
            role="alert"
            aria-live="polite"
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label
              htmlFor="modal-email"
              className="mb-1.5 block font-sans text-sm font-medium text-nl-text"
            >
              Email
            </label>
            <input
              type="email"
              id="modal-email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-2xl border border-nl-border bg-nl-surface-strong px-4 py-3 font-sans text-nl-text transition-colors placeholder:text-nl-muted/60 focus:border-nl-border-strong focus-visible:ring-2 focus-visible:ring-nl-accent/30"
              autoComplete="email"
              inputMode="email"
              required
            />
          </div>

          <div>
            <div className="mb-1.5 flex items-center justify-between gap-3">
              <label
                htmlFor="modal-password"
                className="block font-sans text-sm font-medium text-nl-text"
              >
                Senha
              </label>
              <Link
                to="/forgot-password"
                onClick={onClose}
                className="font-sans text-sm text-nl-muted underline decoration-nl-accent/50 underline-offset-4 hover:text-nl-text"
              >
                Esqueci minha senha
              </Link>
            </div>
            <input
              type="password"
              id="modal-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-2xl border border-nl-border bg-nl-surface-strong px-4 py-3 font-sans text-nl-text transition-colors placeholder:text-nl-muted/60 focus:border-nl-border-strong focus-visible:ring-2 focus-visible:ring-nl-accent/30"
              autoComplete="current-password"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-2xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-5 py-3.5 font-sans text-sm font-semibold text-nl-text shadow-earthen transition-transform hover:-translate-y-0.5 hover:shadow-earthen-strong focus-visible:ring-2 focus-visible:ring-nl-accent/50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? 'Entrando…' : 'Entrar'}
          </button>
        </form>

        <div className="my-6 flex items-center gap-3">
          <div className="h-px flex-1 bg-nl-border" />
          <span className="font-sans text-xs font-semibold uppercase tracking-[0.24em] text-nl-muted">
            ou
          </span>
          <div className="h-px flex-1 bg-nl-border" />
        </div>

        <button
          onClick={handleGoogleLogin}
          className="flex w-full items-center justify-center gap-3 rounded-2xl border border-nl-border bg-nl-surface-strong px-5 py-3.5 font-sans text-sm font-semibold text-nl-text transition-colors hover:border-nl-border-strong hover:bg-nl-surface focus-visible:ring-2 focus-visible:ring-nl-accent/30"
        >
          <span
            className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-nl-surface text-xs font-bold text-nl-accent"
            aria-hidden="true"
          >
            G
          </span>
          Continuar com Google
        </button>

        <p className="mt-5 text-center font-sans text-sm text-nl-muted">
          Não tem conta?{' '}
          <Link
            to="/register"
            onClick={onClose}
            className="font-semibold text-nl-accent underline decoration-nl-accent/50 underline-offset-4 hover:text-nl-accent-strong"
          >
            Criar conta
          </Link>
        </p>
      </div>
    </div>
  )
}
