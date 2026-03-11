import { Link } from 'react-router-dom'
import { ListResponse, LIST_TYPES } from '../types/List'

interface ListCardProps {
  list: ListResponse
}

export function ListCard({ list }: ListCardProps) {
  const typeEmoji = LIST_TYPES.find((t) => t.id === list.type.id)?.emoji || '📝'

  return (
    <Link
      to={`/lists/${list.id}`}
      className="nl-preview-card group flex min-h-[220px] flex-col justify-between transition-transform duration-200 hover:-translate-y-1"
      aria-label={`Abrir lista ${list.name}`}
    >
      <div>
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <span
              className="inline-flex h-12 w-12 items-center justify-center rounded-2xl border border-nl-border bg-nl-surface-strong text-2xl transition-transform duration-200 group-hover:scale-110"
              aria-hidden="true"
            >
              {typeEmoji}
            </span>
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-nl-muted">
                {list.type.name}
              </p>
              <h3 className="mt-2 line-clamp-2 font-display text-2xl font-semibold text-nl-text">
                {list.name}
              </h3>
              <p className="mt-2 text-sm text-nl-muted">
                {list.isOwner ? 'Organizada por voce' : 'Compartilhada com voce'}
              </p>
            </div>
          </div>

          <span className="nl-pill">{list.isOwner ? 'Minha' : 'Compartilhada'}</span>
        </div>

        <div className="mt-6 space-y-3">
          {Array.from({ length: Math.min(Math.max(list.itemsCount, 2), 3) }).map((_, index) => (
            <div key={`${list.id}-${index}`} className="nl-checkline">
              <span className="nl-check" data-done={index === 0 && list.itemsCount > 0}>
                {index === 0 && list.itemsCount > 0 ? '✓' : ''}
              </span>
              <span className="text-sm text-nl-muted">
                {index === 0
                  ? `${list.itemsCount} ${list.itemsCount === 1 ? 'item pronto' : 'itens prontos'}`
                  : index === 1
                    ? 'Checklist compartilhado em um so lugar'
                    : 'Atualizacoes simples e em tempo real'}
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="mt-6 flex items-center justify-between border-t border-nl-border pt-4 text-sm text-nl-muted">
        <span className="font-medium text-nl-text">Abrir lista</span>
        <span className="font-tabular">{list.itemsCount} itens</span>
      </div>
    </Link>
  )
}
