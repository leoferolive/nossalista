import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosResponse } from 'axios'
import { AxiosError, AxiosHeaders } from 'axios'
import { itemsApi } from './itemsApi'
import client from './client'
import type { ListItem } from '../types/Item'

function buildItem(overrides: Partial<ListItem> = {}): ListItem {
  return {
    id: 'item-1',
    name: 'Arroz',
    checked: false,
    quantity: 2,
    dueDate: null,
    url: null,
    position: 0,
    createdBy: { id: 'user-1', username: 'leo', name: 'Leo', avatarUrl: null },
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function buildAxiosError(status: number): AxiosError {
  const response = {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: { detail: 'Falhou' },
  } as AxiosResponse

  return new AxiosError(
    `Request failed with status code ${status}`,
    String(status),
    response.config,
    undefined,
    response
  )
}

describe('itemsApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('getItemsByListId retorna os itens da lista', async () => {
    const items = [buildItem()]
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: items } as AxiosResponse)

    await expect(itemsApi.getItemsByListId('list-1')).resolves.toEqual(items)
    expect(client.get).toHaveBeenCalledWith('/api/lists/list-1/items', expect.any(Object))
  })

  it('getItemsByListId propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(404))

    await expect(itemsApi.getItemsByListId('list-1')).rejects.toMatchObject({ status: 404 })
  })

  it('addItem cria um item na lista', async () => {
    const created = buildItem()
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: created } as AxiosResponse)

    const result = await itemsApi.addItem('list-1', { name: 'Arroz' })

    expect(result).toEqual(created)
    expect(client.post).toHaveBeenCalledWith(
      '/api/lists/list-1/items',
      { name: 'Arroz' },
      expect.any(Object)
    )
  })

  it('addItem propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(400))

    await expect(itemsApi.addItem('list-1', { name: '' })).rejects.toMatchObject({ status: 400 })
  })

  it('toggleItemCheck alterna o status de conclusao', async () => {
    const updated = buildItem({ checked: true })
    vi.spyOn(client, 'patch').mockResolvedValueOnce({ data: updated } as AxiosResponse)

    await expect(itemsApi.toggleItemCheck('list-1', 'item-1')).resolves.toEqual(updated)
    expect(client.patch).toHaveBeenCalledWith(
      '/api/lists/list-1/items/item-1/check',
      undefined,
      expect.any(Object)
    )
  })

  it('toggleItemCheck propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'patch').mockRejectedValueOnce(buildAxiosError(404))

    await expect(itemsApi.toggleItemCheck('list-1', 'item-1')).rejects.toMatchObject({
      status: 404,
    })
  })

  it('updateItem atualiza um item existente', async () => {
    const updated = buildItem({ name: 'Feijao' })
    vi.spyOn(client, 'patch').mockResolvedValueOnce({ data: updated } as AxiosResponse)

    const result = await itemsApi.updateItem('list-1', 'item-1', { name: 'Feijao' })

    expect(result).toEqual(updated)
    expect(client.patch).toHaveBeenCalledWith(
      '/api/lists/list-1/items/item-1',
      { name: 'Feijao' },
      expect.any(Object)
    )
  })

  it('updateItem propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'patch').mockRejectedValueOnce(buildAxiosError(400))

    await expect(itemsApi.updateItem('list-1', 'item-1', { name: '' })).rejects.toMatchObject({
      status: 400,
    })
  })

  it('deleteItem remove um item da lista', async () => {
    vi.spyOn(client, 'delete').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await itemsApi.deleteItem('list-1', 'item-1')

    expect(client.delete).toHaveBeenCalledWith('/api/lists/list-1/items/item-1', expect.any(Object))
  })

  it('deleteItem propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'delete').mockRejectedValueOnce(buildAxiosError(404))

    await expect(itemsApi.deleteItem('list-1', 'item-1')).rejects.toMatchObject({ status: 404 })
  })
})
