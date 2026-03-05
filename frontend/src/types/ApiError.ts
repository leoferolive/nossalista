/**
 * Custom error class para incluir HTTP status code
 * Permite error handling baseado em status em vez de string matching
 */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number
  ) {
    super(message)
    this.name = 'ApiError'
  }
}
