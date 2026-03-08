import React from 'react'

interface AuthLayoutProps {
  title: string
  description: string
  badge?: string
  children: React.ReactNode
  footer?: React.ReactNode
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({
  title,
  description,
  badge = 'Acesso',
  children,
  footer,
}) => {
  return (
    <div className="nl-page flex items-center justify-center">
      <div className="nl-container w-full">
        <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[1fr_1.1fr]">
          <aside className="nl-card relative hidden overflow-hidden p-8 lg:block">
            <div
              className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-orange-300/45 blur-2xl"
              aria-hidden="true"
            />
            <div
              className="absolute -bottom-10 -left-8 h-44 w-44 rounded-full bg-teal-300/35 blur-2xl"
              aria-hidden="true"
            />
            <p className="inline-flex rounded-full border border-nl-border/20 bg-nl-bg-soft px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-nl-accent">
              {badge}
            </p>
            <h2 className="mt-6 font-display text-3xl font-bold text-nl-text">
              Organize Tudo Sem Caos
            </h2>
            <p className="mt-4 max-w-sm text-base leading-7 text-nl-text">
              Centralize listas, convites e atualizacoes em um espaco com ritmo rapido para o dia a
              dia.
            </p>
            <ul className="mt-8 space-y-3 text-sm text-nl-text">
              <li className="rounded-2xl border border-nl-border/20 bg-nl-surface/80 px-4 py-3">
                Colaboracao em tempo real com feedback imediato.
              </li>
              <li className="rounded-2xl border border-nl-border/20 bg-nl-surface/80 px-4 py-3">
                Fluxo simples para entrar, criar e compartilhar listas.
              </li>
              <li className="rounded-2xl border border-nl-border/20 bg-nl-surface/80 px-4 py-3">
                Visual vibrante para reduzir friccao e aumentar foco.
              </li>
            </ul>
          </aside>

          <section className="nl-card w-full p-6 sm:p-8" aria-label="Formulario de acesso">
            <p className="inline-flex rounded-full border border-nl-border/20 bg-nl-primary/15 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-nl-primary">
              {badge}
            </p>
            <h1 className="mt-4 font-display text-3xl font-bold text-nl-text">{title}</h1>
            <p className="mt-2 text-sm leading-6 text-nl-muted">{description}</p>

            <div className="mt-6">{children}</div>

            {footer && <div className="mt-6">{footer}</div>}
          </section>
        </div>
      </div>
    </div>
  )
}
