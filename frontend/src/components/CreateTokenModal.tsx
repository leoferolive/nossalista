import { useState } from 'react'
import { ModalShell } from './ModalShell'
import type { CreateTokenRequest, TokenScope } from '../api/tokensApi'

interface CreateTokenModalProps {
  isOpen: boolean
  onClose: () => void
  onCreate: (data: CreateTokenRequest) => Promise<void>
  isCreating?: boolean
}

const EXPIRATION_OPTIONS: { value: string; label: string }[] = [
  { value: '30', label: '30 dias' },
  { value: '90', label: '90 dias' },
  { value: '365', label: '1 ano' },
  { value: 'never', label: 'Sem expiração' },
]

function useCreateTokenForm(onCreate: (data: CreateTokenRequest) => Promise<void>) {
  const [name, setName] = useState('')
  const [scope, setScope] = useState<TokenScope>('READ')
  const [expiresInDays, setExpiresInDays] = useState('30')
  const [error, setError] = useState('')

  const reset = () => {
    setName('')
    setScope('READ')
    setExpiresInDays('30')
    setError('')
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')

    const trimmedName = name.trim()
    if (trimmedName.length === 0) {
      setError('Dê um nome ao token, ex.: "Claude Desktop".')
      return
    }

    try {
      await onCreate({
        name: trimmedName,
        scope,
        expiresInDays: expiresInDays === 'never' ? undefined : Number(expiresInDays),
      })
      reset()
    } catch (submitError) {
      const message =
        submitError instanceof Error ? submitError.message : 'Não foi possível criar o token.'
      setError(message)
    }
  }

  return {
    name,
    setName,
    scope,
    setScope,
    expiresInDays,
    setExpiresInDays,
    error,
    reset,
    handleSubmit,
  }
}

interface FieldProps<T extends string> {
  value: T
  onChange: (value: T) => void
  disabled: boolean
}

function TokenNameField({ value, onChange, disabled }: FieldProps<string>) {
  return (
    <div>
      <label htmlFor="token-name" className="nl-label">
        Nome
      </label>
      <input
        id="token-name"
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Ex.: Claude Desktop"
        className="nl-input"
        maxLength={100}
        autoFocus
        disabled={disabled}
      />
    </div>
  )
}

function TokenScopeField({ value, onChange, disabled }: FieldProps<TokenScope>) {
  return (
    <div>
      <label htmlFor="token-scope" className="nl-label">
        Escopo de acesso
      </label>
      <select
        id="token-scope"
        value={value}
        onChange={(e) => onChange(e.target.value as TokenScope)}
        className="nl-select"
        disabled={disabled}
      >
        <option value="READ">Somente leitura</option>
        <option value="READ_WRITE">Leitura e escrita</option>
      </select>
      <p className="nl-helper">
        {value === 'READ'
          ? 'Este token só poderá consultar dados — nenhuma criação, edição ou exclusão.'
          : 'Este token poderá criar, editar e excluir dados em seu nome.'}
      </p>
    </div>
  )
}

function TokenExpirationField({ value, onChange, disabled }: FieldProps<string>) {
  return (
    <div>
      <label htmlFor="token-expiration" className="nl-label">
        Expiração
      </label>
      <select
        id="token-expiration"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="nl-select"
        disabled={disabled}
      >
        {EXPIRATION_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  )
}

/**
 * Formulário de criação de um Personal Access Token: nome, escopo de acesso
 * e validade opcional. O valor em claro do token é exibido pelo componente
 * pai (ver {@link TokenCreatedModal}) após a criação bem-sucedida.
 */
export function CreateTokenModal({
  isOpen,
  onClose,
  onCreate,
  isCreating = false,
}: CreateTokenModalProps) {
  const form = useCreateTokenForm(onCreate)

  if (!isOpen) return null

  const handleClose = () => {
    if (isCreating) return
    form.reset()
    onClose()
  }

  return (
    <ModalShell
      title="Novo token de acesso"
      eyebrow="Conexões"
      description="Crie um token para autenticar um cliente MCP ou de API externo em nome da sua conta."
      onClose={handleClose}
    >
      <form onSubmit={form.handleSubmit} className="space-y-5">
        <TokenNameField value={form.name} onChange={form.setName} disabled={isCreating} />
        <TokenScopeField value={form.scope} onChange={form.setScope} disabled={isCreating} />
        <TokenExpirationField
          value={form.expiresInDays}
          onChange={form.setExpiresInDays}
          disabled={isCreating}
        />

        {form.error && (
          <div className="nl-alert" role="alert">
            {form.error}
          </div>
        )}

        <div className="flex gap-3 justify-end pt-2">
          <button
            type="button"
            onClick={handleClose}
            disabled={isCreating}
            className="nl-btn min-h-[44px] min-w-[44px]"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isCreating}
            className="nl-btn-primary min-h-[44px] min-w-[44px]"
          >
            {isCreating ? 'Criando…' : 'Criar token'}
          </button>
        </div>
      </form>
    </ModalShell>
  )
}
