import axios, { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { clearStoredSession, getStoredAuthToken } from '../auth/session';

export const preserveSessionOnUnauthorizedConfig: AxiosRequestConfig = {
  preserveSessionOnUnauthorized: true,
};

/**
 * Instância configurada do Axios para comunicação com a API
 */
const client: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Interceptor para adicionar JWT token automaticamente em todas as requisições
 */
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getStoredAuthToken();

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Interceptor para tratamento de erros de resposta
 * Redireciona para /login quando token é inválido ou expirado
 */
client.interceptors.response.use(
  (response) => response,
  (error) => {
    // Se 401 Unauthorized, limpar token e redirecionar para login
    if (error.response?.status === 401 && !error.config?.preserveSessionOnUnauthorized) {
      clearStoredSession();

      // Redirecionar para login (evita redirecionamento se já estiver na página de login)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export default client;
