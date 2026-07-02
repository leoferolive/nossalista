import {
  Fragment,
  useEffect,
  useState,
  type CSSProperties,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
} from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTheme } from '../contexts/ThemeContext'
import { LoginModal } from '../components/LoginModal'
import { RegisterModal } from '../components/RegisterModal'

type AuthModal = 'login' | 'register' | null

/* -------------------------------------------------------------------------
   Ícones — geometria Lucide (line, 24×24, stroke 2, round) desenhados inline.
   O projeto não usa lib de ícones (fontes self-hosted, deps mínimas); um
   pequeno conjunto inline mantém o footprint zero e é offline-safe.
   ------------------------------------------------------------------------- */
type SvgProps = { size?: number; sw?: number; className?: string; style?: CSSProperties }

function Svg({
  size = 24,
  sw = 2,
  className,
  style,
  children,
}: SvgProps & { children: ReactNode }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={sw}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={style}
      aria-hidden="true"
      focusable="false"
    >
      {children}
    </svg>
  )
}

const CartIcon = (p: SvgProps) => (
  <Svg {...p}>
    <path d="M3 4h2l2.4 12.4a1 1 0 0 0 1 .8h9.7a1 1 0 0 0 1-.8L22 8H6M9 21a1 1 0 1 0 0-2 1 1 0 0 0 0 2Zm9 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z" />
  </Svg>
)
const MoonIcon = (p: SvgProps) => (
  <Svg {...p}>
    <path d="M12 3a9 9 0 1 0 9 9 7 7 0 0 1-9-9Z" />
  </Svg>
)
const SunIcon = (p: SvgProps) => (
  <Svg {...p}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
  </Svg>
)
const ArrowRightIcon = (p: SvgProps) => (
  <Svg {...p}>
    <path d="M5 12h14M13 6l6 6-6 6" />
  </Svg>
)
const CheckIcon = (p: SvgProps) => (
  <Svg {...p}>
    <path d="M20 6 9 17l-5-5" />
  </Svg>
)
const LayersIcon = (p: SvgProps) => (
  <Svg {...p}>
    <path d="M12 2 2 7l10 5 10-5-10-5ZM2 17l10 5 10-5M2 12l10 5 10-5" />
  </Svg>
)
const RefreshIcon = (p: SvgProps) => (
  <Svg {...p}>
    <path d="M23 4v6h-6M1 20v-6h6M3.5 9a9 9 0 0 1 14.9-3.4L23 10M1 14l4.6 4.4A9 9 0 0 0 20.5 15" />
  </Svg>
)
const LinkIcon = (p: SvgProps) => (
  <Svg {...p}>
    <path d="M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1" />
  </Svg>
)

/* Fundos SEMPRE escuros (hero e CTA final não respondem ao tema — por design). */
const HERO_BG =
  'radial-gradient(1100px 600px at 14% -12%, rgba(124,58,237,0.38), transparent 60%),' +
  'radial-gradient(900px 520px at 96% 8%, rgba(20,184,166,0.2), transparent 55%),' +
  'radial-gradient(760px 520px at 50% 118%, rgba(124,58,237,0.16), transparent 62%),' +
  '#14101e'

const CTA_BG =
  'radial-gradient(620px 320px at 18% -30%, rgba(124,58,237,0.5), transparent 60%),' +
  'radial-gradient(520px 300px at 92% 130%, rgba(20,184,166,0.35), transparent 55%),' +
  '#1a1428'

const heroFeatures = [
  {
    label: 'Tudo centralizado',
    tint: 'rgba(167,139,250,0.14)',
    color: '#a78bfa',
    Icon: LayersIcon,
  },
  {
    label: 'Sincroniza na hora',
    tint: 'rgba(45,212,191,0.13)',
    color: '#2dd4bf',
    Icon: RefreshIcon,
  },
  { label: 'Convite por link', tint: 'rgba(99,152,240,0.14)', color: '#6398f0', Icon: LinkIcon },
] as const

const listPreview = [
  { name: 'Tomate sweet grape', done: true },
  { name: 'Queijo minas', done: false },
  { name: 'Iogurte natural', done: false },
  { name: 'Lembrete de feira', done: false },
] as const

const steps = [
  {
    title: 'Crie sua lista',
    desc: 'Compras, tarefas ou desejos. Dê um nome e pronto.',
    color: 'var(--nl-accent)',
  },
  {
    title: 'Convide quem quiser',
    desc: 'Mande um link ou o usuário. A pessoa entra e já edita.',
    color: 'var(--nl-primary-text)',
  },
  {
    title: 'Marquem juntos',
    desc: 'Cada alteração aparece na hora para todo mundo.',
    color: 'var(--nl-info)',
  },
] as const

