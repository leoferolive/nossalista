import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MembersModal } from './MembersModal';
import { ListMemberResponse } from '../types/List';

describe('MembersModal', () => {
  const mockMembers: ListMemberResponse[] = [
    {
      user: {
        id: 'user-1',
        username: 'owneruser',
        name: 'Owner User',
        avatar_url: 'http://example.com/owner.png',
      },
      role: 'OWNER',
      joined_at: '2026-02-20T10:00:00Z',
    },
    {
      user: {
        id: 'user-2',
        username: 'memberuser',
        name: 'Member User',
        avatar_url: null,
      },
      role: 'MEMBER',
      joined_at: '2026-02-21T10:00:00Z',
    },
  ];

  const defaultProps = {
    isOpen: true,
    members: mockMembers,
    loading: false,
    error: null,
    isOwner: false,
    onClose: vi.fn(),
    onLeaveList: vi.fn(),
    leaving: false,
    isLeaveConfirmOpen: false,
    onConfirmLeave: vi.fn(),
    onCancelLeave: vi.fn(),
    onRemoveMember: vi.fn(),
    removingMemberId: null,
    removeConfirmMemberId: null,
    onConfirmRemove: vi.fn(),
    onCancelRemove: vi.fn(),
  };

  it('não deve renderizar quando isOpen é false', () => {
    render(<MembersModal {...defaultProps} isOpen={false} />);
    expect(screen.queryByText('Membros')).not.toBeInTheDocument();
  });

  it('deve renderizar a lista de membros corretamente', () => {
    render(<MembersModal {...defaultProps} />);
    expect(screen.getByText('Membros')).toBeInTheDocument();
    expect(screen.getByText('owneruser')).toBeInTheDocument();
    expect(screen.getByText('memberuser')).toBeInTheDocument();
    expect(screen.getByText('Dono')).toBeInTheDocument();
    expect(screen.getByText('Membro')).toBeInTheDocument();
  });

  it('deve mostrar indicador de carregamento', () => {
    render(<MembersModal {...defaultProps} loading={true} />);
    expect(screen.getByText(/Carregando membros/i)).toBeInTheDocument();
  });

  it('deve mostrar mensagem de erro', () => {
    render(<MembersModal {...defaultProps} error="Erro ao carregar" />);
    expect(screen.getByText('Erro ao carregar')).toBeInTheDocument();
  });

  it('deve mostrar "Você é o dono" para proprietários da lista', () => {
    render(<MembersModal {...defaultProps} isOwner={true} />);
    expect(screen.getByText('Você é o dono')).toBeInTheDocument();
    expect(screen.queryByText('Sair da Lista')).not.toBeInTheDocument();
  });

  it('deve mostrar o botão "Sair da Lista" para membros comuns', () => {
    render(<MembersModal {...defaultProps} isOwner={false} />);
    expect(screen.getByText('Sair da Lista')).toBeInTheDocument();
  });

  it('deve abrir a confirmação de saída quando clicar no botão', () => {
    render(<MembersModal {...defaultProps} isLeaveConfirmOpen={true} />);
    expect(screen.getByText('Sair da lista? Você perderá acesso.')).toBeInTheDocument();
  });

  it('deve chamar onConfirmLeave ao clicar em "Sair" na confirmação', () => {
    const onConfirmLeave = vi.fn();
    render(<MembersModal {...defaultProps} isLeaveConfirmOpen={true} onConfirmLeave={onConfirmLeave} />);
    fireEvent.click(screen.getByText('Sair'));
    expect(onConfirmLeave).toHaveBeenCalled();
  });

  it('deve chamar onCancelLeave ao clicar em "Cancelar" na confirmação', () => {
    const onCancelLeave = vi.fn();
    render(<MembersModal {...defaultProps} isLeaveConfirmOpen={true} onCancelLeave={onCancelLeave} />);
    fireEvent.click(screen.getByText('Cancelar'));
    expect(onCancelLeave).toHaveBeenCalled();
  });

  it('deve desabilitar os botões de confirmação enquanto está saindo', () => {
    render(<MembersModal {...defaultProps} isLeaveConfirmOpen={true} leaving={true} />);
    expect(screen.getByText(/Saindo/i)).toBeDisabled();
    expect(screen.getByText('Cancelar')).toBeDisabled();
  });

  it('deve chamar onLeaveList ao clicar em "Sair da Lista"', () => {
    const onLeaveList = vi.fn();
    render(<MembersModal {...defaultProps} onLeaveList={onLeaveList} />);
    fireEvent.click(screen.getByLabelText('Sair da lista'));
    expect(onLeaveList).toHaveBeenCalled();
  });

  it('deve fechar o modal ao clicar no botão de fechar', () => {
    const onClose = vi.fn();
    render(<MembersModal {...defaultProps} onClose={onClose} />);
    fireEvent.click(screen.getByLabelText('Fechar membros'));
    expect(onClose).toHaveBeenCalled();
  });

  it('deve mostrar botão de remoção para MEMBER quando isOwner é true', () => {
    render(<MembersModal {...defaultProps} isOwner={true} />);
    expect(screen.getByLabelText('Remover memberuser')).toBeInTheDocument();
  });

  it('não deve mostrar botão de remoção quando isOwner é false', () => {
    render(<MembersModal {...defaultProps} isOwner={false} />);
    expect(screen.queryByLabelText('Remover memberuser')).not.toBeInTheDocument();
  });

  it('OWNER não deve ter botão de remoção ao lado do próprio nome', () => {
    render(<MembersModal {...defaultProps} isOwner={true} />);
    expect(screen.queryByLabelText('Remover owneruser')).not.toBeInTheDocument();
  });

  it('deve mostrar modal de confirmação quando removeConfirmMemberId está definido', () => {
    render(<MembersModal {...defaultProps} isOwner={true} removeConfirmMemberId="user-2" />);
    expect(screen.getByText(/Remover memberuser\? Ação não pode ser desfeita\./)).toBeInTheDocument();
    expect(screen.getByText('Confirmar Remoção')).toBeInTheDocument();
  });

  it('deve chamar onConfirmRemove ao confirmar e onCancelRemove ao cancelar', () => {
    const onConfirmRemove = vi.fn();
    const onCancelRemove = vi.fn();
    render(
      <MembersModal
        {...defaultProps}
        isOwner={true}
        removeConfirmMemberId="user-2"
        onConfirmRemove={onConfirmRemove}
        onCancelRemove={onCancelRemove}
      />
    );
    fireEvent.click(screen.getByText('Confirmar Remoção'));
    expect(onConfirmRemove).toHaveBeenCalled();

    fireEvent.click(screen.getByText('Cancelar'));
    expect(onCancelRemove).toHaveBeenCalled();
  });
});
