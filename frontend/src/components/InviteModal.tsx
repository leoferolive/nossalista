import React, { useState, useCallback, useEffect, useRef } from 'react'
import { InviteLinkResponse, InviteByUsernameResponse, UserSearchResult } from '../types/List'
import { useToast } from '../contexts/ToastContext'
import { ModalShell } from './ModalShell'

interface InviteModalProps {
  isOpen: boolean
  listName: string
  onClose: () => void
  onGenerateLink: () => Promise<InviteLinkResponse>
  onSearchUsers: (query: string) => Promise<UserSearchResult[]>
  onInviteByUsername: (username: string) => Promise<InviteByUsernameResponse>
  onInviteSuccess?: (username: string) => void
}

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

  const handleCopyLink = useCallback(async () => {
    if (!inviteData) return

    setCopying(true)

    try {
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(inviteData.inviteLink)
      } else {
        const textarea = document.createElement('textarea')
        textarea.value = inviteData.inviteLink
        textarea.style.position = 'fixed'
        textarea.style.left = '-999999px'
        document.body.appendChild(textarea)
        textarea.select()
        document.execCommand('copy')
        document.body.removeChild(textarea)
      }

      setCopied(true)
      showToast('Link copiado!', 'success')

      setTimeout(() => {
        setCopied(false)
      }, 2000)
    } catch {
      const errorMessage = 'Nao foi possivel copiar o link'
      setError(errorMessage)
      showToast(errorMessage, 'error')
    } finally {
      setCopying(false)
    }
  }, [inviteData, showToast])

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

        const message = err instanceof Error ? err.message : 'Erro ao buscar usuarios'
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
      const message = err instanceof Error ? err.message : 'Erro ao convidar usuario'
      setError(message)
      showToast(message, 'error')
    } finally {
      setInviting(false)
    }
  }, [handleClose, onInviteByUsername, onInviteSuccess, selectedUser, showToast])

  if (!isOpen) return null

  return (
    <ModalShell
      title="Convidar Pessoas"
      eyebrow="Compartilhamento"
      description={`Compartilhe ${listName} por username ou por link, sem etapas desnecessarias.`}
      onClose={handleClose}
    >
      <div className="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="nl-card-soft p-5">
          <p className="nl-label">Convidar por username</p>

          <div className="relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value)
                setSelectedUser(null)
              }}
              placeholder="Buscar usuario..."
              className="nl-input"
              autoComplete="off"
              spellCheck={false}
            />

            {searching && (
              <p className="nl-helper" role="status" aria-live="polite">
                Buscando...
              </p>
            )}

            {searchError && (
              <p className="nl-helper nl-helper-error" role="alert">
                {searchError}
              </p>
            )}

            {searchResults.length > 0 && (
              <div
                className="absolute z-20 mt-2 max-h-56 w-full overflow-y-auto rounded-2xl border border-nl-border bg-nl-surface-strong p-2 shadow-earthen-strong"
                style={{ overscrollBehavior: 'contain' }}
              >
                {searchResults.map((user) => (
                  <button
                    key={user.username}
                    type="button"
                    onClick={() => handleSelectUser(user)}
                    className="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left transition-colors hover:bg-nl-surface-muted/50"
                  >
                    {user.avatarUrl ? (
                      <img
                        src={user.avatarUrl}
                        alt={user.username}
                        className="h-9 w-9 rounded-full object-cover"
                        width={36}
                        height={36}
                      />
                    ) : (
                      <div className="flex h-9 w-9 items-center justify-center rounded-full border border-nl-border bg-nl-surface text-xs font-semibold text-nl-accent">
                        {user.username.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                    <div>
                      <p className="text-sm font-semibold text-nl-text">{user.username}</p>
                      <p className="text-xs text-nl-muted">{user.name}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          <p className="nl-helper">
            O convite por username funciona melhor para quem ja usa o NossaLista.
          </p>

          <button
            type="button"
            onClick={handleInviteByUsername}
            disabled={!selectedUser || inviting}
            className="nl-btn-primary mt-5 w-full"
          >
            {inviting ? 'Convidando...' : 'Convidar por username'}
          </button>
        </section>

        <section className="nl-card-soft p-5">
          <p className="nl-label">Convidar por link</p>
          <p className="text-sm leading-7 text-nl-muted">
            Gere um link temporario para abrir a lista com rapidez, mesmo quando a pessoa ainda nao
            esta no fluxo da sua equipe.
          </p>

          {error && (
            <div className="nl-alert mt-4" role="alert">
              {error}
            </div>
          )}

          {inviteData && (
            <div className="mt-4 rounded-[1.2rem] border border-nl-border bg-nl-surface p-4">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-nl-muted">
                Link ativo
              </p>
              <p className="mt-2 break-all font-mono text-sm text-nl-text">
                {inviteData.inviteLink}
              </p>
              <p className="mt-3 text-xs text-nl-muted">Expira em 24 horas.</p>
            </div>
          )}

          {!inviteData ? (
            <button
              onClick={handleGenerateLink}
              disabled={generating}
              className="nl-btn-secondary mt-5 w-full"
            >
              {generating ? 'Gerando...' : 'Gerar Link'}
            </button>
          ) : copied ? (
            <button disabled className="nl-btn-secondary mt-5 w-full">
              Copiado!
            </button>
          ) : (
            <button
              onClick={handleCopyLink}
              disabled={copying}
              className="nl-btn-primary mt-5 w-full"
            >
              {copying ? 'Copiando...' : 'Copiar Link'}
            </button>
          )}

          <p className="mt-4 text-center text-xs text-nl-muted">
            Qualquer pessoa com o link pode visualizar a lista por 24 horas.
          </p>
        </section>
      </div>
    </ModalShell>
  )
}
