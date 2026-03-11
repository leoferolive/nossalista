import React, { useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { authApi } from '../api/authApi'

function buildLink(path: string, redirectPath: string | null) {
  if (!redirectPath) {
    return path
  }

  return `${path}?redirect=${encodeURIComponent(redirectPath)}`
}

export const Register: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const redirectPath = searchParams.get('redirect')

  const [formData, setFormData] = useState({
    name: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const loginHref = useMemo(() => buildLink('/login', redirectPath), [redirectPath])
  const forgotPasswordHref = useMemo(
    () => buildLink('/forgot-password', redirectPath),
    [redirectPath]
  )

  const handleChange = (field: keyof typeof formData, value: string) => {
    setFormData((prev) => ({
      ...prev,
      [field]: field === 'username' ? value.toLowerCase() : value,
    }))
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')

    const normalizedUsername = formData.username.trim().toLowerCase()
    const normalizedEmail = formData.email.trim().toLowerCase()

    if (!/^[a-z0-9_-]{3,50}$/.test(normalizedUsername)) {
      setError('Use um username com 3 a 50 caracteres em minusculas, numeros, hifen ou underscore.')
      return
    }

    if (formData.password.length < 6) {
      setError('A senha precisa ter pelo menos 6 caracteres.')
      return
    }

    if (formData.password !== formData.confirmPassword) {
      setError('As senhas nao conferem.')
      return
    }

    setLoading(true)

    try {
      await authApi.register({
        name: formData.name.trim() || undefined,
        username: normalizedUsername,
        email: normalizedEmail,
        password: formData.password,
      })

      const nextSearchParams = new URLSearchParams()
      nextSearchParams.set('registered', '1')
      nextSearchParams.set('email', normalizedEmail)

      if (redirectPath) {
        nextSearchParams.set('redirect', redirectPath)
      }

      navigate(`/login?${nextSearchParams.toString()}`, { replace: true })
    } catch (submitError) {
      const message =
        submitError instanceof Error ? submitError.message : 'Nao foi possivel criar sua conta.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      badge="Cadastro"
      title="Criar Conta"
      description="Configure seu acesso com um fluxo simples, bonito e pronto para convidar outras pessoas depois."
      footer={
        <div className="rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
          Ja tem conta?{' '}
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
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label htmlFor="name" className="nl-label">
              Nome
            </label>
            <input
              id="name"
              type="text"
              name="name"
              value={formData.name}
              onChange={(event) => handleChange('name', event.target.value)}
              className="nl-input"
              placeholder="Como voce quer aparecer"
              autoComplete="name"
            />
          </div>

          <div>
            <label htmlFor="username" className="nl-label">
              Username
            </label>
            <input
              id="username"
              type="text"
              name="username"
              value={formData.username}
              onChange={(event) => handleChange('username', event.target.value)}
              className="nl-input"
              placeholder="leo_oliveira"
              autoComplete="username"
              spellCheck={false}
              required
            />
          </div>

          <div>
            <label htmlFor="email" className="nl-label">
              Email
            </label>
            <input
              id="email"
              type="email"
              name="email"
              value={formData.email}
              onChange={(event) => handleChange('email', event.target.value)}
              className="nl-input"
              placeholder="voce@email.com"
              autoComplete="email"
              inputMode="email"
              spellCheck={false}
              required
            />
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="password" className="nl-label">
              Senha
            </label>
            <input
              id="password"
              type="password"
              name="password"
              value={formData.password}
              onChange={(event) => handleChange('password', event.target.value)}
              className="nl-input"
              autoComplete="new-password"
              required
            />
          </div>

          <div>
            <label htmlFor="confirm-password" className="nl-label">
              Confirmar senha
            </label>
            <input
              id="confirm-password"
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={(event) => handleChange('confirmPassword', event.target.value)}
              className="nl-input"
              autoComplete="new-password"
              required
            />
          </div>
        </div>

        <button type="submit" disabled={loading} className="nl-btn-primary w-full">
          {loading ? 'Criando conta...' : 'Criar conta'}
        </button>
      </form>

      <div className="mt-5 text-sm text-nl-muted">
        Quer revisar as opcoes de acesso?{' '}
        <Link className="font-semibold text-nl-accent" to={forgotPasswordHref}>
          Ver ajuda de login
        </Link>
      </div>
    </AuthLayout>
  )
}
