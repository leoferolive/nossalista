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
        <div className="mx-auto max-w-md">
          {/* Decoração geométrica sutil */}
          <div className="relative mb-8 flex flex-col items-center" aria-hidden="true">
            <svg
              className="absolute -top-6 left-1/2 -translate-x-1/2 opacity-10"
              width="260"
              height="60"
              viewBox="0 0 260 60"
              fill="none"
            >
              <line x1="0" y1="30" x2="260" y2="30" stroke="#d2a56e" strokeWidth="0.5" />
              <line x1="130" y1="0" x2="130" y2="60" stroke="#d2a56e" strokeWidth="0.5" />
              <circle cx="130" cy="30" r="20" stroke="#d2a56e" strokeWidth="0.5" />
              <circle cx="130" cy="30" r="8" stroke="#d2a56e" strokeWidth="0.5" />
            </svg>
            {/* Logo */}
            <div className="flex h-16 w-16 items-center justify-center rounded-3xl bg-gradient-to-br from-nl-accent to-nl-accent-strong text-xl font-bold text-nl-text shadow-earthen">
              NL
            </div>
            <p className="mt-3 font-sans text-xs font-semibold uppercase tracking-[0.3em] text-nl-muted">
              NossaLista
            </p>
          </div>

          <section className="nl-card w-full p-6 sm:p-8" aria-label="Formulario de acesso">
            <p className="inline-flex rounded-full border border-nl-border-strong bg-nl-surface-strong px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-nl-accent">
              {badge}
            </p>
            <h1 className="mt-4 font-display text-3xl font-bold text-nl-text">{title}</h1>
            <p className="mt-2 font-sans text-sm leading-6 text-nl-muted">{description}</p>

            <div className="mt-6">{children}</div>

            {footer && <div className="mt-6">{footer}</div>}
          </section>
        </div>
      </div>
    </div>
  )
}
