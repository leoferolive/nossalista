import { useCallback, useEffect, useState } from 'react'
import { useToast } from '../contexts/ToastContext'
import { oauthConnectionsApi, type OAuthConnection } from '../api/oauthConnectionsApi'
import { DisconnectOAuthModal } from './DisconnectOAuthModal'

function formatDate(value: string | null): string {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function scopeLabel(scope: OAuthConnection['scope']): string {
  return scope === 'READ' ? 'Somente leitura' : 'Leitura e escrita'
}

function useOAuthConnectionsManager() {
  const { showToast } = useToast()
  const [connections, setConnections] = useState<OAuthConnection[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadConnections = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      setConnections(await oauthConnectionsApi.list())
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar conexões'
      setError(message)
      showToast(message, 'error')
    } finally {
      setLoading(false)
    }
  }, [showToast])

  useEffect(() => {
    loadConnections()
  }, [loadConnections])

  const disconnect = useCallback(
    async (clientId: string): Promise<boolean> => {
      try {
        await oauthConnectionsApi.revoke(clientId)
        setConnections((prev) => prev.filter((c) => c.clientId !== clientId))
        showToast('Assistente desconectado.', 'success')
        return true
      } catch (err) {
        const message =
          err instanceof Error ? err.message : 'Erro ao desconectar assistente. Tente novamente.'
        showToast(message, 'error')
        return false
      }
    },
    [showToast]
  )

  return { connections, loading, error, loadConnections, disconnect }
}

function useOAuthDisconnectModal(disconnect: (clientId: string) => Promise<boolean>) {
  const [pendingConnection, setPendingConnection] = useState<OAuthConnection | null>(null)
  const [disconnecting, setDisconnecting] = useState(false)

  const handleDisconnect = async () => {
    if (!pendingConnection) return
    setDisconnecting(true)
    const success = await disconnect(pendingConnection.clientId)
    setDisconnecting(false)
    if (success) setPendingConnection(null)
  }

  return {
    pendingConnection,
    requestDisconnect: setPendingConnection,
    cancelDisconnect: () => setPendingConnection(null),
    disconnecting,
    handleDisconnect,
  }
}

interface ConnectionListProps {
  connections: OAuthConnection[]
  onDisconnect: (connection: OAuthConnection) => void
}

function ConnectionList({ connections, onDisconnect }: ConnectionListProps) {
  if (connections.length === 0) {
    return (
      <div className="nl-card p-8 text-center">
        <p className="text-nl-muted">
          Nenhum assistente conectado via OAuth (claude.ai, Claude Code) ainda.
        </p>
      </div>
    )
  }

  return (
    <ul className="space-y-3" data-testid="oauth-connection-list">
      {connections.map((connection) => (
        <li key={connection.clientId} className="nl-card p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="font-display text-lg font-semibold text-nl-text">
                {connection.clientName}
              </p>
              <div className="mt-3 flex flex-wrap gap-4 text-xs text-nl-muted">
                <span className="nl-badge">{scopeLabel(connection.scope)}</span>
                <span>Conectado em {formatDate(connection.createdAt)}</span>
                <span>Último uso: {formatDate(connection.lastUsedAt)}</span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => onDisconnect(connection)}
              className="nl-btn-danger min-h-[44px]"
              aria-label={`Desconectar ${connection.clientName}`}
            >
              Desconectar
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}

/**
 * Painel autocontido "Assistentes conectados via OAuth" — lista os clientes
 * MCP (claude.ai, Claude Code) conectados via OAuth 2.1 (ver docs/mcp.md) e
 * permite desconectá-los, com o mesmo padrão visual das seções da tela
 * "Conexões (API/Assistentes)".
 */
export function OAuthConnectionsPanel() {
  const { connections, loading, error, loadConnections, disconnect } = useOAuthConnectionsManager()
  const modal = useOAuthDisconnectModal(disconnect)

  return (
    <>
      <h2 className="mb-3 mt-2 font-display text-lg font-semibold text-nl-text">
        Assistentes conectados via OAuth
      </h2>

      {loading && (
        <div className="flex items-center justify-center p-8">
          <div className="nl-spinner" role="status" aria-label="Carregando conexões" />
        </div>
      )}

      {!loading && error && (
        <div className="nl-card p-8 text-center">
          <p className="mb-2 text-xl font-bold text-nl-danger">Erro ao carregar conexões</p>
          <p className="mb-4 text-nl-danger">{error}</p>
          <button onClick={loadConnections} className="nl-btn-primary">
            Tentar novamente
          </button>
        </div>
      )}

      {!loading && !error && (
        <ConnectionList connections={connections} onDisconnect={modal.requestDisconnect} />
      )}

      <DisconnectOAuthModal
        isOpen={modal.pendingConnection !== null}
        clientName={modal.pendingConnection?.clientName ?? ''}
        onClose={modal.cancelDisconnect}
        onConfirm={modal.handleDisconnect}
        isDisconnecting={modal.disconnecting}
      />
    </>
  )
}
