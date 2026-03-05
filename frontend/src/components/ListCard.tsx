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
        rounded-3xl border border-orange-200 bg-white/95 p-5
        hover:-translate-y-1 hover:border-orange-300 hover:shadow-tropical
        transition-transform transition-colors
        focus-visible:ring-2 focus-visible:ring-orange-300
        flex flex-col justify-between
      "
      aria-label={`Abrir lista ${list.name}`}
    >
      {/* Header: Emoji + Nome */}
      <div className="flex items-start gap-3">
        <span className="text-3xl" aria-hidden="true">
          {typeEmoji}
        </span>
        <div className="min-w-0 flex-1">
          <h3 className="line-clamp-2 font-display text-lg font-semibold text-slate-900">
            {list.name}
          </h3>
          <p className="mt-1 text-sm text-slate-500">{list.type.name}</p>
        </div>
      </div>

      {/* Footer: Badge + Contagem */}
      <div className="flex items-center justify-between mt-4">
        <div>
          {list.isOwner ? (
            <span className="rounded-full bg-teal-100 px-3 py-1 text-xs font-semibold text-teal-800">
              Minha
            </span>
          ) : (
            <span className="rounded-full bg-orange-100 px-3 py-1 text-xs font-semibold text-orange-800">
              Compartilhada
            </span>
          )}
        </div>
        <div className="font-tabular text-sm text-slate-600">
          {list.itemsCount} {list.itemsCount === 1 ? 'item' : 'itens'}
        </div>
      </div>
    </Link>
  )
}
