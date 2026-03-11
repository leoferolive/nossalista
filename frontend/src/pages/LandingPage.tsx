import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { LoginModal } from '../components/LoginModal'
import { RegisterModal } from '../components/RegisterModal'
import { ThemeToggle } from '../components/ThemeToggle'

type AuthModal = 'login' | 'register' | null

const highlights = [
  'Sync instantaneo',
  'Convite por link ou username',
  'Compras, tarefas e desejos',
]

export function LandingPage() {
  const { isAuthenticated, isBootstrapping } = useAuth()
  const navigate = useNavigate()
  const [activeModal, setActiveModal] = useState<AuthModal>(null)

  useEffect(() => {
    if (!isBootstrapping && isAuthenticated) {
      navigate('/home', { replace: true })
    }
  }, [isAuthenticated, isBootstrapping, navigate])

  if (isBootstrapping) {
    return (
      <div className="nl-page flex items-center justify-center">
        <div className="nl-card px-6 py-4 text-nl-muted">Carregando...</div>
      </div>
    )
  }

  return (
    <div className="nl-page flex items-center">
      <div className="nl-container w-full max-w-6xl">
        <div className="mb-8 flex items-center justify-between gap-4">
          <div>
            <p className="font-display text-3xl font-semibold text-nl-text">
              Nossa<span className="nl-gradient-text">Lista</span>
            </p>
            <p className="mt-2 text-sm text-nl-muted">Listas vivas para organizar junto.</p>
          </div>
          <ThemeToggle />
        </div>

        <div className="grid items-center gap-8 lg:grid-cols-[minmax(0,1fr)_27rem]">
          <section className="nl-reveal">
            <div className="nl-badge">Tempo real sem friccao</div>
            <h1 className="nl-section-title mt-6 max-w-3xl text-nl-text">
              Listas simples.
              <span className="nl-gradient-text block">Colaboracao feliz.</span>
            </h1>
            <p className="mt-5 max-w-2xl text-lg leading-8 text-nl-muted sm:text-xl">
              Crie compras, tarefas e desejos em um espaco bonito, rapido e compartilhavel. Tudo
              sincroniza na hora, sem bagunca visual.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-4">
              <button
                onClick={() => setActiveModal('register')}
                className="nl-btn-primary min-w-[12.5rem] px-7"
              >
                Criar conta gratis
              </button>
              <button
                onClick={() => setActiveModal('login')}
                className="nl-btn-secondary min-w-[10.5rem] px-6"
              >
                Ja tenho conta
              </button>
            </div>

            <div className="mt-8 grid gap-3 sm:grid-cols-3">
              {highlights.map((highlight, index) => (
                <div
                  key={highlight}
                  className={`nl-card-soft p-4 ${index === 1 ? 'nl-reveal nl-reveal-delay-1' : index === 2 ? 'nl-reveal nl-reveal-delay-2' : 'nl-reveal'}`}
                >
                  <p className="text-sm font-semibold text-nl-text">{highlight}</p>
                </div>
              ))}
            </div>
          </section>

          <aside className="nl-reveal lg:justify-self-end">
            <article className="nl-preview-card mx-auto w-full max-w-[27rem]">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="nl-preview-kicker">
                    <span className="nl-status-dots" aria-hidden="true">
                      <span />
                      <span />
                      <span />
                    </span>
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-nl-muted">
                      Compartilhada
                    </p>
                  </div>
                  <h2 className="mt-2 font-display text-3xl font-semibold text-nl-text">
                    Mercado da Semana
                  </h2>
                </div>
                <span className="nl-pill">2 online</span>
              </div>

              <div className="mt-6 space-y-4">
                {[
                  { name: 'Tomate sweet grape', done: true },
                  { name: 'Queijo minas', done: false },
                  { name: 'Iogurte natural', done: false },
                  { name: 'Lembrete de feira', done: false },
                ].map((item) => (
                  <div key={item.name} className="nl-checkline">
                    <span className="nl-check" data-done={item.done}>
                      {item.done ? '✓' : ''}
                    </span>
                    <span
                      className={
                        item.done ? 'text-sm text-nl-muted line-through' : 'text-sm text-nl-text'
                      }
                    >
                      {item.name}
                    </span>
                  </div>
                ))}
              </div>

              <div className="mt-6 rounded-[1.2rem] bg-nl-surface-muted/70 px-4 py-3 text-sm text-nl-muted">
                Ana marcou o primeiro item e todo mundo viu na hora.
              </div>
            </article>
          </aside>
        </div>
      </div>

      {activeModal === 'login' && (
        <LoginModal
          onClose={() => setActiveModal(null)}
          onSwitchToRegister={() => setActiveModal('register')}
        />
      )}

      {activeModal === 'register' && (
        <RegisterModal
          onClose={() => setActiveModal(null)}
          onSwitchToLogin={() => setActiveModal('login')}
        />
      )}
    </div>
  )
}
