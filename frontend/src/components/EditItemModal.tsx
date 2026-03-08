import React, { useState, useEffect, useRef, useCallback } from 'react'
import { ListItem } from '../types/Item'
import { useToast } from './Toast'

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
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const { showToast } = useToast()

  const clearDebounceTimer = useCallback(() => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
      debounceTimerRef.current = null
    }
  }, [])

  // Preencher campos com valores atuais ao abrir
  useEffect(() => {
    if (isOpen && item) {
      clearDebounceTimer()
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
  }, [isOpen, item, clearDebounceTimer])

  useEffect(() => {
    return () => {
      clearDebounceTimer()
    }
  }, [clearDebounceTimer])

  const handleSave = async () => {
    if (!name.trim()) {
      showToast('Nome do item é obrigatório', 'error')
      return
    }

    const request: { name: string; quantity?: number; dueDate?: string; url?: string } = {
      name: name.trim(),
    }

    if (listType === 'SHOPPING') {
      request.quantity = quantity
    } else if (listType === 'TASK') {
      if (dueDate) request.dueDate = new Date(dueDate).toISOString()
    } else if (listType === 'WISHLIST') {
      request.url = url.trim()
    }

    clearDebounceTimer()
    debounceTimerRef.current = setTimeout(async () => {
      setIsSaving(true)
      try {
        await onSave(item.id, request)
        onClose()
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Erro ao atualizar'
        showToast(message, 'error')
      } finally {
        setIsSaving(false)
        debounceTimerRef.current = null
      }
    }, 500)
  }

  const handleClose = () => {
    clearDebounceTimer()
    setIsSaving(false)
    onClose()
  }

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4"
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
          <label htmlFor="edit-item-name" className="mb-1 block text-sm font-medium text-nl-text">
            Nome
          </label>
          <input
            id="edit-item-name"
            type="text"
            name="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-xl border border-nl-border/20 px-3 py-2.5 text-nl-text transition-colors focus:border-nl-focus/40 focus-visible:ring-2 focus-visible:ring-nl-focus/40"
            placeholder="Nome do item…"
            autoComplete="off"
            data-testid="edit-item-name"
          />
        </div>

        {/* Campos específicos por tipo */}
        {listType === 'SHOPPING' && (
          <div className="mb-4">
            <label
              htmlFor="edit-item-quantity"
              className="mb-1 block text-sm font-medium text-nl-text"
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
              className="w-full rounded-xl border border-nl-border/20 px-3 py-2.5 text-nl-text transition-colors focus:border-nl-focus/40 focus-visible:ring-2 focus-visible:ring-nl-focus/40"
              inputMode="numeric"
            />
          </div>
        )}

        {listType === 'TASK' && (
          <div className="mb-4">
            <label
              htmlFor="edit-item-due-date"
              className="mb-1 block text-sm font-medium text-nl-text"
            >
              Data de Prazo
            </label>
            <input
              id="edit-item-due-date"
              type="datetime-local"
              name="dueDate"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="w-full rounded-xl border border-nl-border/20 px-3 py-2.5 text-nl-text transition-colors focus:border-nl-focus/40 focus-visible:ring-2 focus-visible:ring-nl-focus/40"
            />
          </div>
        )}

        {listType === 'WISHLIST' && (
          <div className="mb-4">
            <label
              htmlFor="edit-item-url"
              className="mb-1 block text-sm font-medium text-nl-text"
            >
              URL/Link
            </label>
            <input
              id="edit-item-url"
              type="url"
              name="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              className="w-full rounded-xl border border-nl-border/20 px-3 py-2.5 text-nl-text transition-colors focus:border-nl-focus/40 focus-visible:ring-2 focus-visible:ring-nl-focus/40"
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
            className="min-h-[44px] rounded-xl border border-nl-border/20 px-4 py-2 text-nl-text transition-colors hover:bg-nl-bg-soft focus-visible:ring-2 focus-visible:ring-nl-focus/40 disabled:cursor-not-allowed disabled:opacity-60"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="min-h-[44px] rounded-xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-4 py-2 font-semibold text-white transition-colors hover:from-orange-600 hover:to-amber-600 focus-visible:ring-2 focus-visible:ring-nl-focus/40 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSaving ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </div>
    </div>
  )
}
