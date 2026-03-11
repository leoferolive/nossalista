import React from 'react'

interface TypeCardProps {
  emoji: string
  name: string
  description: string
  isSelected: boolean
  onClick: () => void
}

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
      className={`nl-preview-card min-h-[160px] min-w-[160px] text-left transition-all duration-200 ${
        isSelected
          ? 'border-2 border-nl-primary bg-nl-primary/10'
          : 'hover:-translate-y-1 hover:border-nl-border-strong'
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl border border-nl-border bg-nl-surface-strong text-4xl">
          {emoji}
        </div>
        <span className="nl-pill">{isSelected ? 'Selecionado' : 'Tipo'}</span>
      </div>
      <div className="mt-6">
        <div className="font-display text-2xl font-semibold text-nl-text">{name}</div>
        <div className="mt-2 text-sm leading-7 text-nl-muted">{description}</div>
      </div>
    </button>
  )
}
