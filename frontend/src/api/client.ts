import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

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
    const token = localStorage.getItem('authToken');

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
 */
client.interceptors.response.use(
  (response) => response,
  (error) => {
    // Se 401 Unauthorized, limpar token e redirecionar para login
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken');
      // TODO: Redirecionar para /login quando router estiver configurado
      console.error('Token inválido ou expirado. Faça login novamente.');
    }

    return Promise.reject(error);
  }
);

export default client;
