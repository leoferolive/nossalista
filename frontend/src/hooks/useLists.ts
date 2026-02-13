import { useState, useCallback } from 'react';
import { listsApi } from '../api/listsApi';
import { ListResponse, CreateListRequest } from '../types/List';
import { ProblemDetail } from '../types/ProblemDetail';
import { AxiosError } from 'axios';

interface UseListsReturn {
  // List states (for lists grid)
  lists: ListResponse[];
  loading: boolean;
  error: string | null;

  // Single list states (for detail view)
  currentList: ListResponse | null;
  loadingList: boolean;
  errorList: string | null;

  // Update state
  updatingList: boolean;

  // Delete state
  deletingList: boolean;

  // Actions
  fetchLists: () => Promise<void>;
  fetchListById: (id: string) => Promise<void>;
  createList: (request: CreateListRequest) => Promise<ListResponse>;
  updateListName: (id: string, name: string) => Promise<ListResponse>;
  deleteList: (id: string) => Promise<boolean>;
  clearError: () => void;
  clearListError: () => void;
}

/**
 * Hook para gerenciar estado de listas
 */
export const useLists = (): UseListsReturn => {
  // List grid states
  const [lists, setLists] = useState<ListResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Single list states
  const [currentList, setCurrentList] = useState<ListResponse | null>(null);
  const [loadingList, setLoadingList] = useState(false);
  const [errorList, setErrorList] = useState<string | null>(null);

  // Update state
  const [updatingList, setUpdatingList] = useState(false);

  // Delete state
  const [deletingList, setDeletingList] = useState(false);

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
   * Busca uma lista específica por ID
   * Error handling é feito pela API layer (listsApi.getListById)
   */
  const fetchListById = useCallback(async (id: string) => {
    setLoadingList(true);
    setErrorList(null);
    setCurrentList(null);

    try {
      const data = await listsApi.getListById(id);
      setCurrentList(data);
    } catch (err) {
      // listsApi.getListById já fornece mensagens específicas por status code
      const message = err instanceof Error ? err.message : 'Erro desconhecido ao carregar lista';
      setErrorList(message);
    } finally {
      setLoadingList(false);
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
   * Atualiza o nome de uma lista existente
   */
  const updateListName = useCallback(
    async (id: string, name: string): Promise<ListResponse> => {
      setUpdatingList(true);
      setErrorList(null);

      try {
        const updatedList = await listsApi.updateListName(id, name);
        // Atualiza currentList com os novos dados
        setCurrentList(updatedList);
        // Atualiza a lista no array de listas (se existir)
        setLists((prev) =>
          prev.map((list) => (list.id === id ? updatedList : list))
        );
        return updatedList;
      } catch (err) {
        const message =
          err instanceof Error ? err.message : 'Erro ao atualizar lista';
        setErrorList(message);
        throw new Error(message);
      } finally {
        setUpdatingList(false);
      }
    },
    []
  );

  /**
   * Exclui uma lista existente
   */
  const deleteList = useCallback(
    async (id: string): Promise<boolean> => {
      setDeletingList(true);
      setErrorList(null);

      try {
        await listsApi.deleteList(id);
        // Remove a lista do array de listas
        setLists((prev) => prev.filter((list) => list.id !== id));
        // Limpa currentList se a lista deletada era a atual
        if (currentList?.id === id) {
          setCurrentList(null);
        }
        return true;
      } catch (err) {
        const message =
          err instanceof Error ? err.message : 'Erro ao excluir lista';
        setErrorList(message);
        throw new Error(message);
      } finally {
        setDeletingList(false);
      }
    },
    [currentList]
  );

  /**
   * Limpa o erro da lista de listas
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  /**
   * Limpa o erro da lista individual
   */
  const clearListError = useCallback(() => {
    setErrorList(null);
  }, []);

  return {
    lists,
    loading,
    error,
    currentList,
    loadingList,
    errorList,
    updatingList,
    deletingList,
    fetchLists,
    fetchListById,
    createList,
    updateListName,
    deleteList,
    clearError,
    clearListError,
  };
};
