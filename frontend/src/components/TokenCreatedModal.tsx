import { useState } from 'react'
import { ModalShell } from './ModalShell'
import type { PersonalAccessTokenCreated } from '../api/tokensApi'

interface TokenCreatedModalProps {
  token: PersonalAccessTokenCreated
  onClose: () => void
}

/**
 * Exibe o valor em claro de um Personal Access Token recém-criado. Esta é a
 * ÚNICA vez em que o valor aparece — o backend não o persiste, apenas o
 * hash, então fechar este modal sem copiar significa ter que gerar um novo
 * token.
 */
export function TokenCreatedModal({ token, onClose }: TokenCreatedModalProps) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    try {
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(token.token)
      } else {
        const textarea = document.createElement('textarea')
        textarea.value = token.token
        textarea.style.position = 'fixed'
        textarea.style.left = '-999999px'
        document.body.appendChild(textarea)
        textarea.select()
        document.execCommand('copy')
        document.body.removeChild(textarea)
      }
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // Falha silenciosa: o usuário ainda pode selecionar e copiar manualmente.
    }
  }

  return (
    <ModalShell
      title="Token criado"
      eyebrow="Conexões"
      description={`"${token.name}" foi criado. Copie o token agora — ele não será mostrado novamente.`}
      onClose={onClose}
    >
      <div className="space-y-5">
        <div className="nl-alert" role="alert">
          Este é o único momento em que o valor completo do token é exibido. Guarde-o em um lugar
          seguro (ex.: gerenciador de segredos do seu cliente MCP).
        </div>

        <div className="rounded-[1.2rem] border border-nl-border bg-nl-surface p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-nl-muted">
            Token de acesso
          </p>
          <p className="mt-2 break-all font-mono text-sm text-nl-text" data-testid="token-value">
            {token.token}
          </p>
        </div>

        {copied ? (
          <button disabled className="nl-btn-secondary w-full min-h-[44px]">
            Copiado!
          </button>
        ) : (
          <button onClick={handleCopy} className="nl-btn-primary w-full min-h-[44px]">
            Copiar token
          </button>
        )}

        <button onClick={onClose} className="nl-btn w-full min-h-[44px]">
          Já copiei, fechar
        </button>
      </div>
    </ModalShell>
  )
}
