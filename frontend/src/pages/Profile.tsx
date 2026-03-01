import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserProfile } from '../components/UserProfile';
import { useToast } from '../components/Toast';
import { usersApi } from '../api/usersApi';
import { ApiError } from '../types/ApiError';

/**
 * Página de Perfil do Usuário
 * FR4: Usuário pode acessar seu próprio perfil
 * FR5: Usuário pode atualizar informações do próprio perfil
 */
export const Profile: React.FC = () => {
  const navigate = useNavigate();
  const { toasts, showToast, removeToast } = useToast();

  const [userData, setUserData] = useState({
    username: '',
    email: '',
    name: null as string | null,
    avatarUrl: null as string | null,
    authProvider: '' as string,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [updating, setUpdating] = useState(false);

  // Carregar dados do perfil ao montar
  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true);
        setError(null);
        const profile = await usersApi.getProfile();
        setUserData({
          username: profile.username,
          email: profile.email,
          name: profile.name,
          avatarUrl: profile.avatarUrl,
          authProvider: profile.authProvider,
        });
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Erro ao carregar perfil';
        setError(message);
        showToast(message, 'error');
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  const handleUpdateProfile = useCallback(async (data: { name: string; avatarUrl: string }) => {
    setUpdating(true);
    try {
      await usersApi.updateProfile(data);
      // Atualizar estado local
      setUserData((prev) => ({
        ...prev,
        name: data.name,
        avatarUrl: data.avatarUrl || prev.avatarUrl,
      }));
      showToast('Perfil atualizado com sucesso!', 'success');
      setIsEditing(false);
    } catch (err) {
      const apiError = err as ApiError;
      const message = apiError?.message || 'Erro ao atualizar perfil. Tente novamente.';
      showToast(message, 'error');
      // Permanecer em modo de edição em caso de erro
    } finally {
      setUpdating(false);
    }
  }, []);

  const handleLogout = useCallback(async () => {
    try {
      await usersApi.logout();
      // Limpar tokens e redirecionar para login
      localStorage.removeItem('token');
      showToast('Até logo!', 'info');
      navigate('/login');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao fazer logout';
      showToast(message, 'error');
    }
  }, [navigate, showToast]);

  // Loading state
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-8">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-8 max-w-md text-center">
          <svg className="w-16 h-16 text-red-400 mx-auto mb-4" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 11-16 8 8 013.087.586.172 4.428.429.429 5.429.429 5.429.429 4.428a1.75 1.75 0 003.183-3.25 1.75 4.428.429 4.429 5.429.429 4.428zM10 18a8 8 0 11-16 8 8 013.087.586.172 4.428.429.429 5.429.429 5.429.429 4.428a1.75 1.75 0 003.183-3.25 1.75 4.428.429 4.429 5.429.429 4.428zM2.5 5.5a.5.5 0 013.636-2.25.607.463 1.25 1.25v.607a.5.5.5 0 01.5.5 1.25v3.643h4.286V5.75a.5.5.5 0 01.5.5 1.25H2.5a.5.5.5 0 01.5.5 1.25v3.643z" clipRule="evenodd" />
          </svg>
          <h2 className="text-xl font-bold text-red-900 mb-2">Erro ao Carregar Perfil</h2>
          <p className="text-red-700 mb-4">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-6 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
          >
            Tentar Novamente
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-2xl mx-auto">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900">Meu Perfil</h1>
          <button
            onClick={() => navigate(-1)}
            className="p-3 min-w-[44px] min-h-[44px] hover:bg-gray-200 rounded-lg transition-colors"
            aria-label="Voltar"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-6 w-6 text-gray-700"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10 19l-7-7m0 0l7-7m-7 7l-7 7"
              />
            </svg>
          </button>
        </div>

        {/* UserProfile component */}
        <UserProfile
          username={userData.username}
          email={userData.email}
          name={userData.name}
          avatarUrl={userData.avatarUrl}
          authProvider={userData.authProvider}
          isEditing={isEditing}
          onEdit={() => setIsEditing(true)}
          onCancelEdit={() => setIsEditing(false)}
          onSave={handleUpdateProfile}
        />

        {/* Botão Logout */}
        <div className="mt-8 pt-6 border-t border-gray-200">
          <button
            onClick={handleLogout}
            disabled={updating}
            className="w-full px-4 py-3 min-h-[48px] rounded-lg bg-red-600 text-white font-semibold hover:bg-red-700 disabled:bg-red-300 disabled:opacity-50 transition-colors"
            aria-label="Sair da conta"
          >
            {updating ? 'Saindo...' : 'Sair da Conta'}
          </button>
        </div>

        {/* Toasts */}
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`fixed top-4 right-4 z-50 ${
              toast.type === 'success'
                ? 'bg-green-500'
                : toast.type === 'error'
                  ? 'bg-red-500'
                  : 'bg-blue-500'
            } text-white px-6 py-3 rounded-lg shadow-lg flex items-center gap-3 max-w-md`}
          >
            <span className="text-sm font-medium">{toast.message}</span>
            <button
              onClick={() => removeToast(toast.id)}
              className="ml-2 text-white hover:text-gray-200 focus:outline-none"
              aria-label="Fechar notificação"
            >
              ✕
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
