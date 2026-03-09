import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { LoginModal } from '../components/LoginModal'

/**
 * Página inicial pública — landing page com split screen.
 * Usuários autenticados são redirecionados para /home automaticamente.
 */
export function LandingPage() {
  const { isAuthenticated, isBootstrapping } = useAuth()
  const navigate = useNavigate()
  const [showLogin, setShowLogin] = useState(false)

  useEffect(() => {
    if (!isBootstrapping && isAuthenticated) {
      navigate('/home', { replace: true })
    }
  }, [isAuthenticated, isBootstrapping, navigate])

  if (isBootstrapping) {
    return (
      <div className="nl-page flex items-center justify-center">
        <div className="nl-card px-6 py-4 text-nl-muted">Carregando…</div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen bg-nl-bg">
      {/* ── Left side: Hero ───────────────────────────── */}
      <div className="flex w-full flex-col justify-center px-8 py-16 sm:px-14 lg:w-[55%] lg:px-20">
        {/* Logo */}
        <div className="mb-12">
          <span
            className="font-display text-xl font-bold tracking-tight text-nl-text"
            aria-label="NossaLista"
          >
            Nossa<span className="text-nl-accent">Lista</span>
          </span>
        </div>

        {/* Eyebrow */}
        <span className="mb-3 inline-block w-fit rounded-full border border-nl-primary/30 bg-nl-primary/10 px-3 py-1 font-sans text-xs font-semibold uppercase tracking-widest text-nl-primary">
          Listas em tempo real
        </span>

        {/* Headline */}
        <h1 className="mb-5 font-display text-4xl font-bold leading-tight text-nl-text sm:text-5xl">
          Tudo na mesma lista. <span className="text-nl-accent">Todo mundo junto.</span>
        </h1>

        {/* Description */}
        <p className="mb-10 max-w-md font-sans text-lg leading-relaxed text-nl-muted">
          Crie listas de compras, tarefas e desejos — compartilhe com quem quiser e veja as mudanças
          em tempo real, sem precisar dar F5.
        </p>

        {/* CTA */}
        <div className="flex flex-wrap items-center gap-4">
          <button
            onClick={() => setShowLogin(true)}
            className="rounded-2xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-8 py-4 font-sans text-sm font-semibold text-nl-text shadow-earthen transition-transform hover:-translate-y-0.5 hover:shadow-earthen-strong focus-visible:ring-2 focus-visible:ring-nl-accent/50"
          >
            Começar agora — é grátis
          </button>
          <button
            onClick={() => setShowLogin(true)}
            className="font-sans text-sm font-medium text-nl-muted underline decoration-nl-accent/40 underline-offset-4 hover:text-nl-text"
          >
            Já tenho conta
          </button>
        </div>

        {/* Trust signals */}
        <div className="mt-10 flex flex-wrap gap-6">
          {[
            { icon: '⚡', text: 'Sync instantâneo' },
            { icon: '🔗', text: 'Compartilhe por link' },
            { icon: '📋', text: 'Compras, tarefas e mais' },
          ].map(({ icon, text }) => (
            <div key={text} className="flex items-center gap-2">
              <span aria-hidden="true">{icon}</span>
              <span className="font-sans text-sm text-nl-muted">{text}</span>
            </div>
          ))}
        </div>
      </div>

      {/* ── Right side: App preview ───────────────────── */}
      <div className="relative hidden overflow-hidden bg-nl-surface lg:flex lg:w-[45%] lg:items-center lg:justify-center">
        {/* Ambient glow */}
        <div
          className="pointer-events-none absolute -left-24 -top-24 h-80 w-80 rounded-full opacity-20"
          style={{ background: 'radial-gradient(circle, #d4845a 0%, transparent 70%)' }}
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -bottom-16 -right-16 h-64 w-64 rounded-full opacity-15"
          style={{ background: 'radial-gradient(circle, #5c8a5a 0%, transparent 70%)' }}
          aria-hidden="true"
        />

        {/* Mock app cards */}
        <div className="relative z-10 flex flex-col gap-4 px-10" aria-hidden="true">
          {/* Mock card 1 */}
          <div className="nl-card w-72 p-5 shadow-earthen">
            <div className="mb-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xl">🛒</span>
                <span className="font-display font-semibold text-nl-text">Mercado Semanal</span>
              </div>
              <span className="rounded-full bg-nl-primary/15 px-2 py-0.5 font-sans text-xs font-medium text-nl-primary">
                Compartilhada
              </span>
            </div>
            <div className="space-y-2">
              {[
                { label: 'Arroz 5kg', done: true },
                { label: 'Feijão carioca', done: true },
                { label: 'Azeite extravirgem', done: false },
                { label: 'Queijo muçarela', done: false },
              ].map(({ label, done }) => (
                <div key={label} className="flex items-center gap-3">
                  <div
                    className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border text-xs ${
                      done
                        ? 'border-nl-primary bg-nl-primary text-nl-bg'
                        : 'border-nl-border bg-nl-surface-strong'
                    }`}
                  >
                    {done && '✓'}
                  </div>
                  <span
                    className={`font-sans text-sm ${
                      done ? 'text-nl-muted line-through' : 'text-nl-text'
                    }`}
                  >
                    {label}
                  </span>
                </div>
              ))}
            </div>
            {/* Online members indicator */}
            <div className="mt-4 flex items-center gap-1.5 border-t border-nl-border pt-3">
              <div className="h-2 w-2 rounded-full bg-nl-primary" />
              <span className="font-sans text-xs text-nl-muted">2 pessoas online agora</span>
            </div>
          </div>

          {/* Mock card 2 — offset */}
          <div className="nl-card ml-8 w-64 p-4 shadow-earthen">
            <div className="mb-3 flex items-center gap-2">
              <span className="text-lg">✅</span>
              <span className="font-display text-sm font-semibold text-nl-text">
                Tarefas do Fim de Semana
              </span>
            </div>
            <div className="space-y-2">
              {[
                { label: 'Lavar o carro', done: false },
                { label: 'Ligar pro dentista', done: false },
              ].map(({ label, done }) => (
                <div key={label} className="flex items-center gap-3">
                  <div
                    className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border text-xs ${
                      done
                        ? 'border-nl-primary bg-nl-primary text-nl-bg'
                        : 'border-nl-border bg-nl-surface-strong'
                    }`}
                  >
                    {done && '✓'}
                  </div>
                  <span className="font-sans text-sm text-nl-text">{label}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Live update toast mock */}
          <div className="ml-2 flex items-center gap-3 rounded-2xl border border-nl-primary/30 bg-nl-primary/10 px-4 py-2.5">
            <div className="h-2 w-2 shrink-0 animate-pulse rounded-full bg-nl-primary" />
            <span className="font-sans text-xs text-nl-primary">
              Ana marcou "Arroz 5kg" como concluído
            </span>
          </div>
        </div>
      </div>

      {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
    </div>
  )
}
