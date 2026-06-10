import React, { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { authApi } from '../api/authApi'

type Status = 'verifying' | 'success' | 'error'

const LOGIN_HREF = '/?auth=login'

/**
 * Q2.7: hook que confirma o token de verificação de e-mail no backend.
 */
function useEmailVerification(token: string) {
  const [status, setStatus] = useState<Status>('verifying')
  const [error, setError] = useState('')
  const hasProcessedRef = useRef(false)

  useEffect(() => {
    if (hasProcessedRef.current) {
      return
    }
    hasProcessedRef.current = true

    if (!token) {
      setStatus('error')
      setError('O link de verificação está incompleto ou inválido.')
      return
    }

    void authApi
      .verifyEmail(token)
      .then(() => setStatus('success'))
      .catch((err: unknown) => {
        setStatus('error')
        setError(
          err instanceof Error
            ? err.message
            : 'Não foi possível verificar seu e-mail. Solicite um novo link.'
        )
      })
  }, [token])

  return { status, error }
}

const VerifyingState: React.FC = () => (
  <AuthLayout
    badge="Verificação"
    title="Confirmando seu e-mail..."
    description="Aguarde um instante enquanto validamos seu link."
  >
    <div className="nl-card-soft p-5">
      <p className="text-sm leading-7 text-nl-muted">Validando o token de verificação...</p>
    </div>
  </AuthLayout>
)

const SuccessState: React.FC = () => (
  <AuthLayout
    badge="Verificação"
    title="E-mail Confirmado"
    description="Seu e-mail foi verificado com sucesso."
    footer={
      <div className="rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
        Tudo pronto.{' '}
        <Link className="font-semibold text-nl-accent" to={LOGIN_HREF}>
          Voltar para login
        </Link>
      </div>
    }
  >
    <div className="space-y-4">
      <div className="nl-card-soft p-5">
        <p className="text-sm leading-7 text-nl-muted">
          Sua conta está verificada. Agora você pode entrar normalmente.
        </p>
      </div>
      <Link to={LOGIN_HREF} className="nl-btn-primary block w-full text-center">
        Entrar
      </Link>
    </div>
  </AuthLayout>
)

const ErrorState: React.FC<{ message: string }> = ({ message }) => (
  <AuthLayout
    badge="Erro"
    title="Verificação Falhou"
    description="Não foi possível confirmar seu e-mail."
    footer={
      <div className="rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
        <Link className="font-semibold text-nl-accent" to={LOGIN_HREF}>
          Voltar para login
        </Link>
      </div>
    }
  >
    <div className="space-y-4">
      <div className="nl-alert" role="alert" aria-live="polite">
        {message}
      </div>
      <div className="nl-card-soft p-5">
        <p className="text-sm leading-7 text-nl-muted">
          Você pode solicitar um novo link de verificação na tela de login.
        </p>
      </div>
    </div>
  </AuthLayout>
)

/**
 * Q2.7: página de verificação de e-mail. O link enviado por e-mail aponta para
 * /verify-email?token=... — confirma o token e dá feedback ao usuário.
 */
export const VerifyEmail: React.FC = () => {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const { status, error } = useEmailVerification(token)

  if (status === 'success') {
    return <SuccessState />
  }
  if (status === 'error') {
    return <ErrorState message={error} />
  }
  return <VerifyingState />
}
