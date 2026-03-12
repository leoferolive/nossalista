import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/authApi'
import { ModalShell } from './ModalShell'

interface RegisterModalProps {
  onClose: () => void
  onSwitchToLogin?: () => void
}

export function RegisterModal({ onClose, onSwitchToLogin }: RegisterModalProps) {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    name: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const canSubmit = useMemo(
    () =>
      formData.username.trim().length >= 3 &&
      formData.email.trim().length > 3 &&
      formData.password.length >= 6 &&
      formData.confirmPassword.length >= 6,
    [formData]
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
      setError('Use um username com 3 a 50 caracteres, minusculas, numeros, hifen ou underscore.')
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

      navigate(`/?auth=login&registered=1&email=${encodeURIComponent(normalizedEmail)}`, {
        replace: true,
      })
    } catch (submitError) {
      const message =
        submitError instanceof Error ? submitError.message : 'Nao foi possivel criar sua conta.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ModalShell
      title="Criar Conta"
      eyebrow="Cadastro"
      description="Abra sua conta, monte sua primeira lista e comece a organizar tudo com mais leveza."
      onClose={onClose}
    >
      {error && (
        <div className="nl-alert mb-5" role="alert" aria-live="polite">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="nl-auth-grid">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label htmlFor="register-modal-name" className="nl-label">
              Nome
            </label>
            <input
              id="register-modal-name"
              type="text"
              value={formData.name}
              onChange={(event) => handleChange('name', event.target.value)}
              className="nl-input"
              placeholder="Como voce quer aparecer para a turma"
              autoComplete="name"
            />
          </div>

          <div>
            <label htmlFor="register-modal-username" className="nl-label">
              Username
            </label>
            <input
              id="register-modal-username"
              type="text"
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
            <label htmlFor="register-modal-email" className="nl-label">
              Email
            </label>
            <input
              id="register-modal-email"
              type="email"
              value={formData.email}
              onChange={(event) => handleChange('email', event.target.value)}
              className="nl-input"
              placeholder="voce@email.com"
              autoComplete="email"
              spellCheck={false}
              inputMode="email"
              required
            />
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="register-modal-password" className="nl-label">
              Senha
            </label>
            <input
              id="register-modal-password"
              type="password"
              value={formData.password}
              onChange={(event) => handleChange('password', event.target.value)}
              className="nl-input"
              autoComplete="new-password"
              required
            />
          </div>

          <div>
            <label htmlFor="register-modal-confirm-password" className="nl-label">
              Confirmar senha
            </label>
            <input
              id="register-modal-confirm-password"
              type="password"
              value={formData.confirmPassword}
              onChange={(event) => handleChange('confirmPassword', event.target.value)}
              className="nl-input"
              autoComplete="new-password"
              required
            />
          </div>
        </div>

        <p className="nl-helper">
          Username e email deixam seu convite rapido e seu acesso facil de retomar.
        </p>

        <button type="submit" disabled={!canSubmit || loading} className="nl-btn-primary w-full">
          {loading ? 'Criando conta...' : 'Criar conta e continuar'}
        </button>
      </form>

      <div className="mt-6 rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
        Ja faz parte da lista?{' '}
        {onSwitchToLogin ? (
          <button type="button" className="font-semibold text-nl-accent" onClick={onSwitchToLogin}>
            Entrar agora
          </button>
        ) : (
          <Link to="/?auth=login" onClick={onClose} className="font-semibold text-nl-accent">
            Entrar agora
          </Link>
        )}
      </div>
    </ModalShell>
  )
}
