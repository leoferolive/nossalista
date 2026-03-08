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
        <div className="rounded-3xl border border-nl-border/20 bg-nl-bg-soft p-4 text-sm text-nl-text">
          <Link
            className="font-semibold text-nl-primary underline decoration-nl-accent underline-offset-4"
            to={loginHref}
          >
            Voltar para login
          </Link>
        </div>
      }
    >
      <div className="space-y-4">
        <div className="rounded-[28px] border border-nl-border/20 bg-nl-bg-soft p-5">
          <p className="text-sm font-semibold uppercase tracking-[0.24em] text-nl-accent">
            Status Atual
          </p>
          <p className="mt-3 text-sm leading-7 text-nl-text">
            A recuperacao de senha completa ainda nao foi implementada no backend. Por enquanto, use
            seu login com Google se ele estiver vinculado a conta ou volte ao login tradicional.
          </p>
        </div>

        <div className="rounded-[28px] border border-nl-border/20 bg-nl-surface p-5 text-sm leading-7 text-nl-muted shadow-sm">
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
            className="inline-flex min-h-[48px] flex-1 items-center justify-center rounded-2xl bg-gradient-to-r from-nl-primary to-nl-primary-strong px-5 py-3 text-sm font-semibold text-white transition-colors hover:from-nl-primary-strong hover:to-nl-primary-strong focus-visible:ring-2 focus-visible:ring-nl-focus/40"
          >
            Voltar Para Login
          </Link>
          <Link
            to={
              redirectPath ? `/register?redirect=${encodeURIComponent(redirectPath)}` : '/register'
            }
            className="inline-flex min-h-[48px] flex-1 items-center justify-center rounded-2xl border border-nl-border/20 bg-nl-surface px-5 py-3 text-sm font-semibold text-nl-text transition-colors hover:border-nl-border/50 hover:bg-nl-bg-soft hover:text-nl-text focus-visible:ring-2 focus-visible:ring-nl-focus/40"
          >
            Criar Conta
          </Link>
        </div>
      </div>
    </AuthLayout>
  )
}
