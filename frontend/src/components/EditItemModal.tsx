import React, { useState, useEffect } from 'react'
import { ListItem } from '../types/Item'
import { useToast } from '../contexts/ToastContext'

interface EditItemModalProps {
  item: ListItem
  listType: string
  isOpen: boolean
  onClose: () => void
  onSave: (
    itemId: string,
    request: { name: string; quantity?: number; dueDate?: string; url?: string }
  ) => Promise<void>
}

export const EditItemModal: React.FC<EditItemModalProps> = ({
  item,
  listType,
  isOpen,
  onClose,
  onSave,
}) => {
  const [name, setName] = useState(item.name)
  const [quantity, setQuantity] = useState(item.quantity ?? 1)
  const [dueDate, setDueDate] = useState<string>(() => {
    if (!item.dueDate) return ''
    // Converter para formato YYYY-MM-DDTHH:MM para datetime-local
    const date = new Date(item.dueDate)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day}T${hours}:${minutes}`
  })
  const [url, setUrl] = useState(item.url || '')
  const [isSaving, setIsSaving] = useState(false)
  const { showToast } = useToast()

  // Preencher campos com valores atuais ao abrir
  useEffect(() => {
    if (isOpen && item) {
      setName(item.name)
      setQuantity(item.quantity ?? 1)
      if (item.dueDate) {
        const date = new Date(item.dueDate)
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        setDueDate(`${year}-${month}-${day}T${hours}:${minutes}`)
      } else {
        setDueDate('')
      }
      setUrl(item.url || '')
    }
  }, [isOpen, item])

  const handleSave = async () => {
    if (!name.trim()) {
      showToast('Nome do item é obrigatório', 'error')
      return
    }

    const request: { name: string; quantity?: number; dueDate?: string; url?: string } = {
      name: name.trim(),
    }

    if (listType === 'compras') {
      request.quantity = quantity
    } else if (listType === 'tarefas') {
      if (dueDate) request.dueDate = new Date(dueDate).toISOString()
    } else if (listType === 'wishlist') {
      request.url = url.trim()
    }

    setIsSaving(true)
    try {
      await onSave(item.id, request)
      onClose()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao atualizar'
      showToast(message, 'error')
    } finally {
      setIsSaving(false)
    }
  }

  const handleClose = () => {
    setIsSaving(false)
    onClose()
  }

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-nl-bg/80 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="edit-item-title"
    >
      <div className="nl-card w-full max-w-md p-6">
        <h2 id="edit-item-title" className="mb-4 font-display text-xl font-semibold text-nl-text">
          Editar Item
        </h2>

        {/* Campo name (obrigatório para todos os tipos) */}
        <div className="mb-4">
          <label htmlFor="edit-item-name" className="mb-1 block text-sm font-medium text-nl-muted">
            Nome
          </label>
          <input
            id="edit-item-name"
            type="text"
            name="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-xl border border-nl-border px-3 py-2.5 text-nl-text transition-colors focus:border-nl-accent focus-visible:ring-2 focus-visible:ring-nl-accent/30"
            placeholder="Nome do item…"
            autoComplete="off"
            data-testid="edit-item-name"
          />
        </div>

        {/* Campos específicos por tipo */}
        {listType === 'compras' && (
          <div className="mb-4">
            <label
              htmlFor="edit-item-quantity"
              className="mb-1 block text-sm font-medium text-nl-muted"
            >
              Quantidade
            </label>
            <input
              id="edit-item-quantity"
              type="number"
              name="quantity"
              min="1"
              value={quantity}
              onChange={(e) => setQuantity(parseInt(e.target.value) || 1)}
              className="w-full rounded-xl border border-nl-border px-3 py-2.5 text-nl-text transition-colors focus:border-nl-accent focus-visible:ring-2 focus-visible:ring-nl-accent/30"
              inputMode="numeric"
            />
          </div>
        )}

        {listType === 'tarefas' && (
          <div className="mb-4">
            <label
              htmlFor="edit-item-due-date"
              className="mb-1 block text-sm font-medium text-nl-muted"
            >
              Data de Prazo
            </label>
            <input
              id="edit-item-due-date"
              type="datetime-local"
              name="dueDate"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="w-full rounded-xl border border-nl-border px-3 py-2.5 text-nl-text transition-colors focus:border-nl-accent focus-visible:ring-2 focus-visible:ring-nl-accent/30"
            />
          </div>
        )}

        {listType === 'wishlist' && (
          <div className="mb-4">
            <label htmlFor="edit-item-url" className="mb-1 block text-sm font-medium text-nl-muted">
              URL/Link
            </label>
            <input
              id="edit-item-url"
              type="url"
              name="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              className="w-full rounded-xl border border-nl-border px-3 py-2.5 text-nl-text transition-colors focus:border-nl-accent focus-visible:ring-2 focus-visible:ring-nl-accent/30"
              placeholder="https://exemplo.com/produto…"
              inputMode="url"
              spellCheck={false}
            />
          </div>
        )}

        {/* Botões */}
        <div className="flex gap-2 justify-end mt-6">
          <button
            onClick={handleClose}
            disabled={isSaving}
            className="min-h-[44px] rounded-xl border border-nl-border px-4 py-2 text-nl-muted transition-colors hover:bg-nl-surface-strong focus-visible:ring-2 focus-visible:ring-nl-accent/30 disabled:cursor-not-allowed disabled:opacity-60"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="nl-btn-primary min-h-[44px] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSaving ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </div>
    </div>
  )
}
