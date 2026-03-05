import { useState, useEffect, useCallback } from 'react'
import { listsApi } from '../api/listsApi'
import { ActivityLog } from '../types/ActivityLog'

interface UseActivitiesReturn {
  activities: ActivityLog[]
  loading: boolean
  error: string | null
  hasMore: boolean
  currentPage: number
  loadMore: () => Promise<void>
  refresh: () => Promise<void>
  clearError: () => void
}

/**
 * Hook para gerenciar atividades de uma lista
 * AC: Carrega atividades com paginação, suporta loading incremental
 * @param enabled - quando false, não dispara fetch (carregamento lazy)
 */
export const useActivities = (listId: string, enabled = true): UseActivitiesReturn => {
  const [activities, setActivities] = useState<ActivityLog[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const loadMore = useCallback(async () => {
    if (loading || !listId || currentPage >= totalPages) {
      return
    }

    setLoading(true)
    setError(null)

    try {
      const response = await listsApi.getActivities(listId, currentPage, 50)
      setActivities((prev) => [...prev, ...response.content])
      setCurrentPage(response.number + 1)
      setTotalPages(response.totalPages)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar atividades.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }, [listId, currentPage, totalPages, loading])

  const refresh = useCallback(async () => {
    if (!listId) {
      return
    }

    setLoading(true)
    setError(null)

    try {
      const response = await listsApi.getActivities(listId, 0, 50)
      setActivities(response.content)
      setCurrentPage(response.number + 1)
      setTotalPages(response.totalPages)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar atividades.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }, [listId])

  useEffect(() => {
    if (listId && enabled) {
      refresh()
    }
  }, [listId, enabled, refresh])

  const clearError = useCallback(() => {
    setError(null)
  }, [])

  return {
    activities,
    loading,
    error,
    hasMore: currentPage < totalPages,
    currentPage,
    loadMore,
    refresh,
    clearError,
  }
}
