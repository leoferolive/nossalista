import React from 'react'
import { ThemeToggle } from './ThemeToggle'

interface AuthLayoutProps {
  title: string
  description: string
  badge?: string
  children: React.ReactNode
  footer?: React.ReactNode
}

const previewList = {
  title: 'Lista da Semana',
  status: 'Ao vivo',
  items: [
    { label: 'Tomate sweet grape', done: true },
    { label: 'Separar presentes', done: false },
    { label: 'Fechar wishlist', done: false },
  ],
}

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
        <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
          <section className="nl-card relative overflow-hidden p-6 sm:p-8 lg:p-10">
            <div className="nl-reveal flex items-start justify-between gap-4">
              <div>
                <div className="nl-badge">{badge}</div>
                <h1 className="mt-5 font-display text-4xl font-semibold leading-tight text-nl-text sm:text-[3.6rem]">
                  {title}
                </h1>
                <p className="mt-4 max-w-xl text-base leading-8 text-nl-muted sm:text-lg">
                  {description}
                </p>
              </div>
              <ThemeToggle />
            </div>

            <div className="nl-reveal nl-reveal-delay-1 mt-8 flex flex-wrap gap-3">
              <span className="nl-pill">Fluxo leve</span>
              <span className="nl-pill">Tema claro e escuro</span>
            </div>

            <div className="nl-reveal nl-reveal-delay-2 mt-8">{children}</div>

            {footer && <div className="nl-reveal nl-reveal-delay-3 mt-6">{footer}</div>}
          </section>

          <aside className="relative hidden xl:block">
            <div className="sticky top-6 space-y-5">
              <article className="nl-preview-card">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-nl-muted">
                      {previewList.status}
                    </p>
                    <h3 className="mt-2 font-display text-2xl font-semibold text-nl-text">
                      {previewList.title}
                    </h3>
                  </div>
                  <span className="nl-pill">2 online</span>
                </div>

                <div className="mt-5 space-y-3">
                  {previewList.items.map((item) => (
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

              <div className="nl-card-soft p-5">
                <p className="text-sm font-semibold text-nl-text">
                  Entre e siga do ponto em que parou.
                </p>
                <p className="mt-2 text-sm leading-7 text-nl-muted">
                  Cadastro, login e convites usam a mesma base visual clara e sem excesso.
                </p>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  )
}
