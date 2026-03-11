import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useOnboarding } from '../contexts/OnboardingContext'
import { NotificationBell } from './NotificationBell'
import { usePushNotifications } from '../hooks/usePushNotifications'
import { ThemeToggle } from './ThemeToggle'

interface AppHeaderProps {
  title: string
  subtitle?: string
  eyebrow?: string
  actions?: React.ReactNode
  onBack?: () => void
  backLabel?: string
}

function getInitials(name?: string | null, username?: string) {
  const source = name?.trim() || username || 'NL'
  const parts = source.split(/\s+/).filter(Boolean)

  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase()
  }

  return `${parts[0][0] ?? ''}${parts[1][0] ?? ''}`.toUpperCase()
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  title,
  subtitle,
  eyebrow = 'NossaLista',
  actions,
  onBack,
  backLabel = 'Voltar',
}) => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()
  const { startReplay } = useOnboarding()
  const { permissionState, pushEnabled, availability, enablePush, disablePush } =
    usePushNotifications()
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const displayName = user?.displayName || user?.username || 'Conta'
  const initials = useMemo(
    () => getInitials(user?.displayName, user?.username),
    [user?.displayName, user?.username]
  )

  useEffect(() => {
    setIsMenuOpen(false)
  }, [location.pathname, location.search])

  useEffect(() => {
    if (!isMenuOpen) {
      return
    }

    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsMenuOpen(false)
      }
    }

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsMenuOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isMenuOpen])

  const handleLogout = () => {
    logout()
    navigate('/', { replace: true })
  }

  const pushStatus = (() => {
    if (availability === 'unsupported') {
      return { label: 'Push: não suportado neste navegador', tone: 'text-nl-muted' }
    }
    if (availability === 'server-not-configured') {
      return { label: 'Push: indisponível no ambiente (VAPID)', tone: 'text-nl-danger' }
    }
    if (availability === 'checking') {
      return { label: 'Push: verificando…', tone: 'text-nl-muted' }
    }
    if (pushEnabled) {
      return { label: 'Push: ativado', tone: 'text-nl-accent' }
    }
    return { label: 'Push: desativado', tone: 'text-nl-muted' }
  })()

  return (
    <header className="nl-card mb-6 overflow-visible p-5 sm:p-6">
      <div>
        <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
          <div className="flex items-start gap-4">
            {onBack && (
              <button
                type="button"
                onClick={onBack}
                className="mt-1 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl border border-nl-border bg-nl-surface-strong text-nl-muted transition-colors hover:border-nl-border-strong hover:text-nl-text focus-visible:ring-2 focus-visible:ring-nl-accent/40"
                aria-label={backLabel}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.8}
                    d="M15.75 19.5L8.25 12l7.5-7.5"
                  />
                </svg>
              </button>
            )}

            <div>
              <p className="nl-kicker">{eyebrow}</p>
              <div className="mt-2 flex items-center gap-3">
                <div
                  className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-nl-accent to-nl-primary text-sm font-bold text-[var(--nl-btn-text)] shadow-earthen"
                  aria-hidden="true"
                >
                  NL
                </div>
                <div>
                  <h1 className="font-display text-3xl font-bold tracking-tight text-nl-text sm:text-4xl">
                    {title}
                  </h1>
                  {subtitle && (
                    <p className="mt-2 max-w-2xl text-sm leading-7 text-nl-muted">{subtitle}</p>
                  )}
                </div>
              </div>
            </div>
          </div>

          <div className="flex flex-col items-start gap-4 xl:items-end">
            <div className="flex flex-wrap items-center gap-3 self-stretch xl:justify-end">
              <ThemeToggle />
              {user && <NotificationBell />}
              <div className="hidden rounded-2xl border border-nl-border bg-nl-surface-strong px-4 py-3 text-right text-xs sm:block">
                <p className="font-sans font-semibold uppercase tracking-[0.15em] text-nl-muted">
                  Sessao Ativa
                </p>
                <p className="mt-1 font-sans text-sm font-medium text-nl-text">{displayName}</p>
              </div>
            </div>

            <div className="relative" ref={menuRef}>
              <button
                type="button"
                onClick={() => setIsMenuOpen((prev) => !prev)}
                className="inline-flex min-h-[56px] items-center gap-3 rounded-2xl border border-nl-border bg-nl-surface-strong px-3 py-2 text-left text-nl-text shadow-earthen transition-colors hover:border-nl-border-strong focus-visible:ring-2 focus-visible:ring-nl-accent/40"
                aria-expanded={isMenuOpen}
                aria-haspopup="menu"
                aria-label="Abrir menu da conta"
              >
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-nl-surface-muted font-sans text-sm font-semibold text-nl-accent">
                  {user?.avatarUrl ? (
                    <img
                      src={user.avatarUrl}
                      alt={displayName}
                      className="h-10 w-10 rounded-xl object-cover"
                      width={40}
                      height={40}
                    />
                  ) : (
                    initials
                  )}
                </div>
                <div className="min-w-0">
                  <p className="max-w-[9rem] truncate font-sans text-sm font-semibold text-nl-text">
                    {displayName}
                  </p>
                  <p className="max-w-[9rem] truncate font-sans text-xs text-nl-muted">
                    {user?.username ? `@${user.username}` : 'Conta ativa'}
                  </p>
                </div>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className={`h-4 w-4 text-nl-muted transition-transform ${isMenuOpen ? 'rotate-180' : ''}`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.8}
                    d="M19 9l-7 7-7-7"
                  />
                </svg>
              </button>

              {isMenuOpen && (
                <div
                  className="absolute right-0 z-30 mt-2 w-64 overflow-hidden rounded-2xl border border-nl-border bg-nl-surface p-2 shadow-earthen-strong"
                  role="menu"
                >
                  <div className="rounded-xl bg-nl-surface-strong px-4 py-3">
                    <p className="font-sans text-sm font-semibold text-nl-text">{displayName}</p>
                    <p className="font-sans text-xs text-nl-muted">{user?.email}</p>
                  </div>

                  <div className="mt-2 space-y-1">
                    <div className="rounded-xl bg-nl-surface-strong px-4 py-3">
                      <p className={`font-sans text-xs font-semibold ${pushStatus.tone}`}>
                        {pushStatus.label}
                      </p>
                    </div>
                    <Link
                      to="/home"
                      className="flex w-full items-center justify-between rounded-xl px-4 py-3 font-sans text-sm font-medium text-nl-text transition-colors hover:bg-nl-surface-strong"
                      role="menuitem"
                    >
                      Home
                      <span aria-hidden="true">⌂</span>
                    </Link>
                    <Link
                      to="/profile"
                      className="flex w-full items-center justify-between rounded-xl px-4 py-3 font-sans text-sm font-medium text-nl-text transition-colors hover:bg-nl-surface-strong"
                      role="menuitem"
                    >
                      Meu perfil
                      <span aria-hidden="true">•</span>
                    </Link>
                    <button
                      type="button"
                      onClick={() => {
                        setIsMenuOpen(false)
                        startReplay()
                      }}
                      className="flex w-full items-center justify-between rounded-xl px-4 py-3 font-sans text-sm font-medium text-nl-text transition-colors hover:bg-nl-surface-strong"
                      role="menuitem"
                    >
                      Ver tutorial
                      <span aria-hidden="true">↺</span>
                    </button>
                    {permissionState !== 'unsupported' &&
                      availability !== 'server-not-configured' &&
                      !pushEnabled && (
                        <button
                          type="button"
                          onClick={() => {
                            setIsMenuOpen(false)
                            enablePush()
                          }}
                          className="flex w-full items-center justify-between rounded-xl px-4 py-3 font-sans text-sm font-medium text-nl-text transition-colors hover:bg-nl-surface-strong"
                          role="menuitem"
                        >
                          Ativar notificações push
                          <span aria-hidden="true">🔔</span>
                        </button>
                      )}
                    {permissionState !== 'unsupported' &&
                      availability !== 'server-not-configured' &&
                      pushEnabled && (
                        <button
                          type="button"
                          onClick={() => {
                            setIsMenuOpen(false)
                            disablePush()
                          }}
                          className="flex w-full items-center justify-between rounded-xl px-4 py-3 font-sans text-sm font-medium text-nl-text transition-colors hover:bg-nl-surface-strong"
                          role="menuitem"
                        >
                          Desativar notificações push
                          <span aria-hidden="true">🔕</span>
                        </button>
                      )}
                    <button
                      type="button"
                      onClick={handleLogout}
                      className="flex w-full items-center justify-between rounded-xl px-4 py-3 font-sans text-sm font-medium text-nl-danger transition-colors hover:bg-nl-danger/10"
                      role="menuitem"
                    >
                      Sair
                      <span aria-hidden="true">↗</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {actions && <div className="mt-5 flex flex-wrap gap-3">{actions}</div>}
      </div>
    </header>
  )
}
