import { useEffect, useRef, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { listsApi } from '../api/listsApi'
import { JoinListResponse, JoinListItem, LIST_TYPES } from '../types/List'
import { ApiError } from '../types/ApiError'
import { useAuth } from '../contexts/AuthContext'
import { GoogleAuthButton } from '../components/GoogleAuthButton'

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
  const joiningRef = useRef(false)

  // Se o usuário já está autenticado e a lista carregou, entrar automaticamente
  useEffect(() => {
    if (isAuthenticated && inviteCode && listData && !loading && !error && !joiningRef.current) {
      joiningRef.current = true
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
          joiningRef.current = false
          setJoining(false)
        })
    }
  }, [isAuthenticated, inviteCode, listData, loading, error, navigate])

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
    if (!listData?.expiresAt) return false
    const now = new Date()
    const expiresAt = new Date(listData.expiresAt)
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
    navigate('/?auth=login')
  }

  // Loading state (initial load or joining)
  if (loading || joining) {
    return (
      <div className="nl-page px-0 py-0">
        <header className="nl-glass border-b border-nl-border px-4 py-3">
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
              <div className="nl-spinner mx-auto mb-4" />
              <p className="text-nl-muted">Entrando na lista…</p>
            </div>
          ) : (
            <div>
              <div className="h-8 nl-skeleton mb-4 w-2/3" />
              <div className="h-4 nl-skeleton mb-6 w-1/2" />
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-14 nl-skeleton" />
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
        <header className="nl-glass border-b border-nl-border px-4 py-3">
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
            <Link to="/" className="nl-btn-primary">
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
        <header className="nl-glass border-b border-nl-border px-4 py-3">
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
            <Link to="/" className="nl-btn-primary">
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
        <header className="nl-glass border-b border-nl-border px-4 py-3">
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
            <button onClick={() => window.location.reload()} className="nl-btn-primary">
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
      <header className="nl-glass border-b border-nl-border px-4 py-3">
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
            <span>{getTypeEmoji(listData.typeSlug)}</span>
            {listData.name}
          </h1>
          <p className="text-nl-muted text-sm">
            {listData.typeName} • por @{listData.ownerUsername}
          </p>
        </div>

        {/* Aviso modo leitura */}
        <div
          className="mb-4 rounded-xl border border-nl-primary/30 bg-nl-primary/10 p-4"
          role="alert"
        >
          <p className="text-sm text-nl-primary">
            <span className="font-medium">📖 Você está visualizando em modo leitura.</span>
            <br />
            Entre para colaborar com esta lista!
          </p>
        </div>

        {/* Aviso expiração próxima */}
        {expiringSoon && (
          <div className="mb-4 bg-nl-surface-strong border border-nl-border rounded-xl p-4">
            <p className="text-nl-accent text-sm">
              <span className="font-medium">⚠️ Este link expira em breve!</span>
              <br />
              Entre agora ou peça um novo link.
            </p>
          </div>
        )}

        {/* Lista de itens */}
        <div className="nl-card rounded-xl border shadow-earthen">
          <div className="rounded-t-xl border-b bg-nl-surface-strong px-4 py-3">
            <h2 className="font-medium text-nl-muted">Itens ({listData.items.length})</h2>
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
                    className="h-5 w-5 cursor-not-allowed rounded border-nl-border text-nl-primary"
                    aria-disabled="true"
                  />
                  <span
                    className={`flex-1 ${
                      item.checked ? 'line-through text-nl-muted' : 'text-nl-text'
                    }`}
                  >
                    {item.name}
                  </span>
                  {item.quantity != null && <span className="text-sm text-nl-muted">×{item.quantity}</span>}
                </li>
              ))}
            </ul>
          )}
        </div>
      </main>

      {/* Rodapé fixo com CTAs */}
      <footer className="fixed bottom-0 left-0 right-0 border-t border-nl-border bg-nl-surface-strong/95 px-4 py-4 backdrop-blur-sm">
        <div className="max-w-lg mx-auto space-y-3">
          <p className="text-center text-sm text-nl-muted mb-3">
            Entre para participar desta lista
          </p>
          <GoogleAuthButton
            onClick={handleGoogleLogin}
            label="Entrar com Google"
            className="w-full"
          />
          <button
            onClick={handleEmailLogin}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-nl-border bg-nl-surface-strong px-4 py-3 font-medium text-nl-muted transition-colors hover:bg-nl-surface-strong"
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
