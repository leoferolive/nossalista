import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { CreateTokenModal } from '../components/CreateTokenModal'
import { TokenCreatedModal } from '../components/TokenCreatedModal'
import { RevokeTokenModal } from '../components/RevokeTokenModal'
import { OAuthConnectionsPanel } from '../components/OAuthConnectionsPanel'
import { useToast } from '../contexts/ToastContext'
import {
  tokensApi,
  type CreateTokenRequest,
  type PersonalAccessToken,
  type PersonalAccessTokenCreated,
} from '../api/tokensApi'

function formatDate(value: string | null): string {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function scopeLabel(scope: PersonalAccessToken['scope']): string {
  return scope === 'READ' ? 'Somente leitura' : 'Leitura e escrita'
}

function useTokenManager() {
  const { showToast } = useToast()
  const [tokens, setTokens] = useState<PersonalAccessToken[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadTokens = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      setTokens(await tokensApi.list())
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar tokens'
      setError(message)
      showToast(message, 'error')
    } finally {
      setLoading(false)
    }
  }, [showToast])

  useEffect(() => {
    loadTokens()
  }, [loadTokens])

  const createToken = useCallback(async (data: CreateTokenRequest) => {
    const created = await tokensApi.create(data)
    // Nunca guarda o valor em claro no estado da lista além do necessário
    // para devolvê-lo ao chamador (que o exibe uma única vez no modal).
    const metadata: PersonalAccessToken = {
      id: created.id,
      name: created.name,
      prefix: created.prefix,
      scope: created.scope,
      expiresAt: created.expiresAt,
      lastUsedAt: created.lastUsedAt,
      createdAt: created.createdAt,
    }
    setTokens((prev) => [metadata, ...prev])
    return created
  }, [])

  const revokeToken = useCallback(
    async (id: string): Promise<boolean> => {
      try {
        await tokensApi.revoke(id)
        setTokens((prev) => prev.filter((t) => t.id !== id))
        showToast('Token revogado.', 'success')
        return true
      } catch (err) {
        const message =
          err instanceof Error ? err.message : 'Erro ao revogar token. Tente novamente.'
        showToast(message, 'error')
        return false
      }
    },
    [showToast]
  )

  return { tokens, loading, error, loadTokens, createToken, revokeToken }
}

interface TokenListItemProps {
  token: PersonalAccessToken
  onRevoke: (token: PersonalAccessToken) => void
}

function TokenListItem({ token, onRevoke }: TokenListItemProps) {
  return (
    <li className="nl-card p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-display text-lg font-semibold text-nl-text">{token.name}</p>
          <p className="mt-1 font-mono text-xs text-nl-muted">{token.prefix}…</p>
          <div className="mt-3 flex flex-wrap gap-4 text-xs text-nl-muted">
            <span className="nl-badge">{scopeLabel(token.scope)}</span>
            <span>Criado em {formatDate(token.createdAt)}</span>
            <span>Expira em {formatDate(token.expiresAt)}</span>
            <span>Último uso: {formatDate(token.lastUsedAt)}</span>
          </div>
        </div>

        <button
          type="button"
          onClick={() => onRevoke(token)}
          className="nl-btn-danger min-h-[44px]"
          aria-label={`Revogar token ${token.name}`}
        >
          Revogar
        </button>
      </div>
    </li>
  )
}

interface TokenListSectionProps {
  tokens: PersonalAccessToken[]
  loading: boolean
  error: string | null
  onRetry: () => void
  onRevoke: (token: PersonalAccessToken) => void
}

function TokenListSection({ tokens, loading, error, onRetry, onRevoke }: TokenListSectionProps) {
  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="nl-spinner" role="status" aria-label="Carregando tokens" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="nl-card p-8 text-center">
        <p className="mb-2 text-xl font-bold text-nl-danger">Erro ao carregar tokens</p>
        <p className="mb-4 text-nl-danger">{error}</p>
        <button onClick={onRetry} className="nl-btn-primary">
          Tentar novamente
        </button>
      </div>
    )
  }

  if (tokens.length === 0) {
    return (
      <div className="nl-card p-8 text-center">
        <p className="text-nl-muted">
          Nenhum token criado ainda. Crie um para conectar um assistente ou cliente MCP/API à sua
          conta.
        </p>
      </div>
    )
  }

  return (
    <ul className="space-y-3" data-testid="token-list">
      {tokens.map((token) => (
        <TokenListItem key={token.id} token={token} onRevoke={onRevoke} />
      ))}
    </ul>
  )
}

