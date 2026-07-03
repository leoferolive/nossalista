import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { oauthConsentApi, type OAuthScope, type PendingAuthorization } from '../api/oauthConsentApi'
import { ApiError } from '../types/ApiError'

function scopeLabel(scope: OAuthScope): string {
  return scope === 'READ' ? 'Somente leitura' : 'Leitura e escrita'
}

function scopeDescription(scope: OAuthScope): string {
  return scope === 'READ'
    ? 'Poderá visualizar suas listas, itens e atividade — sem criar, editar ou excluir nada.'
    : 'Poderá visualizar e também criar, editar, marcar e excluir itens e listas em seu nome.'
}

function errorMessageFor(err: unknown): string {
  if (err instanceof ApiError) {
    return err.status === 404
      ? 'Este pedido de autorização não existe mais, já foi respondido ou expirou.'
      : err.message
  }
  return 'Não foi possível carregar o pedido de autorização.'
}

/**
 * Busca o pedido de autorização pendente e expõe a ação de decidir
 * (aprovar/negar), navegando o browser para a URL de retorno do cliente OAuth.
 */
function usePendingAuthorization(requestId: string | null) {
  const [pending, setPending] = useState<PendingAuthorization | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deciding, setDeciding] = useState(false)

  useEffect(() => {
    if (!requestId) {
      setError('Pedido de autorização inválido.')
      setLoading(false)
      return
    }

    let cancelled = false
    oauthConsentApi
      .get(requestId)
      .then((data) => {
        if (!cancelled) setPending(data)
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessageFor(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [requestId])

  const decide = async (action: (id: string) => Promise<{ redirectUrl: string }>) => {
    if (!requestId) return
    setDeciding(true)
    try {
      const { redirectUrl } = await action(requestId)
      window.location.href = redirectUrl
    } catch (err) {
      setDeciding(false)
      setError(
        err instanceof ApiError
          ? err.message
          : 'Não foi possível concluir a autorização. Tente novamente.'
      )
    }
  }

  return { pending, loading, error, deciding, decide }
}

function ConsentStatusCard({ title, message }: { title: string; message: string }) {
  return (
    <div className="nl-page flex items-center justify-center p-4">
      <div className="nl-card w-full max-w-md p-6 text-center">
        <h1 className="mb-2 font-display text-xl font-bold text-nl-text">{title}</h1>
        <p className="text-nl-muted">{message}</p>
      </div>
    </div>
  )
}

interface ConsentCardProps {
  pending: PendingAuthorization
  deciding: boolean
  onDecide: (action: (id: string) => Promise<{ redirectUrl: string }>) => void
}

function ConsentCard({ pending, deciding, onDecide }: ConsentCardProps) {
  return (
    <div className="nl-page flex items-center justify-center p-4">
      <div className="nl-card w-full max-w-md p-6">
        <h1 className="mb-1 font-display text-xl font-bold text-nl-text">Conectar assistente</h1>
        <p className="mb-5 text-nl-muted">
          <strong className="text-nl-text">{pending.clientName}</strong> quer se conectar à sua
          conta NossaLista.
        </p>

        <div className="nl-card mb-5 border border-nl-border bg-nl-surface-muted p-4">
          <p className="mb-1 text-sm font-semibold text-nl-text">Acesso solicitado</p>
          <p className="nl-badge mb-2 inline-block">{scopeLabel(pending.scope)}</p>
          <p className="text-sm text-nl-muted">{scopeDescription(pending.scope)}</p>
        </div>

        <p className="mb-6 text-xs text-nl-muted">
          Você será redirecionado para <strong>{pending.redirectUriHost}</strong> após decidir.
        </p>

        <div className="flex flex-col gap-3 sm:flex-row-reverse">
          <button
            type="button"
            className="nl-btn-primary flex-1"
            disabled={deciding}
            onClick={() => onDecide(oauthConsentApi.approve)}
          >
            Autorizar
          </button>
          <button
            type="button"
            className="nl-btn-secondary flex-1"
            disabled={deciding}
            onClick={() => onDecide(oauthConsentApi.deny)}
          >
            Negar
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Tela de consentimento OAuth 2.1 para clientes do servidor MCP (claude.ai,
 * Claude Code) — ver docs/mcp.md. Protegida por `ProtectedRoute`: usuários não
 * logados são redirecionados ao login e voltam para cá automaticamente.
 */
export function OAuthConsent() {
  const [searchParams] = useSearchParams()
  const requestId = searchParams.get('request_id')
  const { pending, loading, error, deciding, decide } = usePendingAuthorization(requestId)

  if (loading) {
    return <ConsentStatusCard title="Carregando…" message="Carregando pedido de autorização…" />
  }

  if (error || !pending) {
    return (
      <ConsentStatusCard
        title="Não foi possível continuar"
        message={error ?? 'Pedido de autorização inválido.'}
      />
    )
  }

  return <ConsentCard pending={pending} deciding={deciding} onDecide={decide} />
}
