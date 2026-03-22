import React, { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { authApi } from '../api/authApi'

export const ResetPassword: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const loginHref = '/?auth=login'

  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (!token) {
    return (
      <AuthLayout
        badge="Erro"
        title="Link Invalido"
        description="O link de redefinicao de senha esta incompleto ou invalido."
        footer={
          <div className="rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
            <Link className="font-semibold text-nl-accent" to="/forgot-password">
              Solicitar novo link
            </Link>
          </div>
        }
      >
        <div className="space-y-4">
          <div className="nl-card-soft p-5">
            <p className="text-sm leading-7 text-nl-muted">
              O token de redefinicao nao foi encontrado na URL. Solicite um novo link de
              redefinicao de senha.
            </p>
          </div>
          <Link to="/forgot-password" className="nl-btn-primary block w-full text-center">
            Solicitar Novo Link
          </Link>
        </div>
      </AuthLayout>
    )
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (newPassword.length < 6) {
      setError('A senha precisa ter pelo menos 6 caracteres.')
      return
    }

    if (newPassword !== confirmPassword) {
      setError('As senhas nao conferem.')
      return
    }

    setLoading(true)

    try {
      await authApi.resetPassword(token, newPassword)
      navigate('/?auth=login&reset=1', { replace: true })
    } catch (submitError) {
      const message =
        submitError instanceof Error
          ? submitError.message
          : 'Nao foi possivel redefinir sua senha.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      badge="Redefinicao"
      title="Nova Senha"
      description="Escolha uma nova senha para sua conta."
      footer={
        <div className="rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
          Lembrou sua senha?{' '}
          <Link className="font-semibold text-nl-accent" to={loginHref}>
            Voltar para login
          </Link>
        </div>
      }
    >
      {error && (
        <div className="nl-alert mb-5" role="alert" aria-live="polite">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="nl-auth-grid">
        <div>
          <label htmlFor="new-password" className="nl-label">
            Nova senha
          </label>
          <input
            type="password"
            id="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="nl-input"
            autoComplete="new-password"
            minLength={6}
            required
          />
        </div>

        <div>
          <label htmlFor="confirm-password" className="nl-label">
            Confirmar nova senha
          </label>
          <input
            type="password"
            id="confirm-password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="nl-input"
            autoComplete="new-password"
            minLength={6}
            required
          />
        </div>

        <button type="submit" disabled={loading} className="nl-btn-primary w-full">
          {loading ? 'Redefinindo...' : 'Redefinir senha'}
        </button>
      </form>
    </AuthLayout>
  )
}
