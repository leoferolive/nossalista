import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTheme } from '../contexts/ThemeContext'

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
  const { theme, toggleTheme } = useTheme()
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
    navigate('/login', { replace: true })
  }

  return (
    <header className="nl-card mb-6 overflow-visible p-4 sm:p-6">
      <div>
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex items-start gap-4">
            {onBack && (
              <button
                type="button"
                onClick={onBack}
                className="mt-1 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl border border-orange-200 bg-white/90 text-slate-600 transition-colors hover:bg-orange-50 hover:text-slate-900 focus-visible:ring-2 focus-visible:ring-orange-400"
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
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-teal-700">
                {eyebrow}
              </p>
              <div className="mt-2 flex items-center gap-3">
                <div
                  className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-orange-500 to-amber-500 text-sm font-semibold text-white shadow-md"
                  aria-hidden="true"
                >
                  NL
                </div>
                <div>
                  <h1 className="font-display text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
                    {title}
                  </h1>
                  {subtitle && (
                    <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600">{subtitle}</p>
                  )}
                </div>
              </div>
            </div>
          </div>

          <div className="flex items-start justify-between gap-4 sm:justify-end">
            <div className="hidden rounded-2xl border border-orange-200 bg-orange-50/70 px-4 py-3 text-right text-xs text-orange-900 sm:block">
              <p className="font-semibold uppercase tracking-[0.15em] text-orange-700">
                Sessao Ativa
              </p>
              <p className="mt-1 text-sm font-medium text-slate-800">{displayName}</p>
            </div>

            <button
              type="button"
              onClick={toggleTheme}
              aria-label={theme === 'light' ? 'Ativar modo escuro' : 'Ativar modo claro'}
              className="inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-2xl border border-nl-border/30 bg-nl-surface text-nl-muted transition-colors hover:bg-nl-surface-strong hover:text-nl-text focus-visible:ring-2 focus-visible:ring-nl-focus/40"
            >
              {theme === 'light' ? (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.8}
                    d="M21.752 15.002A9.72 9.72 0 0 1 18 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 0 0 3 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 0 0 9.002-5.998Z"
                  />
                </svg>
              ) : (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.8}
                    d="M12 3v2.25m6.364.386-1.591 1.591M21 12h-2.25m-.386 6.364-1.591-1.591M12 18.75V21m-4.773-4.227-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0Z"
                  />
                </svg>
              )}
            </button>

            <div className="relative" ref={menuRef}>
              <button
                type="button"
                onClick={() => setIsMenuOpen((prev) => !prev)}
                className="inline-flex min-h-[52px] items-center gap-3 rounded-2xl border border-orange-200 bg-white px-3 py-2 text-left text-slate-900 shadow-sm transition-colors hover:bg-orange-50 focus-visible:ring-2 focus-visible:ring-orange-400"
                aria-expanded={isMenuOpen}
                aria-haspopup="menu"
                aria-label="Abrir menu da conta"
              >
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-100 text-sm font-semibold text-teal-800">
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
                  <p className="max-w-[9rem] truncate text-sm font-semibold">{displayName}</p>
                  <p className="max-w-[9rem] truncate text-xs text-slate-500">@{user?.username}</p>
                </div>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className={`h-4 w-4 text-slate-500 transition-transform ${isMenuOpen ? 'rotate-180' : ''}`}
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
                  className="absolute right-0 z-30 mt-2 w-64 overflow-hidden rounded-2xl border border-orange-200 bg-white p-2 shadow-tropical"
                  role="menu"
                >
                  <div className="rounded-xl bg-orange-50/80 px-4 py-3">
                    <p className="text-sm font-semibold text-slate-900">{displayName}</p>
                    <p className="text-xs text-slate-500">{user?.email}</p>
                  </div>

                  <div className="mt-2 space-y-1">
                    <Link
                      to="/"
                      className="flex w-full items-center justify-between rounded-xl px-4 py-3 text-sm font-medium text-slate-700 transition-colors hover:bg-orange-50 hover:text-slate-900"
                      role="menuitem"
                    >
                      Home
                      <span aria-hidden="true">⌂</span>
                    </Link>
                    <Link
                      to="/profile"
                      className="flex w-full items-center justify-between rounded-xl px-4 py-3 text-sm font-medium text-slate-700 transition-colors hover:bg-orange-50 hover:text-slate-900"
                      role="menuitem"
                    >
                      Meu perfil
                      <span aria-hidden="true">•</span>
                    </Link>
                    <button
                      type="button"
                      onClick={handleLogout}
                      className="flex w-full items-center justify-between rounded-xl px-4 py-3 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 hover:text-red-700"
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
