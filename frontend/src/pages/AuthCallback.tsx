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
      <div className="nl-page flex items-center justify-center p-4">
        <div className="nl-card w-full max-w-md p-6 text-center">
          <h1 className="mb-2 font-display text-xl font-bold text-slate-800">Falha no Login</h1>
          <p className="mb-4 text-slate-600">{error}</p>
          <button
            type="button"
            onClick={() => navigate('/login', { replace: true })}
            className="w-full rounded-xl bg-gradient-to-r from-orange-500 to-amber-500 px-4 py-2.5 font-semibold text-white transition-colors hover:from-orange-600 hover:to-amber-600 focus-visible:ring-2 focus-visible:ring-orange-300"
          >
            Voltar Para Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="nl-page flex items-center justify-center p-4">
      <div className="nl-card w-full max-w-md p-6 text-center">
        <h1 className="mb-2 font-display text-xl font-bold text-slate-800">Concluindo Login…</h1>
        <p className="text-slate-600">Aguarde um Instante.</p>
      </div>
    </div>
  );
}
