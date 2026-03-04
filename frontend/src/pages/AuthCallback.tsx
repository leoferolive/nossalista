import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import client from '../api/client';
import { listsApi } from '../api/listsApi';
import { ApiError } from '../types/ApiError';
import { clearStoredSession, persistAuthToken } from '../auth/session';

interface CurrentUserResponse {
  id: string;
  username: string;
  email: string;
  name: string;
  avatarUrl?: string;
}

export function AuthCallback() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const [error, setError] = useState('');
  const hasProcessedRef = useRef(false);

  useEffect(() => {
    if (hasProcessedRef.current) {
      return;
    }
    hasProcessedRef.current = true;

    const token = searchParams.get('token');

    if (!token) {
      setError('Token de autenticação não encontrado.');
      return;
    }

    const finishAuth = async () => {
      try {
        // Necessário para que o interceptor envie Authorization ao carregar o perfil.
        persistAuthToken(token);

        const { data } = await client.get<CurrentUserResponse>('/api/users/me');
        login(token, {
          id: data.id,
          username: data.username,
          email: data.email,
          displayName: data.name,
          avatarUrl: data.avatarUrl,
        });

        // Verificar se há um pending invite code para processar
        const pendingInviteCode = sessionStorage.getItem('pendingInviteCode');

        if (pendingInviteCode) {
          try {
            // Tentar entrar na lista automaticamente
            const joinResponse = await listsApi.joinList(pendingInviteCode);

            // Limpar o código pendente
            sessionStorage.removeItem('pendingInviteCode');

            // Redirecionar para a lista passando a mensagem de boas-vindas via state
            navigate(`/lists/${joinResponse.id}`, {
              replace: true,
              state: { toastMessage: joinResponse.message, toastType: 'success' },
            });
            return;
          } catch (error) {
            // Limpar o código pendente mesmo em caso de erro
            sessionStorage.removeItem('pendingInviteCode');

            if (error instanceof ApiError && error.status === 410) {
              setError('Link de convite expirou. Peça um novo link.');
            } else {
              // Redirecionar para home com mensagem de erro via state
              navigate('/', {
                replace: true,
                state: { toastMessage: 'Erro ao entrar na lista. Tente novamente.', toastType: 'error' },
              });
            }
            return;
          }
        }

        // Se não há pending invite, redirecionar para home normalmente
        navigate('/', { replace: true });
      } catch {
        clearStoredSession();
        sessionStorage.removeItem('pendingInviteCode');
        setError('Não foi possível concluir o login com Google.');
      }
    };

    void finishAuth();
  }, [searchParams, login, navigate]);

  if (error) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
        <div className="bg-white p-6 rounded-lg shadow-md w-full max-w-md text-center">
          <h1 className="text-xl font-bold text-gray-800 mb-2">Falha no login</h1>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            type="button"
            onClick={() => navigate('/login', { replace: true })}
            className="w-full bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded"
          >
            Voltar para login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="bg-white p-6 rounded-lg shadow-md w-full max-w-md text-center">
        <h1 className="text-xl font-bold text-gray-800 mb-2">Concluindo login...</h1>
        <p className="text-gray-600">Aguarde um instante.</p>
      </div>
    </div>
  );
}
