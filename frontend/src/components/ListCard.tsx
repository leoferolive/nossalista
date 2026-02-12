import { useNavigate } from 'react-router-dom';
import { ListResponse, LIST_TYPES } from '../types/List';

interface ListCardProps {
  list: ListResponse;
}

/**
 * Card visual para exibir uma lista
 * Atende NFR-A4: Touch target mínimo de 44px (usa min-h-[160px])
 */
export function ListCard({ list }: ListCardProps) {
  const navigate = useNavigate();
  const typeEmoji =
    LIST_TYPES.find((t) => t.id === list.type.id)?.emoji || '📝';

  const handleClick = () => {
    navigate(`/lists/${list.id}`);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  };

  return (
    <div
      onClick={handleClick}
      onKeyDown={handleKeyPress}
      className="
        min-h-[160px]
        border border-gray-200 rounded-lg p-4
        hover:shadow-lg hover:border-blue-400
        transition-all cursor-pointer
        focus:outline-none focus:ring-2 focus:ring-blue-500
        flex flex-col justify-between
      "
      tabIndex={0}
      role="button"
      aria-label={`Abrir lista ${list.name}`}
    >
      {/* Header: Emoji + Nome */}
      <div className="flex items-start gap-3">
        <span className="text-3xl" aria-hidden="true">
          {typeEmoji}
        </span>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900 line-clamp-2">
            {list.name}
          </h3>
          <p className="text-sm text-gray-500 mt-1">{list.type.name}</p>
        </div>
      </div>

      {/* Footer: Badge + Contagem */}
      <div className="flex items-center justify-between mt-4">
        <div>
          {list.isOwner ? (
            <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded font-medium">
              Minha
            </span>
          ) : (
            <span className="text-xs bg-gray-100 text-gray-800 px-2 py-1 rounded font-medium">
              Compartilhada
            </span>
          )}
        </div>
        <div className="text-sm text-gray-600">
          {list.itemsCount}{' '}
          {list.itemsCount === 1 ? 'item' : 'itens'}
        </div>
      </div>
    </div>
  );
}
