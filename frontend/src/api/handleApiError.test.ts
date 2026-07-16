import { describe, expect, it } from 'vitest'
import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import { handleApiError } from './handleApiError'
import { ApiError } from '../types/ApiError'

describe('handleApiError', () => {
  it('lanca ApiError extraindo detail e type de um AxiosError com ProblemDetail', () => {
    const response = {
      status: 404,
      statusText: '',
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: { type: 'https://example.com/not-found', title: 'Not Found', detail: 'Lista nao encontrada.' },
    } as AxiosResponse

    const axiosError = new AxiosError('Request failed', '404', response.config, undefined, response)

    expect(() => handleApiError(axiosError)).toThrow(ApiError)
    try {
      handleApiError(axiosError)
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect((err as ApiError).status).toBe(404)
      expect((err as ApiError).message).toBe('Lista nao encontrada.')
      expect((err as ApiError).type).toBe('https://example.com/not-found')
    }
  })

  it('usa a mensagem do AxiosError quando nao ha detail no corpo', () => {
    const response = {
      status: 500,
      statusText: '',
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: undefined,
    } as AxiosResponse

    const axiosError = new AxiosError('Network Error', undefined, response.config, undefined, response)

    try {
      handleApiError(axiosError)
      throw new Error('deveria ter lancado')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect((err as ApiError).status).toBe(500)
      expect((err as ApiError).message).toBe('Network Error')
    }
  })

  it('usa status 0 quando AxiosError nao tem response (erro de rede)', () => {
    const axiosError = new AxiosError('Network Error')

    try {
      handleApiError(axiosError)
      throw new Error('deveria ter lancado')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect((err as ApiError).status).toBe(0)
    }
  })

  it('envolve um Error generico com status 0', () => {
    try {
      handleApiError(new Error('algo quebrou'))
      throw new Error('deveria ter lancado')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect((err as ApiError).status).toBe(0)
      expect((err as ApiError).message).toBe('algo quebrou')
    }
  })

  it('usa mensagem generica para valores que nao sao Error', () => {
    try {
      handleApiError('string qualquer')
      throw new Error('deveria ter lancado')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect((err as ApiError).message).toBe('Erro desconhecido')
      expect((err as ApiError).status).toBe(0)
    }
  })
})
