import React, { useState } from 'react'
import { useToast } from './Toast'

export interface UserProfileProps {
  username: string
  email: string
  name: string | null
  avatarUrl: string | null
  authProvider: string
  isEditing: boolean
  onEdit: () => void
  onCancelEdit: () => void
  onSave: (data: { name: string; avatarUrl: string }) => Promise<void>
}

export const UserProfile: React.FC<UserProfileProps> = ({
  username,
  email,
  name,
  avatarUrl,
  authProvider,
  isEditing,
  onEdit,
  onCancelEdit,
  onSave,
}) => {
  const [editedName, setEditedName] = useState(name || '')
  const [editedAvatarUrl, setEditedAvatarUrl] = useState(avatarUrl || '')
  const [isSaving, setIsSaving] = useState(false)
  const { showToast } = useToast()

  const handleSave = async () => {
    if (!editedName.trim()) {
      showToast('Nome e obrigatorio', 'error')
      return
    }

    setIsSaving(true)
    try {
      await onSave({
        name: editedName.trim(),
        avatarUrl: editedAvatarUrl,
      })
      onCancelEdit()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao atualizar perfil'
      showToast(message, 'error')
    } finally {
      setIsSaving(false)
    }
  }

  const providerLabel = authProvider === 'GOOGLE' ? 'Google' : 'Email & Senha'

  return (
    <section className="nl-card p-6 sm:p-8" aria-label="Dados de perfil">
      <div className="flex flex-col gap-6 sm:flex-row sm:items-start">
        <div className="relative shrink-0">
          {(isEditing ? editedAvatarUrl : avatarUrl) ? (
            <img
              src={isEditing ? editedAvatarUrl : avatarUrl!}
              alt={username}
              className="h-24 w-24 rounded-3xl border-4 border-orange-100 object-cover shadow-md"
              width={96}
              height={96}
            />
          ) : (
            <div className="flex h-24 w-24 items-center justify-center rounded-3xl border-4 border-orange-100 bg-gradient-to-br from-orange-500 to-amber-500 text-4xl font-bold text-white shadow-md">
              {username.charAt(0).toUpperCase()}
            </div>
          )}

          {!isEditing && (
            <button
              type="button"
              onClick={onEdit}
              className="absolute -bottom-1 -right-1 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-2xl border border-orange-200 bg-white text-slate-700 shadow-sm transition-colors hover:bg-orange-50 focus-visible:ring-2 focus-visible:ring-orange-300"
              aria-label="Editar perfil"
            >
              ✎
            </button>
          )}
        </div>

        <div className="min-w-0 flex-1">
          <h2 className="font-display text-2xl font-bold text-slate-900">Seu Perfil</h2>
          <p className="mt-1 text-sm text-slate-600">
            Mantenha seus dados atualizados para facilitar convites e colaboracao.
          </p>

          {!isEditing ? (
            <button
              type="button"
              onClick={onEdit}
              className="mt-4 inline-flex min-h-[44px] items-center rounded-xl bg-gradient-to-r from-teal-700 to-teal-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:from-teal-800 hover:to-teal-700 focus-visible:ring-2 focus-visible:ring-orange-300"
            >
              Editar Perfil
            </button>
          ) : (
            <div className="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={onCancelEdit}
                className="inline-flex min-h-[44px] items-center rounded-xl border border-orange-200 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-orange-50 focus-visible:ring-2 focus-visible:ring-orange-300"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleSave}
                disabled={isSaving}
                className="inline-flex min-h-[44px] items-center rounded-xl bg-gradient-to-r from-orange-500 to-amber-500 px-4 py-2 text-sm font-semibold text-white transition-colors hover:from-orange-600 hover:to-amber-600 focus-visible:ring-2 focus-visible:ring-orange-300 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSaving ? 'Salvando…' : 'Salvar Alteracoes'}
              </button>
            </div>
          )}
        </div>
      </div>

      {!isEditing && (
        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          <div className="rounded-2xl border border-orange-100 bg-orange-50/70 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-orange-700">
              Username
            </p>
            <p className="mt-1 truncate text-lg font-medium text-slate-900">@{username}</p>
          </div>
          <div className="rounded-2xl border border-orange-100 bg-orange-50/70 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-orange-700">
              Email
            </p>
            <p className="mt-1 truncate text-lg text-slate-900">{email}</p>
          </div>
          <div className="rounded-2xl border border-teal-100 bg-teal-50/70 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-700">Nome</p>
            <p className="mt-1 truncate text-lg text-slate-900">{name || 'Nao informado'}</p>
          </div>
          <div className="rounded-2xl border border-teal-100 bg-teal-50/70 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-700">
              Metodo de Acesso
            </p>
            <p className="mt-1 text-lg text-slate-900">{providerLabel}</p>
          </div>
        </div>
      )}

      {isEditing && (
        <div className="mt-8 space-y-4">
          <div>
            <label
              htmlFor="profile-name"
              className="mb-1.5 block text-sm font-medium text-slate-700"
            >
              Nome
            </label>
            <input
              id="profile-name"
              name="name"
              type="text"
              value={editedName}
              onChange={(e) => setEditedName(e.target.value)}
              className="w-full rounded-xl border border-orange-200 px-4 py-3 text-slate-900 transition-colors focus:border-orange-400 focus-visible:ring-2 focus-visible:ring-orange-300"
              placeholder="Como voce quer aparecer…"
              maxLength={100}
              autoComplete="name"
              data-testid="profile-name-input"
            />
          </div>

          <div>
            <label htmlFor="avatar-url" className="mb-1.5 block text-sm font-medium text-slate-700">
              URL da Foto
            </label>
            <input
              id="avatar-url"
              name="avatarUrl"
              type="url"
              value={editedAvatarUrl}
              onChange={(e) => setEditedAvatarUrl(e.target.value)}
              className="w-full rounded-xl border border-orange-200 px-4 py-3 text-slate-900 transition-colors focus:border-orange-400 focus-visible:ring-2 focus-visible:ring-orange-300"
              placeholder="https://exemplo.com/foto.jpg"
              inputMode="url"
              autoComplete="off"
              data-testid="avatar-url-input"
            />
            {editedAvatarUrl && (
              <div className="mt-3 flex items-center gap-3 rounded-xl border border-orange-100 bg-orange-50/60 p-3">
                <img
                  src={editedAvatarUrl}
                  alt="Preview do avatar"
                  className="h-12 w-12 rounded-xl border border-orange-100 object-cover"
                  width={48}
                  height={48}
                  onError={(e) => {
                    ;(e.target as HTMLImageElement).style.display = 'none'
                  }}
                />
                <button
                  type="button"
                  onClick={() => setEditedAvatarUrl('')}
                  className="inline-flex min-h-[44px] items-center rounded-xl border border-orange-200 px-3 py-1 text-sm text-slate-700 transition-colors hover:bg-orange-100 focus-visible:ring-2 focus-visible:ring-orange-300"
                >
                  Remover
                </button>
              </div>
            )}
          </div>

          <p className="rounded-xl border border-teal-100 bg-teal-50/60 px-4 py-3 text-sm text-teal-800">
            Username e email nao podem ser alterados nesta versao.
          </p>
        </div>
      )}
    </section>
  )
}
