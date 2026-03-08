import React from 'react'

interface TypeCardProps {
  emoji: string
  name: string
  description: string
  isSelected: boolean
  onClick: () => void
}

/**
 * Card visual para seleção de tipo de lista
 * Touch target mínimo: 160px (NFR-A4)
 */
export const TypeCard: React.FC<TypeCardProps> = ({
  emoji,
  name,
  description,
  isSelected,
  onClick,
}) => {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={`Selecionar tipo ${name}: ${description}`}
      aria-pressed={isSelected}
      className={`
        min-h-[160px] min-w-[160px]
        flex flex-col items-center justify-center
        p-4 rounded-2xl
        transition-transform transition-colors duration-200
        ${
          isSelected
            ? 'border-2 border-nl-primary bg-nl-primary/15 shadow-md scale-[1.02]'
            : 'border-2 border-nl-border/20 bg-nl-surface hover:border-nl-border/50 hover:bg-nl-bg-soft'
        }
        focus-visible:ring-2 focus-visible:ring-nl-focus/40 focus-visible:ring-offset-2
      `}
    >
      <div className="text-5xl mb-2" aria-hidden="true">
        {emoji}
      </div>
      <div className="font-display text-lg font-semibold text-nl-text">{name}</div>
      <div className="mt-1 text-center text-sm text-nl-muted">{description}</div>
    </button>
  )
}
