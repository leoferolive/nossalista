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
      title="Recupere Seu Acesso"
      description="O fluxo automatico de redefinicao ainda nao esta disponivel, mas voce consegue retomar o acesso pelos caminhos abaixo."
      footer={
        <div className="rounded-3xl border border-nl-border bg-nl-surface-strong p-4 text-sm text-nl-muted">
          <Link
            className="font-semibold text-nl-primary underline decoration-orange-400 underline-offset-4"
            to={loginHref}
          >
            Voltar para login
          </Link>
        </div>
      }
    >
      <div className="space-y-4">
        <div className="rounded-[28px] border border-nl-border bg-nl-surface-strong p-5">
          <p className="text-sm font-semibold uppercase tracking-[0.24em] text-amber-700">
            Status Atual
          </p>
          <p className="mt-3 text-sm leading-7 text-nl-muted">
            A recuperacao de senha completa ainda nao foi implementada no backend. Por enquanto, use
            seu login com Google se ele estiver vinculado a conta ou volte ao login tradicional.
          </p>
        </div>

        <div className="rounded-[28px] border border-nl-primary/30 bg-nl-surface-strong p-5 text-sm leading-7 text-nl-muted shadow-sm">
          <p className="font-semibold text-nl-text">O Que Fazer Agora</p>
          <ul className="mt-3 space-y-2">
            <li>Use o botao do Google se voce ja acessou dessa forma antes.</li>
            <li>Volte ao login para tentar novamente com outro email.</li>
            <li>Se ainda nao tem conta, crie uma nova com email e senha.</li>
          </ul>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row">
          <Link
            to={loginHref}
            className="inline-flex min-h-[48px] flex-1 items-center justify-center rounded-2xl bg-gradient-to-r from-teal-700 to-teal-600 px-5 py-3 text-sm font-semibold text-white transition-colors hover:from-teal-800 hover:to-teal-700 focus-visible:ring-2 focus-visible:ring-nl-accent/30"
          >
            Voltar Para Login
          </Link>
          <Link
            to={
              redirectPath ? `/register?redirect=${encodeURIComponent(redirectPath)}` : '/register'
            }
            className="inline-flex min-h-[48px] flex-1 items-center justify-center rounded-2xl border border-nl-border bg-nl-surface-strong px-5 py-3 text-sm font-semibold text-nl-muted transition-colors hover:border-nl-border-strong hover:bg-nl-surface-strong hover:text-nl-text focus-visible:ring-2 focus-visible:ring-nl-accent/30"
          >
            Criar Conta
          </Link>
        </div>
      </div>
    </AuthLayout>
  )
}
