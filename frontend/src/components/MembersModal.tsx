import React from 'react';
import { ListMemberResponse } from '../types/List';

/**
 * Props para o componente MembersModal
 */
export interface MembersModalProps {
  isOpen: boolean;
  members: ListMemberResponse[];
  loading: boolean;
  error: string | null;
  isOwner: boolean;
  onClose: () => void;
  onLeaveList: () => void;
  leaving: boolean;
  isLeaveConfirmOpen: boolean;
  onConfirmLeave: () => void;
  onCancelLeave: () => void;
  onRemoveMember: (userId: string) => void;
  removingMemberId: string | null;
  removeConfirmMemberId: string | null;
  onConfirmRemove: () => void;
  onCancelRemove: () => void;
}

/**
 * Componente de modal de membros
 * AC: Mostra lista de membros com avatares, usernames, roles
 * AC: Dono vê mensagem "Você é o dono", não-owners veem botão "Sair"
 * AC: Confirmação de saída com Cancelar/Sair
 */
export const MembersModal: React.FC<MembersModalProps> = ({
  isOpen,
  members,
  loading,
  error,
  isOwner,
  onClose,
  onLeaveList,
  leaving,
  isLeaveConfirmOpen,
  onConfirmLeave,
  onCancelLeave,
  onRemoveMember,
  removingMemberId,
  removeConfirmMemberId,
  onConfirmRemove,
  onCancelRemove,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/20 backdrop-blur-sm flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-labelledby="members-modal-title">
      <div className="bg-white border border-gray-200 rounded-2xl shadow-2xl w-full max-w-md p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 id="members-modal-title" className="text-xl font-bold text-gray-900">Membros</h2>
          <button
            onClick={onClose}
            className="p-2 min-w-[44px] min-h-[44px] hover:bg-gray-100 rounded-lg transition-colors"
            aria-label="Fechar membros"
          >
            ✕
          </button>
        </div>

        {/* Loading state */}
        {loading && (
          <p className="text-sm text-gray-600">Carregando membros...</p>
        )}

        {/* Error state */}
        {!loading && error && (
          <p className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg p-3">
            {error}
          </p>
        )}

        {/* Lista de membros */}
        {!loading && !error && (
          <div className="space-y-3 max-h-72 overflow-y-auto">
            {members.map((member) => (
              <div key={member.user.id}>
                <div className="flex items-center gap-3 p-2 rounded-lg border border-gray-100">
                  {/* Avatar */}
                  {member.user.avatar_url ? (
                    <img
                      src={member.user.avatar_url}
                      alt={member.user.username}
                      className="w-10 h-10 rounded-full object-cover"
                    />
                  ) : (
                    <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-600 text-sm">
                      {member.user.username.slice(0, 2).toUpperCase()}
                    </div>
                  )}

                  {/* Info do usuário */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {member.user.username}
                    </p>
                    <p className="text-xs text-gray-500 truncate">
                      {member.user.name}
                    </p>
                  </div>

                  {/* Badge de role */}
                  <span
                    className={`text-xs px-2 py-1 rounded font-medium ${
                      member.role === 'OWNER'
                        ? 'bg-blue-100 text-blue-700'
                        : 'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {member.role === 'OWNER' ? 'Dono' : 'Membro'}
                  </span>

                  {/* Botão remover: visível apenas para MEMBERs quando o usuário logado é OWNER */}
                  {isOwner && member.role === 'MEMBER' && (
                    <button
                      onClick={() => onRemoveMember(member.user.id)}
                      disabled={removingMemberId === member.user.id}
                      className="p-1.5 min-w-[32px] min-h-[32px] text-red-500 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
                      aria-label={`Remover ${member.user.username}`}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  )}
                </div>

                {/* Confirmação inline de remoção */}
                {removeConfirmMemberId === member.user.id && (
                  <div className="mt-1 border border-red-200 bg-red-50 rounded-lg p-3">
                    <p className="text-sm text-red-900 mb-2">
                      Remover {member.user.username}? Ação não pode ser desfeita.
                    </p>
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={onCancelRemove}
                        className="px-3 py-1.5 min-h-[36px] rounded-lg border border-gray-300 text-gray-700 hover:bg-white text-sm"
                        disabled={removingMemberId === member.user.id}
                      >
                        Cancelar
                      </button>
                      <button
                        onClick={onConfirmRemove}
                        className="px-3 py-1.5 min-h-[36px] rounded-lg bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300 text-sm"
                        disabled={removingMemberId === member.user.id}
                      >
                        {removingMemberId === member.user.id ? 'Removendo...' : 'Confirmar Remoção'}
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Mensagem para dono */}
        {!loading && !error && isOwner && (
          <p className="mt-4 text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded-lg p-3">
            Você é o dono
          </p>
        )}

        {/* Botão "Sair da Lista" para membros */}
        {!loading && !error && !isOwner && (
          <button
            onClick={onLeaveList}
            className="w-full mt-4 px-4 py-3 min-h-[44px] rounded-lg bg-red-600 text-white font-semibold hover:bg-red-700 transition-colors"
            aria-label="Sair da lista"
          >
            Sair da Lista
          </button>
        )}

        {/* Confirmação de saída */}
        {isLeaveConfirmOpen && (
          <div className="mt-4 border border-red-200 bg-red-50 rounded-lg p-4">
            <p className="text-sm text-red-900 mb-3">
              Sair da lista? Você perderá acesso.
            </p>
            <div className="flex gap-2 justify-end">
              <button
                onClick={onCancelLeave}
                className="px-4 py-2 min-h-[44px] rounded-lg border border-gray-300 text-gray-700 hover:bg-white"
                disabled={leaving}
              >
                Cancelar
              </button>
              <button
                onClick={onConfirmLeave}
                className="px-4 py-2 min-h-[44px] rounded-lg bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300"
                disabled={leaving}
              >
                {leaving ? 'Saindo...' : 'Sair'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
