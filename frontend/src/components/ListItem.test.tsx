import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ListItemComponent } from './ListItem';
import { ListItem } from '../types/Item';

describe('ListItemComponent', () => {
  const mockToggle = vi.fn();
  const mockEdit = vi.fn();

  const createMockItem = (overrides: Partial<ListItem> = {}): ListItem => ({
    id: 'item-1',
    name: 'Item de Teste',
    checked: false,
    quantity: null,
    dueDate: null,
    url: null,
    position: 0,
    createdBy: {
      id: 'user-1',
      username: 'testuser',
      name: 'Test User',
      avatarUrl: null,
    },
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  });

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('deve renderizar nome do item', () => {
    const item = createMockItem({ name: 'Arroz Integral' });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    expect(screen.getByText('Arroz Integral')).toBeInTheDocument();
  });

  it('deve renderizar checkbox desmarcado quando checked=false', () => {
    const item = createMockItem({ checked: false });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).toHaveAttribute('aria-checked', 'false');
  });

  it('deve renderizar checkbox marcado quando checked=true', () => {
    const item = createMockItem({ checked: true });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).toHaveAttribute('aria-checked', 'true');
  });

  it('deve aplicar line-through quando checked=true', () => {
    const item = createMockItem({ checked: true, name: 'Item Concluído' });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const nameElement = screen.getByText('Item Concluído');
    expect(nameElement).toHaveClass('line-through');
  });

  it('deve aplicar opacidade 50% quando checked=true', () => {
    const item = createMockItem({ checked: true });
    const { container } = render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const itemContainer = container.firstChild as HTMLElement;
    expect(itemContainer).toHaveClass('opacity-50');
  });

  it('deve chamar onToggle ao clicar no checkbox', () => {
    const item = createMockItem({ id: 'item-123' });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);

    expect(mockToggle).toHaveBeenCalledWith('item-123');
  });

  it('deve chamar onEdit ao clicar no item', () => {
    const item = createMockItem({ id: 'item-456' });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const itemContainer = screen.getByText('Item de Teste').closest('div[class*="flex items-center"]');
    fireEvent.click(itemContainer!);

    expect(mockEdit).toHaveBeenCalledWith(item);
  });

  it('deve mostrar quantity quando não for null', () => {
    const item = createMockItem({ quantity: 5 });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    expect(screen.getByText('5x')).toBeInTheDocument();
  });

  it('não deve mostrar quantity quando for null', () => {
    const item = createMockItem({ quantity: null });
    const { container } = render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    expect(container.textContent).not.toContain('x');
  });

  it('deve mostrar dueDate formatado quando não for null', () => {
    const item = createMockItem({ dueDate: '2026-12-25T00:00:00Z' });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    // Usar regex para ser mais flexível com o formato da data
    expect(screen.getByText(/\d{2}\/\d{2}\/\d{4}/)).toBeInTheDocument();
  });

  it('não deve mostrar dueDate quando for null', () => {
    const item = createMockItem({ dueDate: null });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    // Não deve ter o ícone de calendário
    const calendarIcons = screen.queryAllByRole('img');
    expect(calendarIcons.length).toBe(1); // Apenas o avatar
  });

  it('deve mostrar URL como link quando não for null', () => {
    const item = createMockItem({ url: 'https://example.com/product' });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const link = screen.getByText('Ver produto');
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', 'https://example.com/product');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('não deve mostrar URL quando for null', () => {
    const item = createMockItem({ url: null });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    expect(screen.queryByText('Ver produto')).not.toBeInTheDocument();
  });

  it('deve mostrar avatar e username do criador', () => {
    const item = createMockItem({
      createdBy: {
        id: 'user-1',
        username: 'johndoe',
        name: 'John Doe',
        avatarUrl: 'https://example.com/avatar.jpg',
      },
    });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const avatar = screen.getByAltText('johndoe');
    expect(avatar).toHaveAttribute('src', 'https://example.com/avatar.jpg');
    expect(screen.getByText('johndoe')).toBeInTheDocument();
  });

  it('deve ter touch target mínimo de 44px (NFR-A4)', () => {
    const item = createMockItem();
    const { container } = render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    const itemContainer = container.firstChild as HTMLElement;
    expect(itemContainer).toHaveStyle({ minHeight: '44px' });
  });

  it('deve mostrar múltiplos campos extras simultaneamente', () => {
    const item = createMockItem({
      quantity: 3,
      dueDate: '2026-06-15T00:00:00Z',
    });
    render(<ListItemComponent item={item} onToggle={mockToggle} onEdit={mockEdit} />);

    expect(screen.getByText('3x')).toBeInTheDocument();
    // Usar regex para ser mais flexível com o formato da data
    expect(screen.getByText(/\d{2}\/\d{2}\/\d{4}/)).toBeInTheDocument();
  });
});
