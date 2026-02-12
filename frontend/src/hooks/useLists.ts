import { useState, useCallback } from 'react';
import { listsApi } from '../api/listsApi';
import { ListResponse, CreateListRequest } from '../types/List';
import { ProblemDetail } from '../types/ProblemDetail';
import { AxiosError } from 'axios';

interface UseListsReturn {
  lists: ListResponse[];
  loading: boolean;
  error: string | null;
  fetchLists: () => Promise<void>;
  createList: (request: CreateListRequest) => Promise<ListResponse>;
  clearError: () => void;
}

/**
 * Hook para gerenciar estado de listas
 */
export const useLists = (): UseListsReturn => {
  const [lists, setLists] = useState<ListResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Busca todas as listas do usuário
   */
  const fetchLists = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await listsApi.getAllLists();
      setLists(data);
    } catch (err) {
      const axiosError = err as AxiosError<ProblemDetail>;
      const message =
        axiosError.response?.data?.detail ||
        'Erro ao carregar listas. Tente novamente.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Cria uma nova lista
   */
  const createList = useCallback(
    async (request: CreateListRequest): Promise<ListResponse> => {
      setLoading(true);
      setError(null);

      try {
        const newList = await listsApi.createList(request);
        setLists((prev) => [newList, ...prev]);
        return newList;
      } catch (err) {
        const axiosError = err as AxiosError<ProblemDetail>;
        const message =
          axiosError.response?.data?.detail ||
          'Erro ao criar lista. Tente novamente.';
        setError(message);
        throw new Error(message);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  /**
   * Limpa o erro atual
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    lists,
    loading,
    error,
    fetchLists,
    createList,
    clearError,
  };
};
