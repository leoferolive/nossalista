/**
 * RFC 7807 Problem Details for HTTP APIs
 * https://datatracker.ietf.org/doc/html/rfc7807
 */
export interface ProblemDetail {
  /**
   * URI que identifica o tipo do problema
   */
  type: string;

  /**
   * Título curto e legível do problema
   */
  title: string;

  /**
   * Código de status HTTP
   */
  status: number;

  /**
   * Explicação detalhada específica para esta ocorrência
   */
  detail?: string;

  /**
   * URI que identifica a instância específica do problema
   */
  instance?: string;

  /**
   * Propriedades adicionais específicas do erro
   */
  [key: string]: unknown;
}
