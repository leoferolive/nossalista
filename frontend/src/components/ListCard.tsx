import { Link } from 'react-router-dom'
import { ListResponse, LIST_TYPES } from '../types/List'

interface ListCardProps {
  list: ListResponse
}

/**
 * Card visual para exibir uma lista
 * Atende NFR-A4: Touch target mínimo de 44px (usa min-h-[160px])
 */
export function ListCard({ list }: ListCardProps) {
  const typeEmoji = LIST_TYPES.find((t) => t.id === list.type.id)?.emoji || '📝'

  return (
    <Link
      to={`/lists/${list.id}`}
      className="
        min-h-[160px]
        rounded-3xl border border-nl-border bg-nl-surface p-5
        hover:-translate-y-1 hover:border-nl-border-strong hover:shadow-earthen-strong
        transition-all duration-200
        focus-visible:ring-2 focus-visible:ring-nl-accent/40
        flex flex-col justify-between
        group
      "
      aria-label={`Abrir lista ${list.name}`}
    >
      {/* Header: Emoji + Nome */}
      <div className="flex items-start gap-3">
        <span
          className="flex h-10 w-10 items-center justify-center rounded-2xl bg-nl-surface-strong text-2xl transition-transform group-hover:scale-110"
          aria-hidden="true"
        >
          {typeEmoji}
        </span>
        <div className="min-w-0 flex-1">
          <h3 className="line-clamp-2 font-display text-lg font-semibold text-nl-text">
            {list.name}
          </h3>
          <p className="mt-1 font-sans text-sm text-nl-muted">{list.type.name}</p>
        </div>
      </div>

      {/* Footer: Badge + Contagem */}
      <div className="mt-4 flex items-center justify-between">
        <div>
          {list.isOwner ? (
            <span className="rounded-full border border-nl-primary/30 bg-nl-primary/15 px-3 py-1 font-sans text-xs font-semibold text-nl-primary">
              Minha
            </span>
          ) : (
            <span className="rounded-full border border-nl-accent/30 bg-nl-accent/15 px-3 py-1 font-sans text-xs font-semibold text-nl-accent">
              Compartilhada
            </span>
          )}
        </div>
        <div className="font-tabular font-sans text-sm text-nl-muted">
          {list.itemsCount} {list.itemsCount === 1 ? 'item' : 'itens'}
        </div>
      </div>
    </Link>
  )
}
