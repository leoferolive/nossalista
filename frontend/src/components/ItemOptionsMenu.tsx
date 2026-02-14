import { useEffect, useRef } from 'react';

interface ItemOptionsMenuProps {
  isOpen: boolean;
  onClose: () => void;
  onEdit: () => void;
  onDelete: () => void;
  position: { x: number; y: number };
}

/**
 * Menu dropdown de opções para um item
 * Aparece após long-press, com opções de Editar e Remover
 */
export function ItemOptionsMenu({
  isOpen,
  onClose,
  onEdit,
  onDelete,
  position,
}: ItemOptionsMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);

  // Fechar menu ao clicar fora
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    // Fechar ao pressionar Escape
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  // Ajustar posição para não sair da tela
  const adjustedX = Math.min(position.x, window.innerWidth - 140);
  const adjustedY = Math.min(position.y, window.innerHeight - 100);

  return (
    <>
      {/* Backdrop para fechar ao clicar fora */}
      <div
        className="fixed inset-0 z-40"
        onClick={onClose}
        data-testid="item-options-backdrop"
      />

      {/* Menu dropdown */}
      <div
        ref={menuRef}
        className="fixed z-50 glass-card rounded-lg shadow-lg p-2 min-w-[120px] animate-fade-in"
        style={{ top: adjustedY, left: adjustedX }}
        data-testid="item-options-menu"
      >
        <button
          onClick={() => {
            onEdit();
            onClose();
          }}
          className="w-full text-left px-4 py-2 hover:bg-gray-100 rounded-lg transition-colors text-gray-700"
          data-testid="item-option-edit"
        >
          Editar
        </button>
        <button
          onClick={() => {
            onDelete();
            onClose();
          }}
          className="w-full text-left px-4 py-2 hover:bg-red-50 text-red-600 rounded-lg transition-colors"
          data-testid="item-option-delete"
        >
          Remover
        </button>
      </div>
    </>
  );
}
