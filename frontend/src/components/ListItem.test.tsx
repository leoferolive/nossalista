import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { ListItemComponent } from './ListItem'
import { ListItem } from '../types/Item'

describe('ListItemComponent', () => {
  const mockItem: ListItem = {
    id: 'test-item-id',
    name: 'Test Item',
    checked: false,
    quantity: 2,
    dueDate: null,
    url: null,
    position: 0,
    createdBy: {
      id: 'user-id',
      username: 'testuser',
      name: 'Test User',
      avatarUrl: null,
    },
    createdAt: '2026-02-13T10:00:00Z',
    updatedAt: '2026-02-13T10:00:00Z',
  }

  it('deve chamar onToggle ao clicar no checkbox', () => {
    const onToggle = vi.fn()
    const onEdit = vi.fn()

    render(<ListItemComponent item={mockItem} onToggle={onToggle} onEdit={onEdit} />)

    // Clicar no checkbox (botão com role checkbox)
    const checkbox = screen.getByRole('checkbox')
    fireEvent.click(checkbox)

    // Verificar se onToggle foi chamado com o id correto
    expect(onToggle).toHaveBeenCalledWith('test-item-id')
    expect(onToggle).toHaveBeenCalledTimes(1)
  })

  it('deve chamar onEdit (não onToggle) ao clicar no texto/nome do item', () => {
    const onToggle = vi.fn()
    const onEdit = vi.fn()

    render(<ListItemComponent item={mockItem} onToggle={onToggle} onEdit={onEdit} />)

    // Clicar no nome do item (container do texto)
    const itemName = screen.getByText('Test Item')
    fireEvent.click(itemName)

    // Verificar que onEdit foi chamado e onToggle não foi
    expect(onEdit).toHaveBeenCalledWith(mockItem)
    expect(onEdit).toHaveBeenCalledTimes(1)
    expect(onToggle).not.toHaveBeenCalled()
  })

  it('deve aplicar estilo de riscado quando item está checked', () => {
    const onToggle = vi.fn()
    const onEdit = vi.fn()

    const checkedItem = { ...mockItem, checked: true }

    render(<ListItemComponent item={checkedItem} onToggle={onToggle} onEdit={onEdit} />)

    // Verificar se o checkbox está marcado
    const checkbox = screen.getByRole('checkbox')
    expect(checkbox).toHaveAttribute('aria-checked', 'true')

    // Verificar se o texto tem classe de riscado
    const itemName = screen.getByText('Test Item')
    expect(itemName).toHaveClass('line-through')
  })

  it('deve aplicar classe ws-item-checked quando isWsCheckedHighlight=true', () => {
    render(
      <ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} isWsCheckedHighlight />
    )

    const itemContainer = screen.getByTestId(`list-item-${mockItem.id}`)
    expect(itemContainer).toHaveClass('ws-item-checked')
  })

  it('deve aplicar classe animate-pop no checkbox quando isWsChecked=true', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} isWsChecked />)

    const checkbox = screen.getByRole('checkbox')
    expect(checkbox).toHaveClass('animate-pop')
  })

  it('não deve aplicar classes de websocket quando isWsChecked=false', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)

    const itemContainer = screen.getByTestId(`list-item-${mockItem.id}`)
    const checkbox = screen.getByRole('checkbox')
    expect(itemContainer).not.toHaveClass('ws-item-checked')
    expect(checkbox).not.toHaveClass('animate-pop')
  })

  // ---------------------------------------------------------------------------
  // Estado do item: checked vs unchecked (ícone do checkbox e aria-label)
  // ---------------------------------------------------------------------------

  it('renderiza o ícone de check (svg) apenas quando o item está checked', () => {
    const { container, rerender } = render(
      <ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />
    )
    const checkbox = screen.getByRole('checkbox')
    // Não checked: sem svg interno no checkbox e aria-label "Marcar como concluído"
    expect(checkbox.querySelector('svg')).toBeNull()
    expect(checkbox).toHaveAttribute('aria-label', 'Marcar como concluído')
    expect(checkbox).toHaveAttribute('aria-checked', 'false')

    rerender(
      <ListItemComponent
        item={{ ...mockItem, checked: true }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    const checkedCheckbox = screen.getByRole('checkbox')
    expect(checkedCheckbox.querySelector('svg')).not.toBeNull()
    expect(checkedCheckbox).toHaveAttribute('aria-label', 'Marcar como não concluído')
    expect(container).toBeTruthy()
  })

  it('não aplica opacity-50 quando o item não está checked', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)
    expect(screen.getByTestId(`list-item-${mockItem.id}`)).not.toHaveClass('opacity-50')
  })

  it('aplica opacity-50 quando o item está checked', () => {
    render(
      <ListItemComponent
        item={{ ...mockItem, checked: true }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    expect(screen.getByTestId(`list-item-${mockItem.id}`)).toHaveClass('opacity-50')
  })

  it('aplica animate-fade-out quando isDeleting=true', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} isDeleting />)
    expect(screen.getByTestId(`list-item-${mockItem.id}`)).toHaveClass('animate-fade-out')
  })

  it('aplica ws-item-added quando isWsAdded=true', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} isWsAdded />)
    expect(screen.getByTestId(`list-item-${mockItem.id}`)).toHaveClass('ws-item-added')
  })

  // ---------------------------------------------------------------------------
  // Campos dinâmicos: quantity, dueDate, url
  // ---------------------------------------------------------------------------

  it('exibe o badge de quantidade quando quantity está definido', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)
    expect(screen.getByText('2x')).toBeInTheDocument()
  })

  it('não exibe o badge de quantidade quando quantity é null', () => {
    render(
      <ListItemComponent
        item={{ ...mockItem, quantity: null }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    expect(screen.queryByText(/x$/)).not.toBeInTheDocument()
    expect(screen.queryByText('2x')).not.toBeInTheDocument()
  })

  it('exibe quantidade zero (0x) corretamente — não é tratado como ausente', () => {
    render(
      <ListItemComponent item={{ ...mockItem, quantity: 0 }} onToggle={vi.fn()} onEdit={vi.fn()} />
    )
    expect(screen.getByText('0x')).toBeInTheDocument()
  })

  it('exibe a data de vencimento formatada (pt-BR) quando dueDate está definido', () => {
    render(
      <ListItemComponent
        item={{ ...mockItem, dueDate: '2026-02-13T10:00:00Z' }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    expect(screen.getByText('13/02/2026')).toBeInTheDocument()
  })

  it('não exibe a data de vencimento quando dueDate é null', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)
    expect(screen.queryByText(/\d{2}\/\d{2}\/\d{4}/)).not.toBeInTheDocument()
  })

  it('exibe o link "Ver produto" quando url está definido e impede propagação no clique', () => {
    const onEdit = vi.fn()
    render(
      <ListItemComponent
        item={{ ...mockItem, url: 'https://loja.com/produto' }}
        onToggle={vi.fn()}
        onEdit={onEdit}
      />
    )
    const link = screen.getByRole('link', { name: /Ver produto/i })
    expect(link).toHaveAttribute('href', 'https://loja.com/produto')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')

    // Clique no link não deve disparar onEdit (stopPropagation)
    fireEvent.click(link)
    expect(onEdit).not.toHaveBeenCalled()
  })

  it('não exibe o link "Ver produto" quando url é null', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)
    expect(screen.queryByRole('link', { name: /Ver produto/i })).not.toBeInTheDocument()
  })

  // ---------------------------------------------------------------------------
  // Avatar do criador: URL segura, insegura e fallback onError
  // ---------------------------------------------------------------------------

  it('usa o avatarUrl quando é uma URL https de domínio permitido', () => {
    render(
      <ListItemComponent
        item={{
          ...mockItem,
          createdBy: {
            ...mockItem.createdBy,
            avatarUrl: 'https://lh3.googleusercontent.com/a/photo.jpg',
          },
        }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    const avatar = screen.getByAltText('testuser')
    expect(avatar).toHaveAttribute('src', 'https://lh3.googleusercontent.com/a/photo.jpg')
  })

  it('usa fallback de iniciais quando avatarUrl é de domínio não permitido', () => {
    render(
      <ListItemComponent
        item={{
          ...mockItem,
          createdBy: { ...mockItem.createdBy, avatarUrl: 'https://evil.com/x.jpg' },
        }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    const avatar = screen.getByAltText('testuser')
    expect(avatar.getAttribute('src')).toContain('data:image/svg+xml')
  })

  it('usa fallback de iniciais quando avatarUrl não é https', () => {
    render(
      <ListItemComponent
        item={{
          ...mockItem,
          createdBy: {
            ...mockItem.createdBy,
            avatarUrl: 'http://lh3.googleusercontent.com/a/photo.jpg',
          },
        }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    const avatar = screen.getByAltText('testuser')
    expect(avatar.getAttribute('src')).toContain('data:image/svg+xml')
  })

  it('usa fallback de iniciais quando avatarUrl é uma string inválida (URL inparseável)', () => {
    render(
      <ListItemComponent
        item={{
          ...mockItem,
          createdBy: { ...mockItem.createdBy, avatarUrl: 'not a url' },
        }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    const avatar = screen.getByAltText('testuser')
    expect(avatar.getAttribute('src')).toContain('data:image/svg+xml')
  })

  it('usa fallback de iniciais quando avatarUrl é null', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)
    const avatar = screen.getByAltText('testuser')
    expect(avatar.getAttribute('src')).toContain('data:image/svg+xml')
  })

  it('substitui a imagem por iniciais quando o carregamento falha (onError)', () => {
    render(
      <ListItemComponent
        item={{
          ...mockItem,
          createdBy: {
            ...mockItem.createdBy,
            avatarUrl: 'https://lh3.googleusercontent.com/a/photo.jpg',
          },
        }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    const avatar = screen.getByAltText('testuser') as HTMLImageElement
    // Dispara o handler onError -> substitui src pelo data URI de iniciais e limpa onerror
    fireEvent.error(avatar)
    expect(avatar.getAttribute('src')).toContain('data:image/svg+xml')
    expect(avatar.onerror).toBeNull()
  })

  it('gera iniciais "?" quando o username está vazio no fallback', () => {
    render(
      <ListItemComponent
        item={{
          ...mockItem,
          createdBy: { ...mockItem.createdBy, username: '', avatarUrl: null },
        }}
        onToggle={vi.fn()}
        onEdit={vi.fn()}
      />
    )
    // alt fica vazio; pegamos a img dentro do container
    const container = screen.getByTestId(`list-item-${mockItem.id}`)
    const img = container.querySelector('img') as HTMLImageElement
    // O SVG de fallback contém ">%3F<" (? codificado) — verificamos o data URI
    expect(img.getAttribute('src')).toContain('data:image/svg+xml')
    expect(img.getAttribute('src')).toContain('%3F')
  })

  // ---------------------------------------------------------------------------
  // Menu de opções (abre via botão de ações; editar e remover)
  // ---------------------------------------------------------------------------

  it('abre o menu de opções ao clicar no botão "Mais ações" e não dispara onEdit do item', () => {
    const onEdit = vi.fn()
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={onEdit} />)

    // Menu fechado inicialmente
    expect(screen.queryByTestId('item-options-menu')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Mais ações do item/i }))

    expect(screen.getByTestId('item-options-menu')).toBeInTheDocument()
    // O clique no botão de ações não deve propagar para o handler de edição do container
    expect(onEdit).not.toHaveBeenCalled()
  })

  it('posiciona o menu usando o bounding rect do elemento quando ele existe', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)

    const el = document.getElementById(`list-item-${mockItem.id}`)!
    vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({
      left: 100,
      top: 50,
      width: 200,
      height: 60,
      right: 300,
      bottom: 110,
      x: 100,
      y: 50,
      toJSON: () => ({}),
    } as DOMRect)

    fireEvent.click(screen.getByRole('button', { name: /Mais ações do item/i }))
    const menu = screen.getByTestId('item-options-menu')
    // left = rect.left + width/2 - 60 = 100 + 100 - 60 = 140 (ajustado por innerWidth no menu)
    expect(menu).toBeInTheDocument()
  })

  it('aciona onEdit pelo menu de opções', () => {
    const onEdit = vi.fn()
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={onEdit} />)

    fireEvent.click(screen.getByRole('button', { name: /Mais ações do item/i }))
    fireEvent.click(screen.getByTestId('item-option-edit'))

    expect(onEdit).toHaveBeenCalledWith(mockItem)
  })

  it('aciona onDelete pelo menu de opções quando onDelete é fornecido', () => {
    const onDelete = vi.fn()
    render(
      <ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} onDelete={onDelete} />
    )

    fireEvent.click(screen.getByRole('button', { name: /Mais ações do item/i }))
    fireEvent.click(screen.getByTestId('item-option-delete'))

    expect(onDelete).toHaveBeenCalledWith(mockItem)
  })

  it('não quebra ao remover quando onDelete é undefined (optional chaining)', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: /Mais ações do item/i }))
    // onDelete?.() — sem onDelete não deve lançar
    expect(() => fireEvent.click(screen.getByTestId('item-option-delete'))).not.toThrow()
  })

  // ---------------------------------------------------------------------------
  // Long-press abre o menu (fallback de posição quando elemento não encontrado)
  // ---------------------------------------------------------------------------

  it('abre o menu via long-press (mouse down + timer)', () => {
    vi.useFakeTimers()
    try {
      render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)

      const container = screen.getByTestId(`list-item-${mockItem.id}`)
      fireEvent.mouseDown(container)
      // delay do long-press é 550ms; o callback dispara setState, então embrulhamos em act
      act(() => {
        vi.advanceTimersByTime(600)
      })

      expect(screen.getByTestId('item-options-menu')).toBeInTheDocument()
    } finally {
      vi.useRealTimers()
    }
  })

  it('usa posição central de fallback quando o elemento do item não é encontrado', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)

    const realGetById = document.getElementById.bind(document)
    vi.spyOn(document, 'getElementById').mockImplementation((id: string) =>
      id === `list-item-${mockItem.id}` ? null : realGetById(id)
    )

    fireEvent.click(screen.getByRole('button', { name: /Mais ações do item/i }))
    expect(screen.getByTestId('item-options-menu')).toBeInTheDocument()

    vi.restoreAllMocks()
  })
})
