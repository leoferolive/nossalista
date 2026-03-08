import React, { useState, useCallback, useEffect, useRef } from 'react'
import { InviteLinkResponse, InviteByUsernameResponse, UserSearchResult } from '../types/List'
import { useToast } from './Toast'

interface InviteModalProps {
  isOpen: boolean
  listName: string
  onClose: () => void
  onGenerateLink: () => Promise<InviteLinkResponse>
  onSearchUsers: (query: string) => Promise<UserSearchResult[]>
  onInviteByUsername: (username: string) => Promise<InviteByUsernameResponse>
  onInviteSuccess?: (username: string) => void
}

/**
 * Modal para convidar pessoas via link de convite
 * AC2: Seção "Convidar por Link" com botão "Gerar Link"/"Copiar Link"
 * AC3: Clipboard copy com Toast feedback
 */
export const InviteModal: React.FC<InviteModalProps> = ({
  isOpen,
  listName,
  onClose,
  onGenerateLink,
  onSearchUsers,
  onInviteByUsername,
  onInviteSuccess,
}) => {
  const [inviteData, setInviteData] = useState<InviteLinkResponse | null>(null)
  const [generating, setGenerating] = useState(false)
  const [copying, setCopying] = useState(false)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedUser, setSelectedUser] = useState<UserSearchResult | null>(null)
  const [searchResults, setSearchResults] = useState<UserSearchResult[]>([])
  const [searching, setSearching] = useState(false)
  const [inviting, setInviting] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)
  const { showToast } = useToast()
  const searchRequestRef = useRef(0)

  // Handler para gerar link
  const handleGenerateLink = useCallback(async () => {
    setGenerating(true)
    setError(null)

    try {
      const data = await onGenerateLink()
      setInviteData(data)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao gerar link'
      setError(message)
    } finally {
      setGenerating(false)
    }
  }, [onGenerateLink])

  // Handler para copiar link
  const handleCopyLink = useCallback(async () => {
    if (!inviteData) return

    setCopying(true)

    try {
      // Tenta usar a Clipboard API moderna
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(inviteData.invite_link)
      } else {
        // Fallback para browsers antigos
        const textarea = document.createElement('textarea')
        textarea.value = inviteData.invite_link
        textarea.style.position = 'fixed'
        textarea.style.left = '-999999px'
        document.body.appendChild(textarea)
        textarea.select()
        document.execCommand('copy')
        document.body.removeChild(textarea)
      }

      // Mostra estado "Copiado!"
      setCopied(true)

      // Toast de sucesso AC3: "Link copiado!"
      showToast('Link copiado!', 'success')

      // Volta para "Copiar Link" após 2 segundos
      setTimeout(() => {
        setCopied(false)
      }, 2000)
    } catch {
      const errorMessage = 'Não foi possível copiar o link'
      setError(errorMessage)
      showToast(errorMessage, 'error')
    } finally {
      setCopying(false)
    }
  }, [inviteData, showToast])

  // Fecha modal e reseta estado
  const handleClose = useCallback(() => {
    searchRequestRef.current += 1
    setInviteData(null)
    setError(null)
    setCopied(false)
    setSearchQuery('')
    setSelectedUser(null)
    setSearchResults([])
    setSearchError(null)
    onClose()
  }, [onClose])

  useEffect(() => {
    if (!isOpen) {
      return
    }

    if (searchQuery.trim().length < 2) {
      searchRequestRef.current += 1
      setSearching(false)
      setSearchResults([])
      setSearchError(null)
      return
    }

    if (selectedUser && searchQuery === selectedUser.username) {
      return
    }

    const timeoutId = window.setTimeout(async () => {
      const requestId = ++searchRequestRef.current
      setSearching(true)
      setSearchError(null)

      try {
        const results = await onSearchUsers(searchQuery.trim())

        if (requestId !== searchRequestRef.current) {
          return
        }

        setSearchResults(results)
      } catch (err) {
        if (requestId !== searchRequestRef.current) {
          return
        }

        const message = err instanceof Error ? err.message : 'Erro ao buscar usuários'
        setSearchError(message)
        setSearchResults([])
      } finally {
        if (requestId === searchRequestRef.current) {
          setSearching(false)
        }
      }
    }, 350)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [isOpen, onSearchUsers, searchQuery, selectedUser])

  const handleSelectUser = useCallback((user: UserSearchResult) => {
    setSelectedUser(user)
    setSearchQuery(user.username)
    setSearchResults([])
    setSearchError(null)
  }, [])

  const handleInviteByUsername = useCallback(async () => {
    if (!selectedUser) {
      return
    }

    setInviting(true)
    setError(null)

    try {
      await onInviteByUsername(selectedUser.username)
      showToast(`${selectedUser.username} convidado!`, 'success')
      onInviteSuccess?.(selectedUser.username)
      handleClose()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao convidar usuário'
      setError(message)
      showToast(message, 'error')
    } finally {
      setInviting(false)
    }
  }, [handleClose, onInviteByUsername, onInviteSuccess, selectedUser, showToast])

  // Fecha ao clicar no backdrop
  const handleBackdropClick = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (e.target === e.currentTarget) {
        handleClose()
      }
    },
    [handleClose]
  )

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="invite-modal-title"
    >
      <div
        className="nl-card w-full max-w-md animate-scale-in p-6"
        style={{ overscrollBehavior: 'contain' }}
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <h2 id="invite-modal-title" className="font-display text-xl font-bold text-nl-text">
            Convidar
          </h2>
          <button
            onClick={handleClose}
            className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl p-2 text-nl-muted transition-colors hover:bg-nl-bg-soft hover:text-nl-text focus-visible:ring-2 focus-visible:ring-nl-focus/40"
            aria-label="Fechar modal"
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
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Seção Convidar por Username */}
        <div className="space-y-4 mb-6">
          <h3 className="text-xs font-semibold uppercase tracking-[0.18em] text-nl-primary">
            Convidar por Username
          </h3>

          <div className="relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value)
                setSelectedUser(null)
              }}
              placeholder="Buscar usuário…"
              className="w-full rounded-xl border border-nl-border/20 px-4 py-3 text-sm text-nl-text transition-colors focus:border-nl-focus/40 focus-visible:ring-2 focus-visible:ring-nl-focus/40"
              autoComplete="off"
              spellCheck={false}
            />

            {searching && (
              <p className="mt-2 text-xs text-nl-muted" role="status" aria-live="polite">
                Buscando…
              </p>
            )}

            {searchError && (
              <p className="mt-2 text-xs text-nl-danger" role="alert">
                {searchError}
              </p>
            )}

            {searchResults.length > 0 && (
              <div
                className="absolute z-20 mt-2 max-h-56 w-full overflow-y-auto rounded-xl border border-nl-border/20 bg-nl-surface shadow-earthen"
                style={{ overscrollBehavior: 'contain' }}
              >
                {searchResults.map((user) => (
                  <button
                    key={user.username}
                    type="button"
                    onClick={() => handleSelectUser(user)}
                    className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-nl-bg-soft focus-visible:ring-2 focus-visible:ring-nl-focus/40"
                  >
                    {user.avatarUrl ? (
                      <img
                        src={user.avatarUrl}
                        alt={user.username}
                        className="h-8 w-8 rounded-full object-cover"
                        width={32}
                        height={32}
                      />
                    ) : (
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-nl-bg-soft text-xs text-nl-accent">
                        {user.username.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                    <div>
                      <p className="text-sm font-medium text-nl-text">{user.username}</p>
                      <p className="text-xs text-nl-muted">{user.name}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          <button
            type="button"
            onClick={handleInviteByUsername}
            disabled={!selectedUser || inviting}
            className="w-full min-h-[48px] rounded-xl bg-gradient-to-r from-nl-primary to-nl-primary-strong px-6 py-3 font-semibold text-white shadow-md transition-transform hover:-translate-y-0.5 hover:from-nl-primary-strong hover:to-nl-primary-strong focus-visible:ring-2 focus-visible:ring-nl-focus/40 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {inviting ? 'Convidando…' : 'Convidar'}
          </button>
        </div>

        {/* Seção Convidar por Link */}
        <div className="space-y-4">
          <h3 className="text-xs font-semibold uppercase tracking-[0.18em] text-nl-accent">
            Convidar por Link
          </h3>

          {/* Lista sendo compartilhada */}
          <p className="text-sm text-nl-muted">
            Compartilhar lista: <span className="font-medium text-nl-text">{listName}</span>
          </p>

          {/* Error message */}
          {error && (
            <div className="rounded-xl border border-nl-danger/30 bg-nl-danger/10 p-3" role="alert">
              <p className="text-nl-danger text-sm">{error}</p>
            </div>
          )}

          {/* Link display area (quando gerado) */}
          {inviteData && (
            <div className="space-y-3">
              {/* Link em área de display */}
              <div className="rounded-xl border border-nl-border/20 bg-nl-bg-soft px-4 py-3">
                <p className="mb-1 text-xs text-nl-muted">Link de convite:</p>
                <p className="break-all font-mono text-sm text-nl-text">
                  {inviteData.invite_link}
                </p>
              </div>

              {/* Tempo de expiração */}
              <p className="flex items-center gap-1 text-xs text-nl-muted">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
                Expira em 24 horas
              </p>
            </div>
          )}

          {/* Botão de ação */}
          {!inviteData ? (
            // Estado 1: Gerar Link
            <button
              onClick={handleGenerateLink}
              disabled={generating}
              className="flex w-full min-h-[48px] items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-6 py-3 font-semibold text-white shadow-md transition-transform hover:-translate-y-0.5 hover:from-orange-600 hover:to-amber-600 focus-visible:ring-2 focus-visible:ring-nl-focus/40 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {generating ? (
                <>
                  <svg
                    className="animate-spin h-5 w-5"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    />
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    />
                  </svg>
                  Gerando…
                </>
              ) : (
                <>
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
                      strokeWidth={2}
                      d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"
                    />
                  </svg>
                  Gerar Link
                </>
              )}
            </button>
          ) : copied ? (
            // Estado 3: Copiado!
            <button
              disabled
              className="flex w-full min-h-[48px] items-center justify-center gap-2 rounded-xl bg-teal-600 px-6 py-3 font-semibold text-white"
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
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
              Copiado!
            </button>
          ) : (
            // Estado 2: Copiar Link
            <button
              onClick={handleCopyLink}
              disabled={copying}
              className="flex w-full min-h-[48px] items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-6 py-3 font-semibold text-white shadow-md transition-transform hover:-translate-y-0.5 hover:from-orange-600 hover:to-amber-600 focus-visible:ring-2 focus-visible:ring-nl-focus/40 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {copying ? (
                <>
                  <svg
                    className="animate-spin h-5 w-5"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    />
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    />
                  </svg>
                  Copiando…
                </>
              ) : (
                <>
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
                      strokeWidth={2}
                      d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
                    />
                  </svg>
                  Copiar Link
                </>
              )}
            </button>
          )}

          {/* Dica */}
          <p className="text-center text-xs text-nl-muted">
            Qualquer pessoa com o link pode visualizar a lista por 24 horas.
          </p>
        </div>
      </div>
    </div>
  )
}
