import React from 'react';

interface TypeCardProps {
  emoji: string;
  name: string;
  description: string;
  isSelected: boolean;
  onClick: () => void;
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
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onClick();
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={handleKeyDown}
      aria-label={`Selecionar tipo ${name}: ${description}`}
      aria-pressed={isSelected}
      className={`
        min-h-[160px] min-w-[160px]
        flex flex-col items-center justify-center
        p-4 rounded-lg cursor-pointer
        transition-all duration-200
        ${
          isSelected
            ? 'border-2 border-blue-500 bg-blue-50 shadow-md scale-105'
            : 'border-2 border-gray-300 bg-white hover:border-gray-400 hover:shadow-sm'
        }
        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
      `}
    >
      <div className="text-5xl mb-2" aria-hidden="true">
        {emoji}
      </div>
      <div className="text-lg font-semibold text-gray-900">{name}</div>
      <div className="text-sm text-gray-600 text-center mt-1">
        {description}
      </div>
    </div>
  );
};
