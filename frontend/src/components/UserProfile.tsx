import React, { useState } from 'react';
import { useToast } from './Toast';

/**
 * Props para UserProfile component
 */
export interface UserProfileProps {
  username: string;
  email: string;
  name: string | null;
  avatarUrl: string | null;
  authProvider: string;
  isEditing: boolean;
  onEdit: () => void;
  onCancelEdit: () => void;
  onSave: (data: { name: string; avatarUrl: string }) => Promise<void>;
}

/**
 * Componente de perfil do usuário
 * AC4: Exibe nome, username, email, avatar, auth provider
 * AC4: Modo edição permite alterar nome e avatar
 */
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
  const [editedName, setEditedName] = useState(name || '');
  const [editedAvatarUrl, setEditedAvatarUrl] = useState(avatarUrl || '');
  const [isSaving, setIsSaving] = useState(false);

  const { showToast } = useToast();

  const handleSave = async () => {
    if (!editedName.trim()) {
      showToast('Nome é obrigatório', 'error');
      return;
    }

    setIsSaving(true);
    try {
      await onSave({
        name: editedName.trim(),
        avatarUrl: editedAvatarUrl,
      });
      onCancelEdit();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao atualizar perfil';
      showToast(message, 'error');
    } finally {
      setIsSaving(false);
    }
  };

  const getAuthProviderIcon = () => {
    switch (authProvider) {
      case 'GOOGLE':
        return <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor"><path d="M22.56 12.25c0-1.38-.57-3.13-3.1V6c0-2.45 1.85-4.47 4.47a3 3 0 013 6 0 012 1v2h2V9a1 1 0 00-1-1h-1a1 1 0 00-1 1v2H6a3 3 0 013-1 1v12h-2a3 3 0 013-1 1h2zm-2.8-1.7l1.4 1.4L6 9.5a3 3 0 013-1 1 5.5 4 4 0 015-4 4 1v12h2a3 3 0 013-1 1h-2v-7a1 1 0 00-1 1H6a3 3 0 013-1 1z"/></svg>;
      default:
        return <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4-4V2a2 2 0 00-2-2h12a2 2 0 002 2v5a2 2 0 002-2H6a2 2 0 00-2-2zm0 14a4 4 0 11-8 0 4-4v-2h3v6h3V2zm0 14a4 4 0 11-8 0 4-4v-2h3v6h3V2zm0 14a4 4 0 11-8 0 4-4v-2h3v6h3V2z"/></svg>;
    }
  };

  return (
    <div className="space-y-4 p-6 bg-white border border-gray-200 rounded-2xl">
      {/* Header com avatar e botão editar */}
      <div className="flex items-start gap-4">
        {/* Avatar */}
        <div className="relative">
          {(isEditing ? editedAvatarUrl : avatarUrl) ? (
            <img
              src={isEditing ? editedAvatarUrl : avatarUrl!}
              alt={username}
              className="w-24 h-24 rounded-full object-cover border-4 border-gray-200 shadow-lg"
            />
          ) : (
            <div className="w-24 h-24 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-4xl font-bold border-4 border-gray-200 shadow-lg">
              {username.charAt(0).toUpperCase()}
            </div>
          )}

          {/* Edit button overlay quando em modo visualização */}
          {!isEditing && (
            <button
              onClick={onEdit}
              className="absolute -bottom-1 -right-1 w-8 h-8 bg-white rounded-full shadow-lg flex items-center justify-center border border-gray-200 hover:bg-gray-50 transition-colors"
              aria-label="Editar perfil"
              style={{ minHeight: '44px', minWidth: '44px' }}
            >
              <svg className="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l-4.973 3.768 4.973 8.868A3 3 0 015.232-8.768V19a3 3 0 013-3 3H5.232a3 3 0 013-3 3V8.768c0-1.555.673-3-3.642 3.642l4.972 4.972 3.642 8.642a3 3 0 013.642-3.642v1.232h-6v-6a3 3 0 00-3-3H4.642a3 3 0 013-3-3v6c0-1.555.673-3 3.642 3.642z" />
              </svg>
            </button>
          )}
        </div>

        {/* Info básica e botões de edição */}
        <div className="flex-1">
          <h1 className="text-2xl font-bold text-gray-900 mb-1">Perfil</h1>
          
          {!isEditing ? (
            <div className="flex gap-2">
              <button
                onClick={onEdit}
                className="px-4 py-2 min-h-[44px] rounded-lg bg-blue-600 text-white font-medium hover:bg-blue-700 transition-colors"
              >
                Editar
              </button>
            </div>
          ) : (
            <div className="flex gap-2">
              <button
                onClick={onCancelEdit}
                className="px-4 py-2 min-h-[44px] rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleSave}
                disabled={isSaving}
                className="px-4 py-2 min-h-[44px] rounded-lg bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:bg-blue-300 disabled:opacity-50 transition-colors"
              >
                {isSaving ? 'Salvando...' : 'Salvar'}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Campos de visualização */}
      {!isEditing && (
        <div className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Nome de Usuário</label>
            <p className="text-lg text-gray-900 font-medium">@{username}</p>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Email</label>
            <p className="text-lg text-gray-900">{email}</p>
          </div>

          {name && (
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">Nome</label>
              <p className="text-lg text-gray-900">{name}</p>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Método de Autenticação</label>
            <div className="flex items-center gap-2 text-gray-900">
              {getAuthProviderIcon()}
              <span className="capitalize">
                {authProvider === 'GOOGLE' ? 'Google' : 'Email/Senha'}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Campos de edição */}
      {isEditing && (
        <div className="space-y-4">
          {/* Nome editável */}
          <div>
            <label htmlFor="profile-name" className="block text-sm font-medium text-gray-600 mb-1">
              Nome <span className="text-red-500">*</span>
            </label>
            <input
              id="profile-name"
              type="text"
              value={editedName}
              onChange={(e) => setEditedName(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Seu nome"
              maxLength={100}
              data-testid="profile-name-input"
            />
            <p className="text-xs text-gray-500 mt-1">Máximo 100 caracteres</p>
          </div>

          {/* URL do avatar */}
          <div>
            <label htmlFor="avatar-url" className="block text-sm font-medium text-gray-600 mb-1">
              URL da Foto de Perfil
            </label>
            <input
              id="avatar-url"
              type="url"
              value={editedAvatarUrl}
              onChange={(e) => setEditedAvatarUrl(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="https://exemplo.com/minha-foto.jpg"
              data-testid="avatar-url-input"
            />
            {editedAvatarUrl && (
              <div className="mt-3 flex items-center gap-3">
                <img
                  src={editedAvatarUrl}
                  alt="Preview"
                  className="w-12 h-12 rounded-full object-cover border-2 border-gray-200"
                  onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                />
                <button
                  onClick={() => setEditedAvatarUrl('')}
                  className="px-3 py-1 min-h-[44px] rounded-lg border border-gray-300 hover:bg-gray-50 text-sm"
                >
                  Remover
                </button>
              </div>
            )}
          </div>

          {/* Mensagem informativa */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-sm text-blue-700 flex items-start gap-2">
              <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 8 8 013.087.586.172 4.428.429.429 5.429.429 5.429.429 4.428a1.75 1.75 0 003.183-3.25 1.75 4.428.429 4.429 4.428.429.429 4.428.429 5.429.429 4.428zM10 18a8 8 0 11-16 8 8 013.087.586.172 4.428.429.429 5.429.429 5.429.429 4.428a1.75 1.75 0 003.183-3.25 1.75 4.428.429 4.429 5.429.429 4.428.429 4.428zM2.5 5.5a.5.5 0 013.636-2.25.607.463 1.25 1.25v.607a.5.5.5 0 01.5.5 1.25v3.643h4.286V5.75a.5.5.5 0 01.5.5 1.25H2.5a.5.5.5 0 01.5.5 1.25v3.643z" clipRule="evenodd" />
              </svg>
              <span>
                <strong>Nota:</strong> Username e email não podem ser alterados.
              </span>
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
