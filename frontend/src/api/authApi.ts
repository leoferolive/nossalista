import { AxiosError } from 'axios';
import client from './client';
import { ProblemDetail } from '../types/ProblemDetail';

export interface RegisterPayload {
  email: string;
  username: string;
  password: string;
  name?: string;
}

export interface RegisterResponse {
  id: string;
  username: string;
  email: string;
  name: string | null;
  avatarUrl: string | null;
  authProvider: string;
  createdAt: string;
}

function extractProblemMessage(error: unknown, fallback: string) {
  const axiosError = error as AxiosError<ProblemDetail>;
  return axiosError.response?.data?.detail || fallback;
}

export const authApi = {
  async register(payload: RegisterPayload): Promise<RegisterResponse> {
    try {
      const response = await client.post<RegisterResponse>('/api/auth/register', payload);
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response?.status === 409) {
        throw new Error(extractProblemMessage(error, 'Email ou username ja estao em uso.'));
      }

      if (axiosError.response?.status === 400) {
        throw new Error(extractProblemMessage(error, 'Confira os dados informados e tente novamente.'));
      }

      throw new Error(extractProblemMessage(error, 'Nao foi possivel criar sua conta agora.'));
    }
  },
};
