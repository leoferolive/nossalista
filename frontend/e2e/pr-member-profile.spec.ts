import { expect, test } from '@playwright/test'
import { seedMockAuthSession } from './support/mock-auth'

async function seedMockMemberSession(page: Parameters<typeof test>[0]['page']) {
  let isMember = true
  const nowIso = () => new Date().toISOString()

  await page.addInitScript(() => {
    localStorage.setItem('authToken', 'mock-token-user-member')
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: 'user-member',
        username: 'membro',
        email: 'membro@test.com',
        displayName: 'Usuário Membro',
        avatarUrl: null,
        onboardingCompletedAt: '2026-03-10T10:00:00.000Z',
      })
    )
  })

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const method = request.method()
    const path = url.pathname

    const json = (status: number, body: unknown) =>
      route.fulfill({
        status,
        contentType: 'application/json',
        body: JSON.stringify(body),
      })

    if (path === '/api/users/me' && method === 'GET') {
      return json(200, {
        id: 'user-member',
        username: 'membro',
        email: 'membro@test.com',
        name: 'Usuário Membro',
        avatarUrl: null,
        onboardingCompletedAt: '2026-03-10T10:00:00.000Z',
        authProvider: 'EMAIL',
      })
    }

    if (path === '/api/lists' && method === 'GET') {
      return json(
        200,
        isMember
          ? [
              {
                id: 'list-member-1',
                name: 'Lista Compartilhada',
                type: { id: 1, name: 'Compras', slug: 'compras' },
                owner: { id: 'user-owner', username: 'owner', name: 'Owner', avatarUrl: null },
                inviteCode: 'MOCKMEMBER',
                isOwner: false,
                itemsCount: 0,
                createdAt: nowIso(),
                updatedAt: nowIso(),
              },
            ]
          : []
      )
    }

    if (path === '/api/lists/list-member-1' && method === 'GET') {
      return json(200, {
        id: 'list-member-1',
        name: 'Lista Compartilhada',
        type: { id: 1, name: 'Compras', slug: 'compras' },
        owner: { id: 'user-owner', username: 'owner', name: 'Owner', avatarUrl: null },
        inviteCode: 'MOCKMEMBER',
        isOwner: false,
        itemsCount: 0,
        createdAt: nowIso(),
        updatedAt: nowIso(),
      })
    }

    if (path === '/api/lists/list-member-1/items' && method === 'GET') {
      return json(200, [])
    }

    if (path === '/api/lists/list-member-1/state' && method === 'GET') {
      return json(200, {
        listId: 'list-member-1',
        revision: 0,
        updatedAt: nowIso(),
        itemsCount: 0,
      })
    }

    if (path === '/api/lists/list-member-1/activity' && method === 'GET') {
      return json(200, {
        content: [],
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 1,
        last: true,
      })
    }

    if (path === '/api/lists/list-member-1/members' && method === 'GET') {
      return json(200, [
        {
          user: { id: 'user-owner', username: 'owner', name: 'Owner', avatarUrl: null },
          role: 'OWNER',
          joinedAt: nowIso(),
        },
        {
          user: { id: 'user-member', username: 'membro', name: 'Usuário Membro', avatarUrl: null },
          role: 'MEMBER',
          joinedAt: nowIso(),
        },
      ])
    }

    if (path === '/api/lists/list-member-1/leave' && method === 'POST') {
      isMember = false
      return route.fulfill({ status: 204, body: '' })
    }

    if (path === '/api/users/search' && method === 'GET') {
      return json(200, [])
    }

    if (path === '/api/lists/list-member-1/invite-link' && method === 'POST') {
      return json(200, {
        inviteCode: 'MOCKMEMBER',
        inviteLink: 'http://localhost:4173/join/MOCKMEMBER',
        expiresAt: nowIso(),
      })
    }

    return json(200, {})
  })
}

test('@pr membro pode sair da lista e voltar para home', async ({ page }) => {
  await seedMockMemberSession(page)
  await page.goto('/lists/list-member-1')

  await page.getByRole('button', { name: 'Abrir membros' }).click()
  await page.getByRole('button', { name: 'Sair da Lista' }).click()
  await page.getByRole('button', { name: 'Sair', exact: true }).click()

  await expect(page).toHaveURL(/\/home$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Minhas Listas' })).toBeVisible()
  await expect(
    page.getByRole('heading', { level: 2, name: 'Sua primeira lista começa aqui.' })
  ).toBeVisible()
})

test('@pr perfil permite salvar edição e sair da conta', async ({ page }) => {
  await seedMockAuthSession(page)
  await page.goto('/profile')

  await page.getByRole('button', { name: 'Editar perfil', exact: true }).click()
  await page.getByTestId('profile-name-input').fill('Leo E2E')
  await page.getByRole('button', { name: 'Salvar Alteracoes' }).click()

  await expect(page.getByText('Perfil atualizado com sucesso!')).toBeVisible()
  await expect(page.getByText('Leo E2E')).toBeVisible()

  await page.getByRole('button', { name: 'Abrir menu da conta' }).click()
  await page.getByRole('menuitem', { name: 'Sair' }).click()

  await expect(page).toHaveURL('http://127.0.0.1:4173/?auth=login&redirect=%2Fprofile')
  const token = await page.evaluate(() => localStorage.getItem('authToken'))
  expect(token).toBeNull()
})
