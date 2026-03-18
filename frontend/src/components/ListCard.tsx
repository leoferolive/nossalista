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
      className="nl-preview-card group flex min-h-[152px] flex-col justify-between transition-transform duration-200 hover:-translate-y-1 sm:min-h-[188px]"
      aria-label={`Abrir lista ${list.name}`}
    >
      <div className="space-y-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <span
              className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-nl-border bg-nl-surface-strong text-xl transition-transform duration-200 group-hover:scale-110 sm:h-12 sm:w-12 sm:rounded-2xl sm:text-2xl"
              aria-hidden="true"
            >
              {typeEmoji}
            </span>
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-nl-muted">
                {list.type.name}
              </p>
              <h3 className="line-clamp-2 font-display text-[1.5rem] font-semibold leading-[1.05] text-nl-text sm:mt-1 sm:text-2xl">
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

      <div className="mt-5 flex items-center justify-end border-t border-nl-border pt-4 text-sm text-nl-muted">
        <svg className="h-5 w-5 text-nl-text transition-transform duration-200 group-hover:translate-x-1" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
          <path fillRule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clipRule="evenodd" />
        </svg>
      </div>
    </Link>
  )
}
