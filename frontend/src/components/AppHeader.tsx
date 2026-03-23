import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useOnboarding } from '../contexts/OnboardingContext'
import { usePushNotifications } from '../hooks/usePushNotifications'
import { useIsMobile } from '../hooks/useIsMobile'
import { NotificationBell } from './NotificationBell'
import { ResponsiveSheet } from './ResponsiveSheet'
import { ThemeToggle } from './ThemeToggle'

interface AppHeaderProps {
  title: string
  subtitle?: string
  eyebrow?: string
  primaryAction?: React.ReactNode
  secondaryActions?: React.ReactNode
  accountActions?: React.ReactNode
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
  primaryAction,
  secondaryActions,
  accountActions,
  onBack,
  backLabel = 'Voltar',
}) => {
  const isMobile = useIsMobile()
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
  }, [location.pathname, location.search, isMobile])

  useEffect(() => {
    if (!isMenuOpen || isMobile) {
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
  }, [isMenuOpen, isMobile])

  const handleLogout = () => {
    logout()
    navigate('/', { replace: true })
  }

  const pushStatus = (() => {
    if (availability === 'unsupported') {
      return { label: 'Push: não suportado neste navegador', tone: 'text-nl-muted' }
    }
    if (availability === 'server-not-configured') {
      return { label: 'Push: indisponível no ambiente', tone: 'text-nl-muted' }
    }
    if (availability === 'checking') {
      return { label: 'Push: verificando…', tone: 'text-nl-muted' }
    }
    if (pushEnabled) {
      return { label: 'Push: ativado', tone: 'text-nl-primary' }
    }
    return { label: 'Push: desativado', tone: 'text-nl-muted' }
  })()

  const renderMenuActions = (mode: 'desktop' | 'mobile') => {
    const isDesktop = mode === 'desktop'
    const actionRole = isDesktop ? 'menuitem' : undefined

    return (
      <>
        <div className="nl-account-summary">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-nl-surface-muted font-sans text-sm font-semibold text-nl-accent">
            {user?.avatarUrl ? (
              <img
                src={user.avatarUrl}
                alt={displayName}
                className="h-11 w-11 rounded-2xl object-cover"
                width={44}
                height={44}
              />
            ) : (
              initials
            )}
          </div>
          <div className="min-w-0">
            <p className="truncate font-sans text-sm font-semibold text-nl-text">{displayName}</p>
            <p className="truncate font-sans text-xs text-nl-muted">{user?.email}</p>
          </div>
        </div>

        <section className="nl-section-stack">
          <p className="nl-label mb-2">Preferências</p>
          <ThemeToggle className={!isDesktop ? "w-full justify-between" : ""} fullWidth={!isDesktop} />
          <p className={`mt-3 text-sm ${pushStatus.tone}`}>{pushStatus.label}</p>
        </section>

        <section className="nl-section-stack">
          <p className="nl-label mb-2">Navegação</p>
          <div className="space-y-2">
            <button
              type="button"
              role={actionRole}
              onClick={() => {
                setIsMenuOpen(false)
                navigate('/home')
              }}
              className="nl-action-row"
            >
              <span className="flex items-center gap-3">
                <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                  <path
                    fillRule="evenodd"
                    d="M9.293 2.293a1 1 0 011.414 0l7 7A1 1 0 0117 11h-1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-3a1 1 0 00-1-1H9a1 1 0 00-1 1v3a1 1 0 01-1 1H5a1 1 0 01-1-1v-6H3a1 1 0 01-.707-1.707l7-7z"
                    clipRule="evenodd"
                  />
                </svg>
                <span>Home</span>
              </span>
            </button>

            <button
              type="button"
              role={actionRole}
              onClick={() => {
                setIsMenuOpen(false)
                navigate('/profile')
              }}
              className="nl-action-row"
            >
              <span className="flex items-center gap-3">
                <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                  <path
                    fillRule="evenodd"
                    d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-5.5-2.5a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0zM10 12a5.99 5.99 0 00-4.793 2.39A6.483 6.483 0 0010 16.5a6.483 6.483 0 004.793-2.11A5.99 5.99 0 0010 12z"
                    clipRule="evenodd"
                  />
                </svg>
                <span>Meu perfil</span>
              </span>
            </button>
          </div>
        </section>

        <section className="nl-section-stack">
          <p className="nl-label mb-2">Ajuda</p>
          <button
            type="button"
            role={actionRole}
            onClick={() => {
              setIsMenuOpen(false)
              startReplay()
            }}
            className="nl-action-row"
          >
            <span className="flex items-center gap-3">
              <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path
                  fillRule="evenodd"
                  d="M15.312 11.424a5.5 5.5 0 01-9.201 2.466l-.312-.311h2.433a.75.75 0 000-1.5H4.598a.75.75 0 00-.75.75v3.634a.75.75 0 001.5 0v-2.033l.312.311a7 7 0 0011.712-3.138.75.75 0 00-1.449-.39zm-9.624-2.848a.75.75 0 001.45.388A5.5 5.5 0 0116.313 6.89l.311.311h-2.432a.75.75 0 000 1.5h3.634a.75.75 0 00.75-.75V4.317a.75.75 0 00-1.5 0v2.033l-.311-.311A7 7 0 005.054 9.188.75.75 0 005.688 8.576z"
                  clipRule="evenodd"
                />
              </svg>
              <span>Ver tutorial</span>
            </span>
          </button>
        </section>

        {permissionState !== 'unsupported' && availability !== 'server-not-configured' ? (
          <section className="nl-section-stack">
            <p className="nl-label mb-2">Notificações</p>
            {!pushEnabled ? (
              <button
                type="button"
                role={actionRole}
                onClick={() => {
                  setIsMenuOpen(false)
                  enablePush()
                }}
                className="nl-action-row"
              >
                <span className="flex items-center gap-3">
                  <span aria-hidden="true">🔔</span>
                  <span>Ativar notificações push</span>
                </span>
              </button>
            ) : (
              <button
                type="button"
                role={actionRole}
                onClick={() => {
                  setIsMenuOpen(false)
                  disablePush()
                }}
                className="nl-action-row"
              >
                <span className="flex items-center gap-3">
                  <span aria-hidden="true">🔕</span>
                  <span>Desativar notificações push</span>
                </span>
              </button>
            )}
          </section>
        ) : null}

        {accountActions ? <section className="nl-section-stack">{accountActions}</section> : null}

        <section className="nl-section-stack">
          <button
            type="button"
            role={actionRole}
            onClick={handleLogout}
            className="nl-action-row nl-action-row-danger"
          >
            <span className="flex items-center gap-3">
              <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path
                  fillRule="evenodd"
                  d="M4.25 5.5a.75.75 0 00-.75.75v8.5c0 .414.336.75.75.75h8.5a.75.75 0 00.75-.75v-4a.75.75 0 011.5 0v4A2.25 2.25 0 0112.75 17h-8.5A2.25 2.25 0 012 14.75v-8.5A2.25 2.25 0 014.25 4h5a.75.75 0 010 1.5h-5zm7.03-2.03a.75.75 0 011.06 0l3.5 3.5a.75.75 0 010 1.06l-3.5 3.5a.75.75 0 11-1.06-1.06l2.22-2.22H8a.75.75 0 010-1.5h5.5l-2.22-2.22a.75.75 0 010-1.06z"
                  clipRule="evenodd"
                />
              </svg>
              <span>Sair</span>
            </span>
          </button>
        </section>
      </>
    )
  }

  return (
    <header className="nl-card nl-card-unclipped relative z-30 mb-4 p-3 sm:mb-5 sm:p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          {onBack ? (
            <button
              type="button"
              onClick={onBack}
              className="inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl border border-nl-border bg-nl-surface-strong text-nl-muted transition-colors hover:border-nl-border-strong hover:text-nl-text focus-visible:ring-2 focus-visible:ring-nl-accent/40"
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
          ) : null}

          <div className="min-w-0">
            <p className="nl-kicker">{eyebrow}</p>
            {!isMobile ? (
              <div className="mt-3 flex items-start gap-3">
                <div
                  className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-nl-accent to-nl-primary text-sm font-bold text-[var(--nl-btn-text)] shadow-earthen"
                  aria-hidden="true"
                >
                  NL
                </div>
                <div className="min-w-0">
                  <h1 className="font-display text-3xl font-bold tracking-tight text-nl-text sm:text-[2.8rem]">
                    {title}
                  </h1>
                  {subtitle ? (
                    <p className="mt-2 max-w-2xl text-sm leading-7 text-nl-muted">{subtitle}</p>
                  ) : null}
                </div>
              </div>
            ) : null}
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2">

          {user ? <NotificationBell /> : null}

          <div className="relative z-40" ref={menuRef}>
            <button
              type="button"
              onClick={() => setIsMenuOpen((prev) => !prev)}
              className={`inline-flex items-center rounded-2xl border border-nl-border bg-nl-surface-strong text-left text-nl-text shadow-earthen transition-colors hover:border-nl-border-strong focus-visible:ring-2 focus-visible:ring-nl-accent/40 ${
                isMobile
                  ? 'min-h-[44px] min-w-[44px] justify-center px-2.5 py-2'
                  : 'min-h-[56px] gap-3 px-3 py-2'
              }`}
              aria-expanded={isMenuOpen}
              aria-haspopup={isMobile ? 'dialog' : 'menu'}
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

              {!isMobile ? (
                <>
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
                </>
              ) : null}
            </button>

            {!isMobile && isMenuOpen ? (
              <div
                className="absolute right-0 z-50 mt-2 w-[min(20rem,calc(100vw-2rem))] overflow-hidden rounded-3xl border border-nl-border bg-nl-surface p-3 shadow-earthen-strong animate-fade-in"
                role="menu"
              >
                <div className="space-y-3">{renderMenuActions('desktop')}</div>
              </div>
            ) : null}
          </div>
        </div>
      </div>

      {isMobile ? (
        <div className="mt-3 min-w-0">
          <h1 className="font-display text-[1.95rem] font-bold leading-[1.02] tracking-tight text-nl-text">
            {title}
          </h1>
          {subtitle ? (
            <p className="mt-1 line-clamp-1 text-xs leading-5 text-nl-muted">{subtitle}</p>
          ) : null}
        </div>
      ) : null}

      {primaryAction || secondaryActions ? (
        <div
          className={`mt-3 flex gap-3 ${isMobile ? 'flex-col' : 'flex-wrap items-center justify-between'}`}
        >
          {primaryAction ? (
            <div className={isMobile ? 'w-full' : ''}>
              {React.isValidElement<{ className?: string }>(primaryAction)
                ? React.cloneElement(primaryAction, {
                    className: [primaryAction.props.className, isMobile ? 'w-full' : '']
                      .filter(Boolean)
                      .join(' '),
                  })
                : primaryAction}
            </div>
          ) : null}
          {secondaryActions ? (
            <div
              className={`flex gap-3 ${isMobile ? 'overflow-x-auto pb-1' : 'flex-wrap justify-end'}`}
            >
              {secondaryActions}
            </div>
          ) : null}
        </div>
      ) : null}

      {isMobile ? (
        <ResponsiveSheet
          isOpen={isMenuOpen}
          onClose={() => setIsMenuOpen(false)}
          title="Conta"
          description="Atalhos da conta, preferências e ajuda, reorganizados para navegação móvel."
        >
          <div className="space-y-4">{renderMenuActions('mobile')}</div>
        </ResponsiveSheet>
      ) : null}
    </header>
  )
}
