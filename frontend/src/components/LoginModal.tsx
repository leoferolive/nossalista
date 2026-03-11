import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import client from '../api/client'
import { ModalShell } from './ModalShell'

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
}

export function LoginModal({ onClose, onSwitchToRegister }: Props) {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

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
        onboardingCompletedAt: data.onboardingCompletedAt ?? null,
      })
      onClose()
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
    <ModalShell
      title="Voltar Para Sua Lista"
      eyebrow="Login"
      description="Entre com email ou Google e retome seu mural de listas, convites e checklists em segundos."
      onClose={onClose}
    >
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
            <Link to="/forgot-password" onClick={onClose} className="text-sm font-semibold text-nl-accent">
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
      </form>

      <div className="mt-6 nl-or">ou</div>

      <button onClick={handleGoogleLogin} className="nl-btn-secondary mt-6 w-full">
        <span
          className="inline-flex h-8 w-8 items-center justify-center rounded-full border border-nl-border bg-nl-surface text-xs font-bold text-nl-accent"
          aria-hidden="true"
        >
          G
        </span>
        Continuar com Google
      </button>

      <div className="mt-6 rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
        Ainda nao criou sua base?{' '}
        {onSwitchToRegister ? (
          <button
            type="button"
            className="font-semibold text-nl-accent"
            onClick={onSwitchToRegister}
          >
            Abrir cadastro
          </button>
        ) : (
          <Link to="/register" onClick={onClose} className="font-semibold text-nl-accent">
            Criar conta
          </Link>
        )}
      </div>
    </ModalShell>
  )
}
