import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { CreateListModal } from './CreateListModal';
import { LIST_TYPES } from '../types/List';

describe('CreateListModal', () => {
  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('não deve renderizar quando isOpen=false', () => {
    render(
      <CreateListModal
        isOpen={false}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('deve renderizar modal quando isOpen=true', () => {
    render(
      <CreateListModal
        isOpen={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Criar Nova Lista')).toBeInTheDocument();
  });

  it('deve desabilitar botão quando nome vazio', () => {
    render(
      <CreateListModal
        isOpen={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    const submitButton = screen.getByRole('button', { name: /criar lista/i });
    expect(submitButton).toBeDisabled();
  });

  it('deve desabilitar botão quando tipo não selecionado', async () => {
    render(
      <CreateListModal
        isOpen={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    // Digitar nome válido
    const nameInput = screen.getByLabelText(/nome da lista/i);
    await userEvent.type(nameInput, 'Lista Teste');

    const submitButton = screen.getByRole('button', { name: /criar lista/i });
    expect(submitButton).toBeDisabled();
  });

  it('deve habilitar botão quando nome e tipo válidos', async () => {
    render(
      <CreateListModal
        isOpen={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    const nameInput = screen.getByLabelText(/nome da lista/i);
    await userEvent.type(nameInput, 'Minha Lista');

    // Selecionar primeiro tipo
    const firstTypeCard = screen.getByRole('button', { name: /Compras/i });
    await userEvent.click(firstTypeCard);

    const submitButton = screen.getByRole('button', { name: /criar lista/i });
    expect(submitButton).not.toBeDisabled();
  });

  it('deve mostrar erro quando nome < 3 caracteres', async () => {
    render(
      <CreateListModal
        isOpen={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    const nameInput = screen.getByLabelText(/nome da lista/i);
    await userEvent.type(nameInput, 'AB');

    expect(screen.getByText(/mínimo 3 caracteres/)).toBeInTheDocument();
  });

  it('deve submeter ao pressionar Enter quando válido', async () => {
    mockOnSubmit.mockResolvedValue({ id: 'test-list-id' });

    render(
      <CreateListModal
        isOpen={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    const nameInput = screen.getByLabelText(/nome da lista/i);
    await userEvent.type(nameInput, 'Minha Lista');

    const firstTypeCard = screen.getByRole('button', { name: /Compras/i });
    await userEvent.click(firstTypeCard);

    // Pressionar Enter
    await userEvent.type(nameInput, '{Enter}');

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledOnce();
      expect(mockOnSuccess).toHaveBeenCalledWith('test-list-id');
    });
  });

  it('deve fechar modal ao pressionar Escape', async () => {
    render(
      <CreateListModal
        isOpen={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    const modal = screen.getByRole('dialog');
    await userEvent.type(modal, '{Escape}');

    expect(mockOnClose).toHaveBeenCalledOnce();
  });

  it('deve renderizar 4 TypeCards (um para cada tipo)', () => {
    render(
      <CreateListModal
        isOpen={true}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
        onSubmit={mockOnSubmit}
      />
    );

    LIST_TYPES.forEach((type) => {
      expect(screen.getByText(type.name)).toBeInTheDocument();
      expect(screen.getByText(type.emoji)).toBeInTheDocument();
    });
  });
});
