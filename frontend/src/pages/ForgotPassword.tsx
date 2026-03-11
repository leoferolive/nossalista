import React from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'

function buildLoginHref(redirectPath: string | null) {
  if (!redirectPath) {
    return '/login'
  }

  return `/login?redirect=${encodeURIComponent(redirectPath)}`
}

export const ForgotPassword: React.FC = () => {
  const [searchParams] = useSearchParams()
  const redirectPath = searchParams.get('redirect')
  const loginHref = buildLoginHref(redirectPath)

  return (
    <AuthLayout
      badge="Recuperacao"
      title="Retomar o Acesso"
      description="O reset automatico ainda nao foi implementado, mas a interface ja organiza os caminhos de retorno com mais clareza."
      footer={
        <div className="rounded-[1.4rem] border border-nl-border bg-nl-surface-muted/50 p-4 text-sm text-nl-muted">
          <Link className="font-semibold text-nl-accent" to={loginHref}>
            Voltar para login
          </Link>
        </div>
      }
    >
      <div className="space-y-4">
        <div className="nl-card-soft p-5">
          <p className="nl-kicker">Status atual</p>
          <p className="mt-3 text-sm leading-7 text-nl-muted">
            A redefinicao completa ainda depende do backend. Por enquanto, use o login com Google
            se sua conta ja estiver vinculada ou tente novamente com email e senha.
          </p>
        </div>

        <div className="nl-card-soft p-5">
          <p className="font-semibold text-nl-text">O que fazer agora</p>
          <ul className="mt-3 space-y-2 text-sm leading-7 text-nl-muted">
            <li>Use o botao do Google se esse foi seu caminho anterior.</li>
            <li>Volte ao login e revise o email digitado.</li>
            <li>Se ainda nao tem conta, abra um cadastro novo.</li>
          </ul>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row">
          <Link to={loginHref} className="nl-btn-primary flex-1">
            Voltar Para Login
          </Link>
          <Link
            to={redirectPath ? `/register?redirect=${encodeURIComponent(redirectPath)}` : '/register'}
            className="nl-btn-secondary flex-1"
          >
            Criar Conta
          </Link>
        </div>
      </div>
    </AuthLayout>
  )
}
