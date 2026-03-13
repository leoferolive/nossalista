import { Link } from 'react-router-dom'
import { ListResponse, LIST_TYPES } from '../types/List'

interface ListCardProps {
  list: ListResponse
}

export function ListCard({ list }: ListCardProps) {
  const typeEmoji = LIST_TYPES.find((t) => t.id === list.type.id)?.emoji || '📝'
  const itemStatus =
    list.itemsCount === 0
      ? 'Sem itens ainda'
      : `${list.itemsCount} ${list.itemsCount === 1 ? 'item' : 'itens'}`
  const realtimeStatus = list.isOwner
    ? 'Convide pessoas para colaborar'
    : 'Atualização em tempo real'

  return (
    <Link
      to={`/lists/${list.id}`}
      className="nl-preview-card group flex min-h-[188px] flex-col justify-between transition-transform duration-200 hover:-translate-y-1"
      aria-label={`Abrir lista ${list.name}`}
    >
      <div className="space-y-5">
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
              <h3 className="mt-1 line-clamp-2 font-display text-[1.85rem] font-semibold leading-[1.02] text-nl-text sm:text-2xl">
                {list.name}
              </h3>
              <p className="mt-2 text-sm text-nl-muted">
                {list.isOwner ? 'Organizada por você' : 'Compartilhada com você'}
              </p>
            </div>
          </div>

          <span
            className={
              list.isOwner
                ? 'nl-pill'
                : 'nl-pill border-nl-border-strong bg-nl-accent-soft/60 text-nl-text'
            }
          >
            {list.isOwner ? 'Minha' : 'Compartilhada'}
          </span>
        </div>

        <div className="space-y-2 text-sm text-nl-muted">
          <p className="font-medium text-nl-text">{itemStatus}</p>
          <p>{realtimeStatus}</p>
        </div>
      </div>

      <div className="mt-5 flex items-center justify-between border-t border-nl-border pt-4 text-sm text-nl-muted">
        <span className="font-medium text-nl-text">Toque para abrir</span>
        <span className="font-tabular">{list.itemsCount} itens</span>
        <span aria-hidden="true" className="text-base leading-none text-nl-text">
          →
        </span>
      </div>
    </Link>
  )
}
