import axios from 'axios'
import { ApiError } from '../types/ApiError'

/**
 * Centraliza o tratamento de erros de API.
 * Extrai campos RFC 7807 (ProblemDetail) de respostas Axios e
 * lanca ApiError com status, message e type.
 *
 * Uso:
 *   try { ... } catch (error) { handleApiError(error) }
 */
export function handleApiError(error: unknown): never {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status ?? 0
    const data = error.response?.data as Record<string, unknown> | undefined
    const detail = (data?.detail as string) ?? error.message
    const type = (data?.type as string) ?? undefined
    throw new ApiError(detail, status, type)
  }
  if (error instanceof Error) {
    throw new ApiError(error.message, 0)
  }
  throw new ApiError('Erro desconhecido', 0)
}