function useTokenModals(
  createToken: (data: CreateTokenRequest) => Promise<PersonalAccessTokenCreated>,
  revokeToken: (id: string) => Promise<boolean>
) {
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [creating, setCreating] = useState(false)
  const [createdToken, setCreatedToken] = useState<PersonalAccessTokenCreated | null>(null)
  const [tokenPendingRevoke, setTokenPendingRevoke] = useState<PersonalAccessToken | null>(null)
  const [revoking, setRevoking] = useState(false)

  const handleCreate = async (data: CreateTokenRequest) => {
    setCreating(true)
    try {
      const created = await createToken(data)
      setShowCreateModal(false)
      setCreatedToken(created)
    } finally {
      setCreating(false)
    }
  }

  const handleRevoke = async () => {
    if (!tokenPendingRevoke) return
    setRevoking(true)
    const success = await revokeToken(tokenPendingRevoke.id)
    setRevoking(false)
    if (success) setTokenPendingRevoke(null)
  }

  return {
    showCreateModal,
    openCreateModal: () => setShowCreateModal(true),
    closeCreateModal: () => setShowCreateModal(false),
    creating,
    createdToken,
    closeCreatedModal: () => setCreatedToken(null),
    tokenPendingRevoke,
    requestRevoke: setTokenPendingRevoke,
    cancelRevoke: () => setTokenPendingRevoke(null),
    revoking,
    handleCreate,
    handleRevoke,
  }
}

type TokenModalsState = ReturnType<typeof useTokenModals>

function TokenModals({ modal }: { modal: TokenModalsState }) {
  return (
    <>
      <CreateTokenModal
        isOpen={modal.showCreateModal}
        onClose={modal.closeCreateModal}
        onCreate={modal.handleCreate}
        isCreating={modal.creating}
      />

      {modal.createdToken && (
        <TokenCreatedModal token={modal.createdToken} onClose={modal.closeCreatedModal} />
      )}

      <RevokeTokenModal
        isOpen={modal.tokenPendingRevoke !== null}
        tokenName={modal.tokenPendingRevoke?.name ?? ''}
        onClose={modal.cancelRevoke}
        onConfirm={modal.handleRevoke}
        isRevoking={modal.revoking}
      />
    </>
  )
}

/**
 * Página "Conexões (API/Assistentes)": gestão de Personal Access Tokens
 * (PAT) e de assistentes conectados via OAuth (claude.ai, Claude Code) usados
 * para autenticar clientes MCP/API externos em nome da conta.
 */
export const Connections: React.FC = () => {
  const navigate = useNavigate()
  const { tokens, loading, error, loadTokens, createToken, revokeToken } = useTokenManager()
  const modal = useTokenModals(createToken, revokeToken)

  return (
    <div className="nl-page">
      <div className="nl-container max-w-3xl">
        <AppHeader
          eyebrow="NossaLista"
          title="Conexões (API/Assistentes)"
          subtitle="Gerencie os tokens de acesso pessoal e os assistentes conectados via OAuth."
          onBack={() => navigate(-1)}
          primaryAction={
            <button type="button" onClick={modal.openCreateModal} className="nl-btn-primary">
              Criar token
            </button>
          }
        />

        <OAuthConnectionsPanel />

        <h2 className="mb-3 mt-8 font-display text-lg font-semibold text-nl-text">
          Personal Access Tokens
        </h2>
        <TokenListSection
          tokens={tokens}
          loading={loading}
          error={error}
          onRetry={loadTokens}
          onRevoke={modal.requestRevoke}
        />
      </div>

      <TokenModals modal={modal} />
    </div>
  )
}