const features = [
  {
    title: 'Tudo centralizado',
    desc: 'Suas listas param de viver em papéis soltos e conversas perdidas. Uma casa só para organizar a vida.',
    tint: 'var(--nl-accent-soft)',
    color: 'var(--nl-accent)',
    Icon: LayersIcon,
  },
  {
    title: 'Tempo real de verdade',
    desc: 'Alguém marca um item e todo mundo vê na hora. Sem recarregar, sem “quem já comprou o leite?”.',
    tint: 'color-mix(in srgb, var(--nl-primary) 14%, transparent)',
    color: 'var(--nl-primary)',
    Icon: RefreshIcon,
  },
  {
    title: 'Compartilhe em segundos',
    desc: 'Convide por link ou usuário. Família, colegas de casa ou amigos entram e começam a editar juntos.',
    tint: 'color-mix(in srgb, var(--nl-info) 14%, transparent)',
    color: 'var(--nl-info)',
    Icon: LinkIcon,
  },
] as const

export function LandingPage() {
  const { isAuthenticated, isBootstrapping } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [activeModal, setActiveModal] = useState<AuthModal>(null)
  const authQuery = searchParams.get('auth')
  const registeredQuery = searchParams.get('registered') === '1'
  const resetQuery = searchParams.get('reset') === '1'
  const emailQuery = searchParams.get('email') ?? ''

  useEffect(() => {
    const hasPendingInviteCode = sessionStorage.getItem('pendingInviteCode')

    if (!isBootstrapping && isAuthenticated && !authQuery && !hasPendingInviteCode) {
      navigate('/home', { replace: true })
    }
  }, [authQuery, isAuthenticated, isBootstrapping, navigate])

  useEffect(() => {
    if (authQuery === 'login' || authQuery === 'register') {
      setActiveModal(authQuery)
      return
    }

    setActiveModal(null)
  }, [authQuery])

  const updateAuthSearch = (
    modal: AuthModal,
    options?: { registered?: boolean; email?: string | null }
  ) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.delete('auth')
    nextSearchParams.delete('registered')
    nextSearchParams.delete('email')

    if (modal) {
      nextSearchParams.set('auth', modal)
    }

    if (options?.registered) {
      nextSearchParams.set('registered', '1')
    }

    if (options?.email) {
      nextSearchParams.set('email', options.email)
    }

    setSearchParams(nextSearchParams, { replace: true })
  }

  const openRegister = () => updateAuthSearch('register')
  const openLogin = (event: ReactMouseEvent<HTMLAnchorElement>) => {
    event.preventDefault()
    updateAuthSearch('login')
  }

  if (isBootstrapping) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--nl-bg)]">
        <div className="nl-card px-6 py-4 text-nl-muted">Carregando...</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen w-full overflow-x-hidden bg-[var(--nl-bg)] text-[var(--nl-text)]">
      {/* ===== HERO (sempre escuro) ===== */}
      <div className="nl-dark-section relative overflow-hidden" style={{ background: HERO_BG }}>
        {/* Nav */}
        <nav className="mx-auto flex max-w-[1120px] flex-wrap items-center gap-4 px-10 py-[22px]">
          <span className="inline-flex items-center gap-[10px]">
            <span
              className="grid h-[34px] w-[34px] flex-none place-items-center rounded-[10px] text-white"
              style={{ background: 'linear-gradient(135deg,#7c3aed,#6326c9)' }}
            >
              <CartIcon size={19} />
            </span>
            <span className="font-display text-[20px] font-bold" style={{ color: '#ece9f4' }}>
              Nossa<span style={{ color: '#a78bfa' }}>Lista</span>
            </span>
          </span>

          <div className="flex flex-1 justify-center gap-7 text-[14px] font-semibold">
            <a
              href="#recursos"
              className="text-[rgba(236,233,244,0.66)] transition-colors hover:text-[#ece9f4]"
            >
              Recursos
            </a>
            <a
              href="#como-funciona"
              className="text-[rgba(236,233,244,0.66)] transition-colors hover:text-[#ece9f4]"
            >
              Como funciona
            </a>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={toggleTheme}
              aria-label="Alternar tema"
              className="grid h-10 w-10 place-items-center rounded-[8px] border border-[rgba(255,255,255,0.16)] bg-[rgba(255,255,255,0.07)] text-[rgba(236,233,244,0.8)] transition-colors hover:bg-[rgba(255,255,255,0.12)] hover:text-[#ece9f4]"
            >
              {theme === 'dark' ? <SunIcon size={18} /> : <MoonIcon size={18} />}
            </button>
            <a
              href="?auth=login"
              onClick={openLogin}
              className="text-[14px] font-bold text-[rgba(236,233,244,0.8)] transition-colors hover:text-[#ece9f4]"
            >
              Entrar
            </a>
            <button
              type="button"
              onClick={openRegister}
              className="nl-btn-primary"
              style={{ minHeight: '40px', padding: '0 18px', fontSize: '13px' }}
            >
              Criar conta
            </button>
          </div>
        </nav>

        {/* Hero grid */}
        <div className="mx-auto grid max-w-[1120px] items-center gap-[56px] px-10 pb-[84px] pt-[60px] [grid-template-columns:repeat(auto-fit,minmax(min(100%,400px),1fr))]">
          {/* Coluna de texto */}
          <div className="nl-reveal">
            <span
              className="inline-flex h-8 items-center gap-2 rounded-full border px-[14px] text-[11px] font-extrabold uppercase tracking-[0.14em]"
              style={{
                background: 'rgba(45,212,191,0.12)',
                borderColor: 'rgba(45,212,191,0.3)',
                color: '#2dd4bf',
              }}
            >
              <span
                className="h-[7px] w-[7px] rounded-full"
                style={{ background: '#2dd4bf', boxShadow: '0 0 10px #2dd4bf' }}
              />
              Tempo real
            </span>

            <h1
              className="mt-5 font-bold"
              style={{
                fontSize: 'clamp(40px, 4.8vw, 62px)',
                lineHeight: 1.02,
                letterSpacing: '-0.03em',
                color: '#ece9f4',
              }}
            >
              A lista que todo mundo
              <br />
              <span
                style={{
                  background: 'linear-gradient(120deg,#a78bfa,#2dd4bf)',
                  WebkitBackgroundClip: 'text',
                  backgroundClip: 'text',
                  color: 'transparent',
                }}
              >
                vê ao mesmo tempo.
              </span>
            </h1>

            <p
              className="mt-[22px]"
              style={{
                fontSize: '19px',
                lineHeight: 1.55,
                color: 'rgba(236,233,244,0.68)',
                maxWidth: '46ch',
              }}
            >
              Compras, tarefas e desejos num lugar só. Você marca um item e todo mundo vê na hora —
              no mercado, em casa, onde estiver.
            </p>

            <div className="mt-[34px] flex flex-wrap items-center gap-6">
              <button
                type="button"
                onClick={openRegister}
                className="nl-btn-primary"
                style={{ minHeight: '56px', padding: '0 30px', fontSize: '15px' }}
              >
                Criar conta grátis
              </button>
              <a
                href="?auth=login"
                onClick={openLogin}
                className="inline-flex items-center gap-2 text-[16px] font-bold text-[#ece9f4] transition-opacity hover:opacity-80"
              >
                Já tenho conta
                <ArrowRightIcon size={18} sw={2.2} />
              </a>
            </div>

            <div
              className="mt-[18px] inline-flex items-center gap-2 text-[13px] font-semibold"
              style={{ color: 'rgba(236,233,244,0.45)' }}
            >
              <span style={{ color: '#2dd4bf' }}>
                <CheckIcon size={16} sw={2.4} />
              </span>
              Grátis para sempre · sem cartão de crédito
            </div>

            {/* Recursos do hero — ícone + texto, NUNCA botões */}
            <div
              className="mt-[38px] flex flex-wrap gap-[22px_34px] border-t pt-[26px]"
              style={{ borderColor: 'rgba(255,255,255,0.1)' }}
            >
              {heroFeatures.map(({ label, tint, color, Icon }) => (
                <span
                  key={label}
                  className="inline-flex items-center gap-[11px] text-[14.5px] font-semibold"
                  style={{ color: 'rgba(236,233,244,0.72)' }}
                >
                  <span
                    className="grid h-8 w-8 flex-none place-items-center rounded-[9px]"
                    style={{ background: tint, color }}
                  >
                    <Icon size={17} />
                  </span>
                  {label}
                </span>
              ))}
            </div>
          </div>

          {/* Coluna do card "em vidro" */}
          <div className="nl-reveal nl-reveal-delay-2 relative w-full max-w-[410px] justify-self-center">
            <div
              aria-hidden="true"
              className="pointer-events-none absolute"
              style={{
                inset: '-14% -10%',
                background:
                  'radial-gradient(50% 50% at 50% 50%, rgba(124,58,237,0.35), transparent 70%)',
              }}
            />
            <div
              className="relative p-[22px]"
              style={{
                background: 'rgba(33,27,53,0.78)',
                border: '1px solid rgba(255,255,255,0.12)',
                borderRadius: '20px',
                boxShadow: '0 30px 70px rgba(0,0,0,0.45)',
                backdropFilter: 'blur(14px)',
                WebkitBackdropFilter: 'blur(14px)',
              }}
            >
              <div className="flex items-center gap-[10px]">
                <span className="flex gap-[5px]">
                  <span
                    className="h-[9px] w-[9px] rounded-full"
                    style={{ background: '#a78bfa' }}
                  />
                  <span
                    className="h-[9px] w-[9px] rounded-full"
                    style={{ background: '#2dd4bf' }}
                  />
                  <span
                    className="h-[9px] w-[9px] rounded-full"
                    style={{ background: 'rgba(255,255,255,0.22)' }}
                  />
                </span>
                <span
                  className="text-[11px] font-extrabold tracking-[0.14em]"
                  style={{ color: 'rgba(236,233,244,0.45)' }}
                >
                  COMPARTILHADA
                </span>
                <span
                  className="ml-auto inline-flex items-center gap-[6px] rounded-full px-[10px] py-1 text-[12px] font-bold"
                  style={{ color: '#2dd4bf', background: 'rgba(45,212,191,0.12)' }}
                >
                  <span
                    className="h-[7px] w-[7px] rounded-full"
                    style={{ background: '#2dd4bf', boxShadow: '0 0 8px #2dd4bf' }}
                  />
                  2 online
                </span>
              </div>

              <p
                className="mb-1 mt-[14px] font-display text-[24px] font-bold"
                style={{ letterSpacing: '-0.02em', color: '#ece9f4' }}
              >
                Mercado da semana
              </p>

              <div className="mt-2">
                {listPreview.map((item) => (
                  <div
                    key={item.name}
                    className="flex items-center gap-3 border-t py-3"
                    style={{ borderColor: 'rgba(255,255,255,0.08)' }}
                  >
                    {item.done ? (
                      <span
                        className="grid h-[22px] w-[22px] flex-none place-items-center rounded-[7px]"
                        style={{ background: 'linear-gradient(135deg,#2dd4bf,#14b8a6)' }}
                      >
                        <svg
                          width="13"
                          height="13"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="#0b2b26"
                          strokeWidth={3}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          aria-hidden="true"
                        >
                          <path d="M20 6 9 17l-5-5" />
                        </svg>
                      </span>
                    ) : (
                      <span
                        className="h-[22px] w-[22px] flex-none rounded-[7px]"
                        style={{ border: '2px solid rgba(255,255,255,0.22)' }}
                      />
                    )}
                    <span
                      className="flex-1 text-[15px] font-semibold"
                      style={
                        item.done
                          ? { color: 'rgba(236,233,244,0.4)', textDecoration: 'line-through' }
                          : { color: '#ece9f4' }
                      }
                    >
                      {item.name}
                    </span>
                  </div>
                ))}
              </div>

              <div
                className="mt-[14px] flex items-center gap-[10px] border-t pt-[14px]"
                style={{ borderColor: 'rgba(255,255,255,0.08)' }}
              >
                <span
                  className="grid h-[26px] w-[26px] flex-none place-items-center rounded-full text-[12px] font-extrabold"
                  style={{ background: 'rgba(167,139,250,0.2)', color: '#a78bfa' }}
                >
                  A
                </span>
                <span className="text-[13px]" style={{ color: 'rgba(236,233,244,0.55)' }}>
                  Ana marcou o primeiro item — todo mundo viu na hora.
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ===== Como funciona (claro, responde ao tema) ===== */}
      <section id="como-funciona" className="mx-auto max-w-[1120px] px-10 pb-2 pt-[60px]">
        <div className="mb-9 text-center">
          <span className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[var(--nl-accent)]">
            Como funciona
          </span>
          <h2
            className="mt-[10px]"
            style={{ fontSize: 'clamp(28px, 3.2vw, 40px)', letterSpacing: '-0.02em' }}
          >
            Três passos e pronto
          </h2>
        </div>

        <div className="flex flex-wrap items-stretch justify-center gap-4">
          {steps.map((step, index) => (
            <Fragment key={step.title}>
              {index > 0 && (
                <div
                  className="hidden items-center self-center text-[var(--nl-text-faint)] lg:flex"
                  aria-hidden="true"
                >
                  <ArrowRightIcon size={20} />
                </div>
              )}
              <div className="min-w-[250px] flex-1 rounded-[12px] border border-[var(--nl-border)] bg-[var(--nl-surface-strong)] p-6 shadow-[var(--nl-card-shadow)]">
                <span className="font-display text-[15px] font-bold" style={{ color: step.color }}>
                  Passo {index + 1}
                </span>
                <h3 className="mb-[6px] mt-2 text-[18px]">{step.title}</h3>
                <p className="text-[14.5px] leading-[1.55] text-[var(--nl-text-muted)]">
                  {step.desc}
                </p>
              </div>
            </Fragment>
          ))}
        </div>
      </section>

      {/* ===== Recursos (claro, responde ao tema) ===== */}
      <section
        id="recursos"
        className="mx-auto grid max-w-[1120px] gap-5 px-10 pb-2 pt-[56px] [grid-template-columns:repeat(auto-fit,minmax(min(100%,300px),1fr))]"
      >
        <h2 className="sr-only">Recursos</h2>
        {features.map(({ title, desc, tint, color, Icon }) => (
          <div
            key={title}
            className="rounded-[12px] border border-[var(--nl-border)] bg-[var(--nl-surface-strong)] p-[26px] shadow-[var(--nl-card-shadow)] transition-[transform,box-shadow] duration-200 hover:-translate-y-[3px] hover:shadow-[var(--nl-card-shadow-strong)]"
          >
            <span
              className="mb-4 grid h-12 w-12 place-items-center rounded-[14px]"
              style={{ background: tint, color }}
            >
              <Icon size={22} />
            </span>
            <h3 className="mb-2 text-[19px]">{title}</h3>
            <p className="text-[14.5px] leading-[1.55] text-[var(--nl-text-muted)]">{desc}</p>
          </div>
        ))}
      </section>

      {/* ===== CTA final (sempre escuro) ===== */}
      <section className="mx-auto mt-[56px] max-w-[1120px] px-10 pb-[56px]">
        <div
          className="nl-dark-section relative overflow-hidden rounded-[16px] px-8 py-[52px] text-center"
          style={{ background: CTA_BG }}
        >
          <h2
            className="relative"
            style={{
              fontSize: 'clamp(28px, 3.6vw, 40px)',
              letterSpacing: '-0.02em',
              color: '#ece9f4',
            }}
          >
            Organize junto, hoje
          </h2>
          <p
            className="relative mb-[26px] mt-3 text-[16px]"
            style={{ color: 'rgba(236,233,244,0.66)' }}
          >
            Crie sua primeira lista compartilhada. É grátis, para sempre.
          </p>
          <button
            type="button"
            onClick={openRegister}
            className="nl-btn-primary relative"
            style={{ minHeight: '54px', padding: '0 30px', fontSize: '15px' }}
          >
            Criar conta grátis
          </button>
        </div>
      </section>

      {/* ===== Rodapé ===== */}
      <footer className="border-t border-[var(--nl-border)]">
        <div className="mx-auto flex max-w-[1120px] flex-wrap items-center gap-4 px-10 py-[26px] text-[13px] text-[var(--nl-text-muted)]">
          <span className="font-display font-bold text-[var(--nl-text)]">NossaLista</span>
          <span className="flex-1" />
          <a href="#" className="transition-colors hover:text-[var(--nl-text)]">
            Privacidade
          </a>
          <a href="#" className="transition-colors hover:text-[var(--nl-text)]">
            Termos
          </a>
          <a href="#" className="transition-colors hover:text-[var(--nl-text)]">
            Contato
          </a>
        </div>
      </footer>

      {activeModal === 'login' && (
        <LoginModal
          onClose={() => updateAuthSearch(null)}
          onSwitchToRegister={() => updateAuthSearch('register')}
          showRegisteredMessage={registeredQuery}
          showResetMessage={resetQuery}
          initialEmail={emailQuery}
        />
      )}

      {activeModal === 'register' && (
        <RegisterModal
          onClose={() => updateAuthSearch(null)}
          onSwitchToLogin={() => updateAuthSearch('login')}
        />
      )}
    </div>
  )
}
