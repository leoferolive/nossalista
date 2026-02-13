import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import client from '../api/client';

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
        localStorage.setItem('authToken', token);

        const { data } = await client.get<CurrentUserResponse>('/api/users/me');
        login(token, {
          id: data.id,
          username: data.username,
          email: data.email,
          displayName: data.name,
          avatarUrl: data.avatarUrl,
        });
        navigate('/', { replace: true });
      } catch {
        localStorage.removeItem('authToken');
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
