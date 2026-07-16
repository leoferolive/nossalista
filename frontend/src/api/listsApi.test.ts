import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosResponse } from 'axios'
import { AxiosError, AxiosHeaders } from 'axios'
import { listsApi } from './listsApi'
import client from './client'

function buildAxiosError(status: number): AxiosError {
  const response = {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: { detail: 'Falhou' },
  } as AxiosResponse

  return new AxiosError(`Request failed with status code ${status}`, String(status), response.config, undefined, response)
}

describe('listsApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('createList cria uma lista nova', async () => {
    const created = { id: 'list-1', name: 'Mercado' }
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: created } as AxiosResponse)

    await expect(listsApi.createList({ name: 'Mercado', typeId: 1 })).resolves.toEqual(created)
    expect(client.post).toHaveBeenCalledWith('/api/lists', { name: 'Mercado', typeId: 1 }, expect.any(Object))
  })

  it('getAllLists retorna as listas do usuario', async () => {
    const lists = [{ id: 'list-1', name: 'Mercado' }]
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: lists } as AxiosResponse)

    await expect(listsApi.getAllLists()).resolves.toEqual(lists)
    expect(client.get).toHaveBeenCalledWith('/api/lists', expect.any(Object))
  })

  it('getListById retorna uma lista especifica', async () => {
    const list = { id: 'list-1', name: 'Mercado' }
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: list } as AxiosResponse)

    await expect(listsApi.getListById('list-1')).resolves.toEqual(list)
  })

  it('getListById propaga ApiError quando a lista nao existe', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(404))

    await expect(listsApi.getListById('list-1')).rejects.toMatchObject({ status: 404 })
  })

  it('getListState retorna a revision atual da lista', async () => {
    const state = { listId: 'list-1', revision: 3, updatedAt: '2026-01-01T00:00:00Z', itemsCount: 2 }
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: state } as AxiosResponse)

    await expect(listsApi.getListState('list-1')).resolves.toEqual(state)
  })

  it('getListState propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(500))

    await expect(listsApi.getListState('list-1')).rejects.toMatchObject({ status: 500 })
  })

  it('updateListName atualiza o nome da lista', async () => {
    const updated = { id: 'list-1', name: 'Novo nome' }
    vi.spyOn(client, 'patch').mockResolvedValueOnce({ data: updated } as AxiosResponse)

    await expect(listsApi.updateListName('list-1', 'Novo nome')).resolves.toEqual(updated)
    expect(client.patch).toHaveBeenCalledWith('/api/lists/list-1', { name: 'Novo nome' }, expect.any(Object))
  })

  it('updateListName propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'patch').mockRejectedValueOnce(buildAxiosError(400))

    await expect(listsApi.updateListName('list-1', '')).rejects.toMatchObject({ status: 400 })
  })

  it('deleteList exclui a lista', async () => {
    vi.spyOn(client, 'delete').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await listsApi.deleteList('list-1')

    expect(client.delete).toHaveBeenCalledWith('/api/lists/list-1', expect.any(Object))
  })

  it('deleteList propaga ApiError quando o usuario nao e dono', async () => {
    vi.spyOn(client, 'delete').mockRejectedValueOnce(buildAxiosError(403))

    await expect(listsApi.deleteList('list-1')).rejects.toMatchObject({ status: 403 })
  })

  it('generateInviteLink retorna um link de convite', async () => {
    const invite = { inviteCode: 'abc123', inviteLink: '/join/abc123', expiresAt: '2026-01-02T00:00:00Z' }
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: invite } as AxiosResponse)

    await expect(listsApi.generateInviteLink('list-1')).resolves.toEqual(invite)
  })

  it('generateInviteLink propaga ApiError quando o usuario nao e dono', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(403))

    await expect(listsApi.generateInviteLink('list-1')).rejects.toMatchObject({ status: 403 })
  })

  it('getListByInviteCode retorna a lista em modo leitura', async () => {
    const joinList = { id: 'list-1', name: 'Mercado', items: [] }
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: joinList } as AxiosResponse)

    await expect(listsApi.getListByInviteCode('abc123')).resolves.toEqual(joinList)
    expect(client.get).toHaveBeenCalledWith('/api/lists/join/abc123')
  })

  it('getListByInviteCode propaga ApiError quando o convite nao existe', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(404))

    await expect(listsApi.getListByInviteCode('abc123')).rejects.toMatchObject({ status: 404 })
  })

  it('joinList entra na lista via codigo de convite', async () => {
    const joined = { id: 'list-1', message: 'Bem-vindo!' }
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: joined } as AxiosResponse)

    await expect(listsApi.joinList('abc123')).resolves.toEqual(joined)
    expect(client.post).toHaveBeenCalledWith('/api/lists/join/abc123', undefined, expect.any(Object))
  })

  it('joinList propaga ApiError quando o convite expirou', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(410))

    await expect(listsApi.joinList('abc123')).rejects.toMatchObject({ status: 410 })
  })

  it('searchUsers busca usuarios por query', async () => {
    const users = [{ id: 'user-1', username: 'leo', name: 'Leo', avatarUrl: null }]
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: users } as AxiosResponse)

    await expect(listsApi.searchUsers('leo')).resolves.toEqual(users)
    expect(client.get).toHaveBeenCalledWith('/api/users/search', expect.objectContaining({ params: { q: 'leo' } }))
  })

  it('searchUsers propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(500))

    await expect(listsApi.searchUsers('leo')).rejects.toMatchObject({ status: 500 })
  })

  it('inviteByUsername convida um usuario existente', async () => {
    const response = { invited: true }
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: response } as AxiosResponse)

    await expect(listsApi.inviteByUsername('list-1', 'leo')).resolves.toEqual(response)
    expect(client.post).toHaveBeenCalledWith('/api/lists/list-1/invite', { username: 'leo' }, expect.any(Object))
  })

  it('inviteByUsername propaga ApiError quando o usuario nao existe', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(404))

    await expect(listsApi.inviteByUsername('list-1', 'inexistente')).rejects.toMatchObject({ status: 404 })
  })

  it('getListMembers retorna os membros da lista', async () => {
    const members = [{ id: 'user-1', username: 'leo', name: 'Leo', avatarUrl: null, isOwner: true }]
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: members } as AxiosResponse)

    await expect(listsApi.getListMembers('list-1')).resolves.toEqual(members)
  })

  it('getListMembers propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(404))

    await expect(listsApi.getListMembers('list-1')).rejects.toMatchObject({ status: 404 })
  })

  it('deleteListMember remove um membro da lista', async () => {
    vi.spyOn(client, 'delete').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await listsApi.deleteListMember('list-1', 'user-2')

    expect(client.delete).toHaveBeenCalledWith('/api/lists/list-1/members/user-2', expect.any(Object))
  })

  it('deleteListMember propaga ApiError quando o usuario nao e dono', async () => {
    vi.spyOn(client, 'delete').mockRejectedValueOnce(buildAxiosError(403))

    await expect(listsApi.deleteListMember('list-1', 'user-2')).rejects.toMatchObject({ status: 403 })
  })

  it('leaveList sai da lista', async () => {
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await listsApi.leaveList('list-1')

    expect(client.post).toHaveBeenCalledWith('/api/lists/list-1/leave', undefined, expect.any(Object))
  })

  it('leaveList propaga ApiError quando o dono tenta sair', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(409))

    await expect(listsApi.leaveList('list-1')).rejects.toMatchObject({ status: 409 })
  })

  it('getActivities retorna atividades paginadas usando os defaults de pagina/tamanho', async () => {
    const activities = { content: [], totalPages: 0, totalElements: 0, size: 50, number: 0, first: true, last: true }
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: activities } as AxiosResponse)

    await expect(listsApi.getActivities('list-1')).resolves.toEqual(activities)
    expect(client.get).toHaveBeenCalledWith(
      '/api/lists/list-1/activity',
      expect.objectContaining({ params: { page: 0, size: 50 } })
    )
  })

  it('getActivities aceita pagina e tamanho customizados', async () => {
    const activities = { content: [], totalPages: 1, totalElements: 5, size: 5, number: 1, first: false, last: true }
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: activities } as AxiosResponse)

    await listsApi.getActivities('list-1', 1, 5)

    expect(client.get).toHaveBeenCalledWith(
      '/api/lists/list-1/activity',
      expect.objectContaining({ params: { page: 1, size: 5 } })
    )
  })

  it('getActivities propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(404))

    await expect(listsApi.getActivities('list-1')).rejects.toMatchObject({ status: 404 })
  })
})
