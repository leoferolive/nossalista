import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { LoginModal } from '../components/LoginModal'
import { RegisterModal } from '../components/RegisterModal'
import { ThemeToggle } from '../components/ThemeToggle'

type AuthModal = 'login' | 'register' | null

const previewColumns = [
  {
    title: 'Mercado editorial',
    label: 'Compartilhada',
    items: [
      { name: 'Papel arroz premium', done: true },
      { name: 'Queijo meia cura', done: false },
      { name: 'Lista do almoco', done: false },
    ],
  },
  {
    title: 'Sexta em andamento',
    label: 'Checklist',
    items: [
      { name: 'Separar presentes', done: true },
      { name: 'Responder convites', done: false },
      { name: 'Fechar wishlist', done: false },
    ],
  },
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
      <div className="nl-container w-full">
        <div className="grid items-center gap-6 xl:grid-cols-[1.05fr_0.95fr]">
          <section className="nl-reveal relative overflow-hidden rounded-[2rem] p-3">
            <div className="nl-card p-6 sm:p-8 lg:p-10">
              <div className="flex flex-wrap items-start justify-between gap-6">
                <div>
                  <p className="font-display text-3xl font-semibold text-nl-text">
                    Nossa<span className="nl-gradient-text">Lista</span>
                  </p>
                  <p className="mt-3 max-w-xl text-sm leading-7 text-nl-muted">
                    Um mural colaborativo para compras, tarefas e desejos com atmosfera de papel
                    marcado a mao e tempo real de verdade.
                  </p>
                </div>
                <ThemeToggle />
              </div>

              <div className="mt-10">
                <div className="nl-badge">Listas em tempo real</div>
                <h1 className="nl-section-title mt-6 text-nl-text">
                  Tudo na mesma folha.
                  <span className="nl-gradient-text block">Todo mundo no mesmo ritmo.</span>
                </h1>
                <p className="mt-6 max-w-2xl text-lg leading-9 text-nl-muted sm:text-xl">
                  Organize compras, tarefas e desejos em um painel vivo: com presença online,
                  mudanças instantaneas, links de convite e uma linguagem visual que lembra caderno,
                  ficha e checklist bem resolvido.
                </p>
              </div>

              <div className="mt-8 flex flex-wrap items-center gap-4">
                <button
                  onClick={() => setActiveModal('register')}
                  className="nl-btn-primary px-7 sm:px-8"
                >
                  Criar conta gratis
                </button>
                <button onClick={() => setActiveModal('login')} className="nl-btn-secondary px-7">
                  Ja tenho conta
                </button>
              </div>

              <div className="mt-8 flex flex-wrap gap-3">
                <span className="nl-pill">Sync instantaneo</span>
                <span className="nl-pill">Compartilhe por username ou link</span>
                <span className="nl-pill">Compras, tarefas e wishlist</span>
              </div>

              <div className="mt-10 grid gap-4 sm:grid-cols-3">
                {[
                  {
                    title: 'Presenca ao vivo',
                    text: 'Veja quem entrou, marcou item e puxou a lista no mesmo instante.',
                  },
                  {
                    title: 'Checklist com cara de produto',
                    text: 'A experiencia parece uma folha bem desenhada, nao um formulario sem alma.',
                  },
                  {
                    title: 'Fluxo rapido de entrada',
                    text: 'Cadastro e login ficam claros, separados e prontos para mobile e desktop.',
                  },
                ].map((feature, index) => (
                  <article
                    key={feature.title}
                    className={`nl-card-soft nl-reveal p-4 ${index === 1 ? 'nl-reveal-delay-1' : index === 2 ? 'nl-reveal-delay-2' : ''}`}
                  >
                    <p className="text-sm font-semibold text-nl-text">{feature.title}</p>
                    <p className="mt-2 text-sm leading-7 text-nl-muted">{feature.text}</p>
                  </article>
                ))}
              </div>
            </div>
          </section>

          <aside className="relative hidden xl:block">
            <div className="absolute -left-10 top-10 h-72 w-72 rounded-full bg-nl-accent/15 blur-3xl" />
            <div className="absolute bottom-10 right-0 h-72 w-72 rounded-full bg-nl-primary/15 blur-3xl" />
            <div className="relative z-10 flex flex-col gap-5 px-8">
              {previewColumns.map((column, index) => (
                <article
                  key={column.title}
                  className={`nl-preview-card ${index === 0 ? 'nl-float' : 'ml-16 nl-float-delay'}`}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-nl-muted">
                        {column.label}
                      </p>
                      <h2 className="mt-2 font-display text-3xl font-semibold text-nl-text">
                        {column.title}
                      </h2>
                    </div>
                    <span className="nl-pill">{index === 0 ? '2 online' : 'Flow pessoal'}</span>
                  </div>

                  <div className="mt-6 space-y-3">
                    {column.items.map((item) => (
                      <div key={item.name} className="nl-checkline">
                        <span className="nl-check" data-done={item.done}>
                          {item.done ? '✓' : ''}
                        </span>
                        <span
                          className={`text-sm ${
                            item.done ? 'text-nl-muted line-through' : 'text-nl-text'
                          }`}
                        >
                          {item.name}
                        </span>
                      </div>
                    ))}
                  </div>
                </article>
              ))}

              <div className="nl-card-soft ml-5 flex items-center gap-3 p-4">
                <span className="inline-flex h-3 w-3 rounded-full bg-nl-primary shadow-[0_0_20px_var(--nl-primary)]" />
                <p className="text-sm text-nl-text">
                  Ana concluiu "Papel arroz premium" e o grupo viu no mesmo instante.
                </p>
              </div>
            </div>
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
