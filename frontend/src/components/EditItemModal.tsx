import React, { useState, useEffect } from 'react';
import { ListItem } from '../types/Item';
import { useToast } from './Toast';

interface EditItemModalProps {
  item: ListItem;
  listType: string;
  isOpen: boolean;
  onClose: () => void;
  onSave: (itemId: string, request: { name: string; quantity?: number; dueDate?: string; url?: string }) => Promise<void>;
}

export const EditItemModal: React.FC<EditItemModalProps> = ({
  item,
  listType,
  isOpen,
  onClose,
  onSave,
}) => {
  const [name, setName] = useState(item.name);
  const [quantity, setQuantity] = useState(item.quantity ?? 1);
  const [dueDate, setDueDate] = useState<string>(() => {
    if (!item.dueDate) return '';
    // Converter para formato YYYY-MM-DDTHH:MM para datetime-local
    const date = new Date(item.dueDate);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  });
  const [url, setUrl] = useState(item.url || '');
  const [isSaving, setIsSaving] = useState(false);
  const { showToast } = useToast();

  // Preencher campos com valores atuais ao abrir
  useEffect(() => {
    if (isOpen && item) {
      setName(item.name);
      setQuantity(item.quantity ?? 1);
      if (item.dueDate) {
        const date = new Date(item.dueDate);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        setDueDate(`${year}-${month}-${day}T${hours}:${minutes}`);
      } else {
        setDueDate('');
      }
      setUrl(item.url || '');
    }
  }, [isOpen, item]);

  const handleSave = async () => {
    if (!name.trim()) {
      showToast('Nome do item é obrigatório', 'error');
      return;
    }

    setIsSaving(true);
    try {
      const request: { name: string; quantity?: number; dueDate?: string; url?: string } = { name: name.trim() };

      if (listType === 'SHOPPING') {
        request.quantity = quantity;
      } else if (listType === 'TASK') {
        if (dueDate) request.dueDate = new Date(dueDate).toISOString();
      } else if (listType === 'WISHLIST') {
        request.url = url.trim();
      }

      await onSave(item.id, request);
      showToast('Item atualizado', 'success');
      onClose();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao atualizar';
      showToast(message, 'error');
    } finally {
      setIsSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="glass-card w-full max-w-md mx-4 p-6">
        <h2 className="text-xl font-semibold mb-4">Editar Item</h2>

        {/* Campo name (obrigatório para todos os tipos) */}
        <div className="mb-4">
          <label htmlFor="edit-item-name" className="block text-sm font-medium mb-1">Nome</label>
          <input
            id="edit-item-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full p-2 border rounded-lg"
            placeholder="Nome do item"
            data-testid="edit-item-name"
          />
        </div>

        {/* Campos específicos por tipo */}
        {listType === 'SHOPPING' && (
          <div className="mb-4">
            <label htmlFor="edit-item-quantity" className="block text-sm font-medium mb-1">Quantidade</label>
            <input
              id="edit-item-quantity"
              type="number"
              min="1"
              value={quantity}
              onChange={(e) => setQuantity(parseInt(e.target.value) || 1)}
              className="w-full p-2 border rounded-lg"
            />
          </div>
        )}

        {listType === 'TASK' && (
          <div className="mb-4">
            <label htmlFor="edit-item-due-date" className="block text-sm font-medium mb-1">Data de Prazo</label>
            <input
              id="edit-item-due-date"
              type="datetime-local"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="w-full p-2 border rounded-lg"
            />
          </div>
        )}

        {listType === 'WISHLIST' && (
          <div className="mb-4">
            <label htmlFor="edit-item-url" className="block text-sm font-medium mb-1">URL/Link</label>
            <input
              id="edit-item-url"
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              className="w-full p-2 border rounded-lg"
              placeholder="https://..."
            />
          </div>
        )}

        {/* Botões */}
        <div className="flex gap-2 justify-end mt-6">
          <button
            onClick={onClose}
            disabled={isSaving}
            className="px-4 py-2 rounded-lg border hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="px-4 py-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600 disabled:opacity-50"
          >
            {isSaving ? 'Salvando...' : 'Salvar'}
          </button>
        </div>
      </div>
    </div>
  );
};
