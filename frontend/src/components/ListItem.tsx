import React, { useState, useCallback } from 'react'
import { ListItemProps } from '../types/Item'
import { useLongPress } from '../hooks/useLongPress'
import { ItemOptionsMenu } from './ItemOptionsMenu'

/**
 * Componente de item de lista
 * AC: Renderiza checkbox customizado, nome, campos extras, criador
 * AC: Suporta long-press (1 segundo) para abrir menu de opções
 * AC: Animação fade-out (200ms) antes de remover
 */
export const ListItemComponent: React.FC<ListItemProps> = React.memo(
  ({
    item,
    onToggle,
    onEdit,
    onDelete,
    isDeleting = false,
    isWsAdded = false,
    isWsChecked = false,
    isWsCheckedHighlight = false,
  }) => {
    const [menuOpen, setMenuOpen] = useState(false)
    const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 })

    const handleCheckboxClick = (e: React.MouseEvent) => {
      e.stopPropagation()
      onToggle(item.id)
    }

    const handleItemClick = () => {
      onEdit(item)
    }

    // Handler para long-press
    const handleLongPress = useCallback(() => {
      // Calcular posição do menu (centro do item)
      const element = document.getElementById(`list-item-${item.id}`)
      if (element) {
        const rect = element.getBoundingClientRect()
        setMenuPosition({
          x: rect.left + rect.width / 2 - 60, // Centro - metade da largura do menu
          y: rect.top + rect.height / 2,
        })
      } else {
        // Fallback: usar posição do mouse ou centro da tela
        setMenuPosition({
          x: window.innerWidth / 2 - 60,
          y: window.innerHeight / 2,
        })
      }
      setMenuOpen(true)
    }, [item.id])

    const longPressProps = useLongPress({
      onLongPress: handleLongPress,
      delay: 1000, // 1 segundo
    })

    // Formatar data para exibição (pt-BR)
    const formatDate = (dateString: string | null): string => {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      })
    }

    return (
      <>
        <div
          id={`list-item-${item.id}`}
          className={`flex items-center gap-3 rounded-2xl border border-orange-100 bg-white/80 p-3 transition-colors hover:bg-orange-50/60 ${
            item.checked ? 'opacity-50' : ''
          } ${isDeleting ? 'animate-fade-out' : ''} ${isWsAdded ? 'ws-item-added' : ''} ${isWsCheckedHighlight ? 'ws-item-checked' : ''}`}
          {...longPressProps}
          style={{ minHeight: '44px' }} // NFR-A4: Touch target ≥ 44px
          data-testid={`list-item-${item.id}`}
        >
          {/* Checkbox customizado com animação "pop" */}
          <button
            onClick={handleCheckboxClick}
            className={`h-6 w-6 rounded border-2 transition-colors duration-300 flex items-center justify-center focus-visible:ring-2 focus-visible:ring-orange-300 ${
              item.checked
                ? 'border-teal-600 bg-teal-600'
                : 'border-orange-200 hover:border-orange-400'
            } ${isWsChecked ? 'animate-pop' : ''}`}
            aria-label={item.checked ? 'Marcar como não concluído' : 'Marcar como concluído'}
            aria-checked={item.checked}
            role="checkbox"
          >
            {item.checked && (
              <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                  clipRule="evenodd"
                />
              </svg>
            )}
          </button>

          {/* Conteúdo do item */}
          <div className="flex-1 min-w-0">
            <button
              type="button"
              onClick={handleItemClick}
              className={`w-full truncate text-left font-medium transition-colors hover:text-teal-800 focus-visible:ring-2 focus-visible:ring-orange-300 ${
                item.checked ? 'text-slate-500 line-through' : 'text-slate-900'
              }`}
            >
              {item.name}
            </button>

            {/* Campos extras */}
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-500">
              {/* Quantity (Compras) */}
              {item.quantity !== null && item.quantity !== undefined && (
                <span className="rounded bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-800">
                  {item.quantity}x
                </span>
              )}

              {/* Due Date (Tarefas) */}
              {item.dueDate && (
                <span className="flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                    />
                  </svg>
                  {formatDate(item.dueDate)}
                </span>
              )}

              {/* URL (Wishlist) */}
              {item.url && (
                <a
                  href={item.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-1 text-teal-700 underline-offset-2 hover:underline"
                  onClick={(e) => e.stopPropagation()}
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
                    />
                  </svg>
                  Ver produto
                </a>
              )}
            </div>
          </div>

          {/* Criador (avatar + username) */}
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <img
              src={item.createdBy.avatarUrl || '/default-avatar.png'}
              alt={item.createdBy.username}
              className="h-6 w-6 rounded-full bg-orange-100"
              width={24}
              height={24}
              onError={(e) => {
                // Fallback para avatar padrão se imagem falhar
                ;(e.target as HTMLImageElement).src = '/default-avatar.png'
              }}
            />
            <span className="hidden sm:inline truncate max-w-[100px]">
              {item.createdBy.username}
            </span>
          </div>
        </div>

        {/* Menu de opções (aparece após long-press) */}
        <ItemOptionsMenu
          isOpen={menuOpen}
          onClose={() => setMenuOpen(false)}
          onEdit={() => onEdit(item)}
          onDelete={() => onDelete?.(item)}
          position={menuPosition}
        />
      </>
    )
  }
)

ListItemComponent.displayName = 'ListItemComponent'
