import { useState } from 'react'

interface CopyableCodeProps {
  code: string
  label?: string
}

/**
 * Bloco de código com botão de copiar, mesmo padrão de clipboard usado em
 * {@link TokenCreatedModal} (Clipboard API com fallback via
 * `document.execCommand('copy')` para navegadores/contextos sem suporte).
 */
export function CopyableCode({ code, label }: CopyableCodeProps) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    try {
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(code)
      } else {
        const textarea = document.createElement('textarea')
        textarea.value = code
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
    <div className="overflow-hidden rounded-[1.2rem] border border-nl-border bg-nl-surface">
      {label ? (
        <p className="border-b border-nl-border px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em] text-nl-muted">
          {label}
        </p>
      ) : null}
      <div className="flex items-start justify-between gap-3 p-4">
        <pre className="min-w-0 flex-1 overflow-x-auto whitespace-pre-wrap break-all font-mono text-sm text-nl-text">
          {code}
        </pre>
        <button
          type="button"
          onClick={handleCopy}
          className="nl-btn-secondary shrink-0 min-h-[36px] px-3 py-1.5 text-xs"
          aria-label="Copiar comando"
        >
          {copied ? 'Copiado!' : 'Copiar'}
        </button>
      </div>
    </div>
  )
}
