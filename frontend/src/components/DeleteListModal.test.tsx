import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { DeleteListModal } from './DeleteListModal';

describe('DeleteListModal', () => {
  const mockOnClose = vi.fn();
  const mockOnConfirm = vi.fn();
  const defaultProps = {
    isOpen: true,
    listName: 'Minha Lista de Teste',
    onClose: mockOnClose,
    onConfirm: mockOnConfirm,
    isDeleting: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('deve renderizar modal com título e mensagem corretos', () => {
    render(<DeleteListModal {...defaultProps} />);

    expect(screen.getByText('Excluir Lista?')).toBeInTheDocument();
    expect(screen.getByText(/Minha Lista de Teste/)).toBeInTheDocument();
    expect(screen.getByText(/Esta ação não pode ser desfeita/)).toBeInTheDocument();
  });

  it('deve fechar modal ao clicar em Cancelar sem chamar onConfirm', () => {
    render(<DeleteListModal {...defaultProps} />);

    const cancelButton = screen.getByText('Cancelar');
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
    expect(mockOnConfirm).not.toHaveBeenCalled();
  });

  it('deve chamar onConfirm ao clicar em Excluir', async () => {
    mockOnConfirm.mockResolvedValue(undefined);
    render(<DeleteListModal {...defaultProps} />);

    const confirmButton = screen.getByText('Excluir');
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockOnConfirm).toHaveBeenCalledTimes(1);
    });
  });

  it('deve desabilitar botões durante isDeleting', () => {
    render(<DeleteListModal {...defaultProps} isDeleting={true} />);

    const cancelButton = screen.getByText('Cancelar');
    const confirmButton = screen.getByText('Excluindo...');

    expect(cancelButton).toBeDisabled();
    expect(confirmButton).toBeDisabled();
  });

  it('deve mostrar loading indicator durante isDeleting', () => {
    render(<DeleteListModal {...defaultProps} isDeleting={true} />);

    expect(screen.getByText('Excluindo...')).toBeInTheDocument();
  });

  it('deve fechar modal ao pressionar ESC (se não isDeleting)', () => {
    render(<DeleteListModal {...defaultProps} />);

    const modal = screen.getByRole('dialog');
    fireEvent.keyDown(modal, { key: 'Escape' });

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('não deve fechar modal ao pressionar ESC durante isDeleting', () => {
    render(<DeleteListModal {...defaultProps} isDeleting={true} />);

    const modal = screen.getByRole('dialog');
    fireEvent.keyDown(modal, { key: 'Escape' });

    expect(mockOnClose).not.toHaveBeenCalled();
  });

  it('não deve renderizar nada quando isOpen é false', () => {
    const { container } = render(<DeleteListModal {...defaultProps} isOpen={false} />);

    expect(container.firstChild).toBeNull();
  });

  it('deve ter botões com touch target mínimo de 44px', () => {
    render(<DeleteListModal {...defaultProps} />);

    const cancelButton = screen.getByText('Cancelar');
    const confirmButton = screen.getByText('Excluir');

    // Verifica classes Tailwind min-w-[44px] min-h-[44px]
    expect(cancelButton).toHaveClass('min-w-[44px]', 'min-h-[44px]');
    expect(confirmButton).toHaveClass('min-w-[44px]', 'min-h-[44px]');
  });

  it('deve ter ARIA labels apropriados', () => {
    render(<DeleteListModal {...defaultProps} />);

    const modal = screen.getByRole('dialog');
    expect(modal).toHaveAttribute('aria-modal', 'true');
    expect(modal).toHaveAttribute('aria-labelledby', 'delete-modal-title');
  });

  it('deve auto-focus no botão Cancelar quando abre (NFR-A4)', () => {
    render(<DeleteListModal {...defaultProps} />);

    const cancelButton = screen.getByText('Cancelar');
    expect(cancelButton).toHaveFocus();
  });

  it('deve implementar focus trap com TAB (NFR-A4)', () => {
    render(<DeleteListModal {...defaultProps} />);

    const modal = screen.getByRole('dialog');
    const cancelButton = screen.getByText('Cancelar');
    const confirmButton = screen.getByText('Excluir');

    // Cancelar tem focus inicialmente
    expect(cancelButton).toHaveFocus();

    // TAB move para Excluir
    fireEvent.keyDown(modal, { key: 'Tab' });
    expect(confirmButton).toHaveFocus();

    // TAB novamente volta para Cancelar (focus trap)
    fireEvent.keyDown(modal, { key: 'Tab' });
    expect(cancelButton).toHaveFocus();
  });
});
