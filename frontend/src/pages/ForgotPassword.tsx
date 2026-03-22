import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { authApi } from '../api/authApi'

export const ForgotPassword: React.FC = () => {
  const loginHref = '/?auth=login'
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      await authApi.forgotPassword(email.trim().toLowerCase())
      setSubmitted(true)
    } catch {
      setError('Nao foi possivel processar sua solicitacao. Tente novamente.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      badge="Recuperacao"
      title="Redefinir Senha"
      description="Informe seu email para receber um link de redefinicao de senha."
      footer={
        <div className="rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
          Lembrou sua senha?{' '}
          <Link className="font-semibold text-nl-accent" to={loginHref}>
            Voltar para login
          </Link>
        </div>
      }
    >
      {submitted ? (
        <div className="space-y-4">
          <div className="rounded-[1.2rem] border border-nl-primary/30 bg-nl-primary/10 px-4 py-3 text-sm text-nl-text">
            Se o email estiver cadastrado, voce recebera um link para redefinir sua senha.
          </div>

          <div className="nl-card-soft p-5">
            <p className="font-semibold text-nl-text">Proximo passo</p>
            <p className="mt-2 text-sm leading-7 text-nl-muted">
              Verifique sua caixa de entrada e clique no link recebido. O link expira em 1 hora.
            </p>
          </div>

          <Link to={loginHref} className="nl-btn-primary block w-full text-center">
            Voltar Para Login
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {error && (
            <div className="nl-alert" role="alert" aria-live="polite">
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
                placeholder="voce@email.com"
                required
              />
            </div>

            <button type="submit" disabled={loading} className="nl-btn-primary w-full">
              {loading ? 'Enviando...' : 'Enviar link de redefinicao'}
            </button>
          </form>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Link to={loginHref} className="nl-btn-secondary flex-1 text-center">
              Voltar Para Login
            </Link>
            <Link to="/register" className="nl-btn-secondary flex-1 text-center">
              Criar Conta
            </Link>
          </div>
        </div>
      )}
    </AuthLayout>
  )
}
