import React from 'react'
import { ThemeToggle } from './ThemeToggle'

interface AuthLayoutProps {
  title: string
  description: string
  badge?: string
  children: React.ReactNode
  footer?: React.ReactNode
}

const previewLists = [
  {
    title: 'Mercado da Semana',
    status: 'Compartilhada',
    items: [
      { label: 'Queijo minas fresco', done: true },
      { label: 'Tomate sweet grape', done: false },
      { label: 'Papel manteiga', done: false },
    ],
  },
  {
    title: 'Ritual de Domingo',
    status: 'Checklist',
    items: [
      { label: 'Planejar refeicoes', done: true },
      { label: 'Separar presentes', done: false },
      { label: 'Fechar wishlist', done: false },
    ],
  },
]

export const AuthLayout: React.FC<AuthLayoutProps> = ({
  title,
  description,
  badge = 'Acesso',
  children,
  footer,
}) => {
  return (
    <div className="nl-page flex items-center">
      <div className="nl-container w-full">
        <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
          <section className="nl-card relative overflow-hidden p-6 sm:p-8 lg:p-10">
            <div className="nl-reveal flex items-start justify-between gap-4">
              <div>
                <div className="nl-badge">{badge}</div>
                <h1 className="mt-5 font-display text-4xl font-semibold leading-tight text-nl-text sm:text-5xl">
                  {title}
                </h1>
                <p className="mt-4 max-w-xl text-base leading-8 text-nl-muted sm:text-lg">
                  {description}
                </p>
              </div>
              <ThemeToggle />
            </div>

            <div className="nl-reveal nl-reveal-delay-1 mt-8 flex flex-wrap gap-3">
              <span className="nl-pill">Ritmo editorial</span>
              <span className="nl-pill">Tema claro e escuro</span>
              <span className="nl-pill">Checklist em tempo real</span>
            </div>

            <div className="nl-reveal nl-reveal-delay-2 mt-8">{children}</div>

            {footer && <div className="nl-reveal nl-reveal-delay-3 mt-6">{footer}</div>}
          </section>

          <aside className="relative hidden xl:block">
            <div className="sticky top-6 space-y-5">
              <div className="nl-card p-7">
                <p className="nl-kicker">Atmosfera</p>
                <h2 className="mt-3 font-display text-3xl font-semibold text-nl-text">
                  Papel vivo com brilho de interface.
                </h2>
                <p className="mt-4 text-sm leading-7 text-nl-muted">
                  A experiencia mistura a familiaridade de listas em folha com sinais de presenca,
                  links e colaboracao acontecendo ao vivo.
                </p>
              </div>

              <div className="space-y-4">
                {previewLists.map((list, index) => (
                  <article
                    key={list.title}
                    className={`nl-preview-card ${index % 2 === 0 ? 'nl-float' : 'nl-float-delay'} ${
                      index % 2 === 1 ? 'ml-8' : ''
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-nl-muted">
                          {list.status}
                        </p>
                        <h3 className="mt-2 font-display text-2xl font-semibold text-nl-text">
                          {list.title}
                        </h3>
                      </div>
                      <span className="nl-pill">{index === 0 ? '2 online' : 'Fluxo pessoal'}</span>
                    </div>

                    <div className="mt-5 space-y-3">
                      {list.items.map((item) => (
                        <div key={item.label} className="nl-checkline">
                          <span className="nl-check" data-done={item.done}>
                            {item.done ? '✓' : ''}
                          </span>
                          <span
                            className={`text-sm ${
                              item.done ? 'text-nl-muted line-through' : 'text-nl-text'
                            }`}
                          >
                            {item.label}
                          </span>
                        </div>
                      ))}
                    </div>
                  </article>
                ))}
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  )
}
