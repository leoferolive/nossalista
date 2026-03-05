import React from 'react'
import { ListMemberResponse } from '../types/List'

/**
 * Props para o componente MembersModal
 */
export interface MembersModalProps {
  isOpen: boolean
  members: ListMemberResponse[]
  loading: boolean
  error: string | null
  isOwner: boolean
  onClose: () => void
  onLeaveList: () => void
  leaving: boolean
  isLeaveConfirmOpen: boolean
  onConfirmLeave: () => void
  onCancelLeave: () => void
  onRemoveMember: (userId: string) => void
  removingMemberId: string | null
  removeConfirmMemberId: string | null
  onConfirmRemove: () => void
  onCancelRemove: () => void
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
  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="members-modal-title"
    >
      <div className="nl-card w-full max-w-md p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 id="members-modal-title" className="font-display text-xl font-bold text-slate-900">
            Membros
          </h2>
          <button
            onClick={onClose}
            className="min-h-[44px] min-w-[44px] rounded-xl p-2 text-slate-500 transition-colors hover:bg-orange-50 hover:text-slate-700 focus-visible:ring-2 focus-visible:ring-orange-300"
            aria-label="Fechar membros"
          >
            ✕
          </button>
        </div>

        {/* Loading state */}
        {loading && (
          <p className="text-sm text-slate-600" role="status" aria-live="polite">
            Carregando membros…
          </p>
        )}

        {/* Error state */}
        {!loading && error && (
          <p
            className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"
            role="alert"
          >
            {error}
          </p>
        )}

        {/* Lista de membros */}
        {!loading && !error && (
          <div
            className="max-h-72 space-y-3 overflow-y-auto"
            style={{ overscrollBehavior: 'contain' }}
          >
            {members.map((member) => (
              <div key={member.user.id}>
                <div className="flex items-center gap-3 rounded-xl border border-orange-100 bg-orange-50/40 p-2.5">
                  {/* Avatar */}
                  {member.user.avatar_url ? (
                    <img
                      src={member.user.avatar_url}
                      alt={member.user.username}
                      className="h-10 w-10 rounded-full object-cover"
                      width={40}
                      height={40}
                    />
                  ) : (
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-orange-100 text-sm text-orange-700">
                      {member.user.username.slice(0, 2).toUpperCase()}
                    </div>
                  )}

                  {/* Info do usuário */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {member.user.username}
                    </p>
                    <p className="truncate text-xs text-slate-500">{member.user.name}</p>
                  </div>

                  {/* Badge de role */}
                  <span
                    className={`text-xs px-2 py-1 rounded font-medium ${
                      member.role === 'OWNER'
                        ? 'bg-teal-100 text-teal-700'
                        : 'bg-orange-100 text-orange-700'
                    }`}
                  >
                    {member.role === 'OWNER' ? 'Dono' : 'Membro'}
                  </span>

                  {/* Botão remover: visível apenas para MEMBERs quando o usuário logado é OWNER */}
                  {isOwner && member.role === 'MEMBER' && (
                    <button
                      onClick={() => onRemoveMember(member.user.id)}
                      disabled={removingMemberId === member.user.id}
                      className="min-h-[36px] min-w-[36px] rounded-lg p-1.5 text-red-500 transition-colors hover:bg-red-50 focus-visible:ring-2 focus-visible:ring-red-300 disabled:opacity-50"
                      aria-label={`Remover ${member.user.username}`}
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-4 w-4"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                        />
                      </svg>
                    </button>
                  )}
                </div>

                {/* Confirmação inline de remoção */}
                {removeConfirmMemberId === member.user.id && (
                  <div className="mt-1 rounded-xl border border-red-200 bg-red-50 p-3">
                    <p className="text-sm text-red-900 mb-2">
                      Remover {member.user.username}? Ação não pode ser desfeita.
                    </p>
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={onCancelRemove}
                        className="min-h-[36px] rounded-lg border border-orange-200 px-3 py-1.5 text-sm text-slate-700 transition-colors hover:bg-white focus-visible:ring-2 focus-visible:ring-orange-300"
                        disabled={removingMemberId === member.user.id}
                      >
                        Cancelar
                      </button>
                      <button
                        onClick={onConfirmRemove}
                        className="min-h-[36px] rounded-lg bg-red-600 px-3 py-1.5 text-sm text-white transition-colors hover:bg-red-700 focus-visible:ring-2 focus-visible:ring-red-300 disabled:bg-red-300"
                        disabled={removingMemberId === member.user.id}
                      >
                        {removingMemberId === member.user.id ? 'Removendo…' : 'Confirmar Remoção'}
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
          <p className="mt-4 rounded-xl border border-teal-200 bg-teal-50 p-3 text-sm text-teal-700">
            Você é o dono
          </p>
        )}

        {/* Botão "Sair da Lista" para membros */}
        {!loading && !error && !isOwner && (
          <button
            onClick={onLeaveList}
            className="mt-4 w-full min-h-[44px] rounded-xl bg-red-600 px-4 py-3 font-semibold text-white transition-colors hover:bg-red-700 focus-visible:ring-2 focus-visible:ring-red-300"
            aria-label="Sair da lista"
          >
            Sair da Lista
          </button>
        )}

        {/* Confirmação de saída */}
        {isLeaveConfirmOpen && (
          <div className="mt-4 rounded-xl border border-red-200 bg-red-50 p-4">
            <p className="text-sm text-red-900 mb-3">Sair da lista? Você perderá acesso.</p>
            <div className="flex gap-2 justify-end">
              <button
                onClick={onCancelLeave}
                className="min-h-[44px] rounded-xl border border-orange-200 px-4 py-2 text-slate-700 transition-colors hover:bg-white focus-visible:ring-2 focus-visible:ring-orange-300"
                disabled={leaving}
              >
                Cancelar
              </button>
              <button
                onClick={onConfirmLeave}
                className="min-h-[44px] rounded-xl bg-red-600 px-4 py-2 text-white transition-colors hover:bg-red-700 focus-visible:ring-2 focus-visible:ring-red-300 disabled:bg-red-300"
                disabled={leaving}
              >
                {leaving ? 'Saindo…' : 'Sair'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
