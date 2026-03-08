import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { listsApi } from '../api/listsApi'
import { JoinListResponse, JoinListItem, LIST_TYPES } from '../types/List'
import { ApiError } from '../types/ApiError'
import { useAuth } from '../contexts/AuthContext'

/**
 * Tipo de erro possível na página de join
 */
type JoinErrorType = 'not_found' | 'expired' | 'generic' | null

/**
 * Página de visualização de lista via convite (modo read-only)
 * Endpoint público - não requer autenticação
 */
export function JoinListPage() {
  const { inviteCode } = useParams<{ inviteCode: string }>()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<JoinErrorType>(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [listData, setListData] = useState<JoinListResponse | null>(null)
  const [joining, setJoining] = useState(false)

  // Se o usuário já está autenticado e a lista carregou, entrar automaticamente
  useEffect(() => {
    if (isAuthenticated && inviteCode && listData && !loading && !error && !joining) {
      setJoining(true)
      listsApi
        .joinList(inviteCode)
        .then((response) => {
          navigate(`/lists/${response.id}`, {
            replace: true,
            state: { toastMessage: response.message, toastType: 'success' },
          })
        })
        .catch((err) => {
          if (err instanceof ApiError && err.status === 410) {
            setError('expired')
          } else if (err instanceof ApiError && err.status === 404) {
            setError('not_found')
          } else {
            setError('generic')
            setErrorMessage(err instanceof Error ? err.message : 'Erro ao entrar na lista')
          }
          setJoining(false)
        })
    }
  }, [isAuthenticated, inviteCode, listData, loading, error, joining, navigate])

  useEffect(() => {
    async function loadList() {
      if (!inviteCode) {
        setError('not_found')
        setLoading(false)
        return
      }

      try {
        const data = await listsApi.getListByInviteCode(inviteCode)
        setListData(data)
        setError(null)
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Erro desconhecido'
        setErrorMessage(message)

        if (err instanceof ApiError && err.status === 410) {
          setError('expired')
        } else if (err instanceof ApiError && err.status === 404) {
          setError('not_found')
        } else {
          setError('generic')
        }
      } finally {
        setLoading(false)
      }
    }

    loadList()
  }, [inviteCode])

  /**
   * Calcula se o link expira em breve (< 5 minutos)
   */
  const isExpiringSoon = (): boolean => {
    if (!listData?.expires_at) return false
    const now = new Date()
    const expiresAt = new Date(listData.expires_at)
    const minutesRemaining = Math.floor((expiresAt.getTime() - now.getTime()) / 60000)
    return minutesRemaining >= 0 && minutesRemaining < 5
  }

  /**
   * Obtém o emoji do tipo de lista
   */
  const getTypeEmoji = (slug: string): string => {
    const type = LIST_TYPES.find((t) => t.slug === slug)
    return type?.emoji || '📋'
  }

  /**
   * Salva o inviteCode no sessionStorage para uso após login
   */
  const saveInviteCodeForRedirect = () => {
    if (inviteCode) {
      sessionStorage.setItem('pendingInviteCode', inviteCode)
    }
  }

  /**
   * Handler para entrar com Google
   */
  const handleGoogleLogin = () => {
    saveInviteCodeForRedirect()
    window.location.href = '/api/auth/google'
  }

  /**
   * Handler para entrar com Email
   */
  const handleEmailLogin = () => {
    saveInviteCodeForRedirect()
    navigate(`/login?redirect=${encodeURIComponent(`/join/${inviteCode}`)}`)
  }

  // Loading state (initial load or joining)
  if (loading || joining) {
    return (
      <div className="nl-page px-0 py-0">
        <header className="nl-glass border-b border-nl-border/20 px-4 py-3">
          <div className="max-w-lg mx-auto flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-2xl">📋</span>
              <span className="font-display font-semibold text-nl-text">NossaLista</span>
            </div>
          </div>
        </header>
        <main className="max-w-lg mx-auto p-4">
          {joining ? (
            <div className="text-center py-12">
              <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-b-2 border-nl-accent" />
              <p className="text-nl-muted">Entrando na lista…</p>
            </div>
          ) : (
            <div className="animate-pulse">
              <div className="h-8 bg-nl-surface-strong rounded mb-4 w-2/3" />
              <div className="h-4 bg-nl-surface-strong rounded mb-6 w-1/2" />
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-14 bg-nl-bg-soft rounded-lg" />
                ))}
              </div>
            </div>
          )}
        </main>
      </div>
    )
  }

  // Error 404 state
  if (error === 'not_found') {
    return (
      <div className="nl-page flex flex-col px-0 py-0">
        <header className="nl-glass border-b border-nl-border/20 px-4 py-3">
          <div className="max-w-lg mx-auto flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-2xl">📋</span>
              <span className="font-semibold text-nl-text">NossaLista</span>
            </div>
          </div>
        </header>
        <main className="flex-1 flex items-center justify-center p-4">
          <div className="text-center max-w-md">
            <div className="text-6xl mb-4">🔗</div>
            <h1 className="text-2xl font-bold text-nl-text mb-2">Convite não encontrado</h1>
            <p className="text-nl-muted mb-6">Este link pode ter sido desativado ou não existe.</p>
            <Link
              to="/"
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-6 py-3 text-white transition-colors hover:from-orange-600 hover:to-amber-600"
            >
              Ir para página inicial
            </Link>
          </div>
        </main>
      </div>
    )
  }

  // Error 410 state
  if (error === 'expired') {
    return (
      <div className="nl-page flex flex-col px-0 py-0">
        <header className="nl-glass border-b border-nl-border/20 px-4 py-3">
          <div className="max-w-lg mx-auto flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-2xl">📋</span>
              <span className="font-semibold text-nl-text">NossaLista</span>
            </div>
          </div>
        </header>
        <main className="flex-1 flex items-center justify-center p-4">
          <div className="text-center max-w-md">
            <div className="text-6xl mb-4">⏰</div>
            <h1 className="text-2xl font-bold text-nl-text mb-2">Link de convite expirado</h1>
            <p className="text-nl-muted mb-6">
              Este link de convite expirou. Peça um novo link ao dono da lista.
            </p>
            <Link
              to="/"
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-6 py-3 text-white transition-colors hover:from-orange-600 hover:to-amber-600"
            >
              Ir para página inicial
            </Link>
          </div>
        </main>
      </div>
    )
  }

  // Generic error state
  if (error === 'generic') {
    return (
      <div className="nl-page flex flex-col px-0 py-0">
        <header className="nl-glass border-b border-nl-border/20 px-4 py-3">
          <div className="max-w-lg mx-auto flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-2xl">📋</span>
              <span className="font-semibold text-nl-text">NossaLista</span>
            </div>
          </div>
        </header>
        <main className="flex-1 flex items-center justify-center p-4">
          <div className="text-center max-w-md">
            <div className="text-6xl mb-4">⚠️</div>
            <h1 className="text-2xl font-bold text-nl-text mb-2">Erro ao carregar lista</h1>
            <p className="text-nl-muted mb-6">{errorMessage}</p>
            <button
              onClick={() => window.location.reload()}
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-6 py-3 text-white transition-colors hover:from-orange-600 hover:to-amber-600"
            >
              Tentar novamente
            </button>
          </div>
        </main>
      </div>
    )
  }

  // Read-only view
  if (!listData) return null

  const expiringSoon = isExpiringSoon()

  return (
    <div className="nl-page flex flex-col px-0 py-0">
      {/* Header */}
      <header className="nl-glass border-b border-nl-border/20 px-4 py-3">
        <div className="max-w-lg mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-2xl">📋</span>
            <span className="font-display font-semibold text-nl-text">NossaLista</span>
          </div>
          <button
            onClick={handleEmailLogin}
            className="text-sm font-medium text-nl-primary hover:text-nl-primary"
          >
            Entrar
          </button>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 max-w-lg mx-auto w-full p-4 pb-32">
        {/* Badge modo leitura */}
        <div className="mb-4">
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm font-medium">
            <span>🔒</span>
            Modo Leitura
          </span>
        </div>

        {/* Lista info */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-nl-text mb-1 flex items-center gap-2">
            <span>{getTypeEmoji(listData.type_slug)}</span>
            {listData.name}
          </h1>
          <p className="text-nl-muted text-sm">
            {listData.type_name} • por @{listData.owner_username}
          </p>
        </div>

        {/* Aviso modo leitura */}
        <div className="mb-4 rounded-xl border border-nl-border/20 bg-nl-primary/15 p-4" role="alert">
          <p className="text-sm text-nl-primary">
            <span className="font-medium">📖 Você está visualizando em modo leitura.</span>
            <br />
            Entre para colaborar com esta lista!
          </p>
        </div>

        {/* Aviso expiração próxima */}
        {expiringSoon && (
          <div className="mb-4 bg-nl-bg-soft border border-nl-border/20 rounded-xl p-4">
            <p className="text-nl-accent text-sm">
              <span className="font-medium">⚠️ Este link expira em breve!</span>
              <br />
              Entre agora ou peça um novo link.
            </p>
          </div>
        )}

        {/* Lista de itens */}
        <div className="nl-card rounded-xl border shadow-sm">
          <div className="rounded-t-xl border-b bg-nl-bg-soft px-4 py-3">
            <h2 className="font-medium text-nl-text">Itens ({listData.items.length})</h2>
          </div>

          {listData.items.length === 0 ? (
            <div className="p-8 text-center text-nl-muted">
              <p>Esta lista ainda não tem itens.</p>
            </div>
          ) : (
            <ul className="divide-y">
              {listData.items.map((item: JoinListItem) => (
                <li
                  key={item.id}
                  className={`px-4 py-3 flex items-center gap-3 ${
                    item.checked ? 'opacity-60' : ''
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={item.checked}
                    disabled
                    className="h-5 w-5 cursor-not-allowed rounded border-nl-border/20 text-nl-primary"
                    aria-disabled="true"
                  />
                  <span
                    className={`flex-1 ${
                      item.checked ? 'line-through text-nl-muted' : 'text-nl-text'
                    }`}
                  >
                    {item.name}
                  </span>
                  {item.quantity && <span className="text-sm text-nl-muted">×{item.quantity}</span>}
                </li>
              ))}
            </ul>
          )}
        </div>
      </main>

      {/* Rodapé fixo com CTAs */}
      <footer className="fixed bottom-0 left-0 right-0 border-t border-nl-border/20 bg-nl-surface/95 px-4 py-4 backdrop-blur-sm">
        <div className="max-w-lg mx-auto space-y-3">
          <p className="text-center text-sm text-nl-muted mb-3">
            Entre para participar desta lista
          </p>
          <button
            onClick={handleGoogleLogin}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-nl-primary to-nl-primary-strong px-4 py-3 font-medium text-white transition-colors hover:from-nl-primary-strong hover:to-nl-primary-strong"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path
                fill="currentColor"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="currentColor"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="currentColor"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
              />
              <path
                fill="currentColor"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
              />
            </svg>
            Entrar com Google
          </button>
          <button
            onClick={handleEmailLogin}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-nl-border/20 bg-nl-surface px-4 py-3 font-medium text-nl-text transition-colors hover:bg-nl-bg-soft"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
              />
            </svg>
            Entrar com Email
          </button>
        </div>
      </footer>
    </div>
  )
}
