import React, { useState, useEffect, useRef } from 'react'
import { TypeCard } from './TypeCard'
import { LIST_TYPES } from '../types/List'
import { CreateListRequest } from '../types/List'
import { ModalShell } from './ModalShell'

interface CreateListModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (listId: string) => void
  onSubmit: (request: CreateListRequest) => Promise<{ id: string }>
}

export const CreateListModal: React.FC<CreateListModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  onSubmit,
}) => {
  const [name, setName] = useState('')
  const [selectedTypeId, setSelectedTypeId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus()
    }
  }, [isOpen])

  useEffect(() => {
    if (!isOpen) {
      setName('')
      setSelectedTypeId(null)
      setError(null)
    }
  }, [isOpen])

  const isNameValid = name.trim().length >= 3
  const isTypeSelected = selectedTypeId !== null
  const isFormValid = isNameValid && isTypeSelected

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) {
      e.preventDefault()
    }

    if (!isFormValid || loading) {
      return
    }

    setLoading(true)
    setError(null)

    try {
      const result = await onSubmit({
        name: name.trim(),
        typeId: selectedTypeId!,
      })

      onSuccess(result.id)
    } catch (err) {
      const axiosError = err as {
        response?: { data?: { detail?: string; errors?: Record<string, string> } }
      }
      const fieldErrors = axiosError.response?.data?.errors
      const detail = axiosError.response?.data?.detail

      let errorMessage = 'Erro ao criar lista'
      if (fieldErrors) {
        const firstError = Object.values(fieldErrors)[0]
        errorMessage = firstError || detail || 'Erro ao criar lista'
      } else if (detail) {
        errorMessage = detail
      } else if (err instanceof Error) {
        errorMessage = err.message
      }
      setError(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && isFormValid) {
      e.preventDefault()
      handleSubmit()
    }
  }

  if (!isOpen) {
    return null
  }

  return (
    <ModalShell
      title="Criar Nova Lista"
      eyebrow="Nova lista"
      description="Escolha um nome e um tipo. O resto fica simples de ajustar depois."
      onClose={onClose}
      size="lg"
    >
      <form onSubmit={handleSubmit}>
        <div className="mb-6">
          <label htmlFor="list-name" className="nl-label">
            Nome da lista
          </label>
          <input
            ref={inputRef}
            id="list-name"
            name="listName"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ex: Mercado semanal"
            className="nl-input"
            autoComplete="off"
            aria-describedby="name-error"
            aria-invalid={name.length > 0 && !isNameValid}
          />
          {name.length > 0 && !isNameValid ? (
            <p id="name-error" className="nl-helper nl-helper-error" role="alert">
              O nome deve ter no minimo 3 caracteres.
            </p>
          ) : (
            <p className="nl-helper">Use um nome curto e facil de reconhecer.</p>
          )}
        </div>

        <div className="mb-6">
          <label className="nl-label">Tipo da lista</label>
          <div
            className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
            role="radiogroup"
            aria-label="Selecione o tipo de lista"
          >
            {LIST_TYPES.map((type) => (
              <TypeCard
                key={type.id}
                emoji={type.emoji}
                name={type.name}
                description={type.description}
                isSelected={selectedTypeId === type.id}
                onClick={() => setSelectedTypeId(type.id)}
              />
            ))}
          </div>
        </div>

        {error && (
          <div className="nl-alert mb-6" role="alert" aria-live="polite">
            {error}
          </div>
        )}

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button type="button" onClick={onClose} disabled={loading} className="nl-btn-secondary">
            Cancelar
          </button>
          <button type="submit" disabled={!isFormValid || loading} className="nl-btn-primary">
            {loading ? 'Criando...' : 'Criar Lista'}
          </button>
        </div>
      </form>
    </ModalShell>
  )
}
