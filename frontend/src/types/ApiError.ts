/**
 * Custom error class para incluir HTTP status code e RFC 7807 type
 * Permite error handling baseado em status em vez de string matching
 */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number = 0,
    public readonly type?: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}
