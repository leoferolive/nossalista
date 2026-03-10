import { expect, test } from '@playwright/test'

interface SessionOptions {
  onboardingCompletedAt: string | null
  initialLists?: Array<{ id: string; name: string }>
}

async function mockSession(page: Parameters<typeof test>[0]['page'], options: SessionOptions) {
  let onboardingCompletedAt = options.onboardingCompletedAt
  let completeOnboardingCalls = 0
  let createdListCounter = 0

  const nowIso = () => new Date().toISOString()

  const baseList = (id: string, name: string) => ({
    id,
    name,
    type: { id: 1, name: 'Compras', slug: 'compras' },
    owner: { id: 'user-1', username: 'leo', name: 'Leo', avatarUrl: null },
    inviteCode: 'INVITE-123',
    isOwner: true,
    itemsCount: 0,
    createdAt: nowIso(),
    updatedAt: nowIso(),
  })

  const lists = (options.initialLists ?? []).map((list) => baseList(list.id, list.name))

  await page.addInitScript(
    ({ onboarding }) => {
      localStorage.setItem('authToken', 'token-e2e')
      localStorage.setItem(
        'user',
        JSON.stringify({
          id: 'user-1',
          username: 'leo',
          email: 'leo@test.com',
          displayName: 'Leo',
          avatarUrl: null,
          onboardingCompletedAt: onboarding,
        })
      )
    },
    { onboarding: onboardingCompletedAt }
  )

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
        id: 'user-1',
        username: 'leo',
        email: 'leo@test.com',
        name: 'Leo',
        avatarUrl: null,
        authProvider: 'EMAIL',
        onboardingCompletedAt,
        createdAt: nowIso(),
      })
    }

    if (path === '/api/users/me/onboarding/complete' && method === 'POST') {
      completeOnboardingCalls += 1
      onboardingCompletedAt = nowIso()
      return route.fulfill({ status: 204, body: '' })
    }

    if (path === '/api/lists' && method === 'GET') {
      return json(200, lists)
    }

    if (path === '/api/lists' && method === 'POST') {
      createdListCounter += 1
      const payload = request.postDataJSON() as { name: string }
      const created = baseList(`list-e2e-${createdListCounter}`, payload.name)
      lists.unshift(created)
      return json(201, created)
    }

    const listIdMatch = path.match(/^\/api\/lists\/([^/]+)$/)
    if (listIdMatch && method === 'GET') {
      const found = lists.find((list) => list.id === listIdMatch[1])
      return found ? json(200, found) : json(404, { detail: 'Lista não encontrada' })
    }

    if (path.match(/^\/api\/lists\/[^/]+\/items$/) && method === 'GET') {
      return json(200, [])
    }

    if (path.match(/^\/api\/lists\/[^/]+\/state$/) && method === 'GET') {
      const id = path.split('/')[3] ?? 'unknown'
      return json(200, {
        listId: id,
        revision: 0,
        updatedAt: nowIso(),
        itemsCount: 0,
      })
    }

    if (path.match(/^\/api\/lists\/[^/]+\/activity$/) && method === 'GET') {
      return json(200, {
        content: [],
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 1,
        last: true,
      })
    }

    if (path.match(/^\/api\/lists\/[^/]+\/members$/) && method === 'GET') {
      return json(200, [
        {
          user: { id: 'user-1', username: 'leo', name: 'Leo', avatar_url: null },
          role: 'OWNER',
          joined_at: nowIso(),
        },
      ])
    }

    if (path.match(/^\/api\/users\/search$/) && method === 'GET') {
      return json(200, [])
    }

    if (path.match(/^\/api\/lists\/[^/]+\/invite-link$/) && method === 'POST') {
      return json(200, {
        invite_code: 'INVITE-123',
        invite_link: 'http://localhost:5173/join/INVITE-123',
        expires_at: nowIso(),
      })
    }

    return json(200, {})
  })

  return {
    getCompleteOnboardingCalls: () => completeOnboardingCalls,
  }
}

test('first login tutorial completa o fluxo e persiste conclusão', async ({ page }) => {
  const mocked = await mockSession(page, { onboardingCompletedAt: null })

  await page.goto('/home')

  await expect(page.getByText('Comece pela primeira lista')).toBeVisible()
  await page.getByRole('button', { name: 'Próximo' }).click()

  await expect(page.getByText('Defina nome e tipo')).toBeVisible()
  await page.getByLabel('Nome da lista').fill('Lista E2E Tutorial')
  await page
    .getByRole('button', { name: /Compras/i })
    .first()
    .click()
  await page.getByRole('button', { name: 'Criar Lista' }).click({ force: true })

  await expect(page).toHaveURL(/\/lists\/list-e2e-1/)
  await expect(page.getByText('Lista criada e pronta para uso')).toBeVisible()

  await page.getByRole('button', { name: 'Próximo' }).click()
  await expect(page.getByText('Compartilhe com os amigos')).toBeVisible()

  await page.getByRole('button', { name: 'Próximo' }).click()
  await expect(page.getByText('Adicione e edite itens')).toBeVisible()

  await page.getByRole('button', { name: 'Próximo' }).click()
  await expect(page.getByText('Acompanhe tudo em tempo real')).toBeVisible()

  await page.getByRole('button', { name: 'Concluir' }).click()

  await expect(page.getByText('Comece pela primeira lista')).not.toBeVisible()
  expect(mocked.getCompleteOnboardingCalls()).toBe(1)
})

test('skip no tutorial persiste conclusão sem repetir na sessão', async ({ page }) => {
  const mocked = await mockSession(page, { onboardingCompletedAt: null })

  await page.goto('/home')
  await expect(page.getByText('Comece pela primeira lista')).toBeVisible()

  await page.getByRole('button', { name: 'Pular' }).click()

  await expect(page.getByText('Comece pela primeira lista')).not.toBeVisible()
  expect(mocked.getCompleteOnboardingCalls()).toBe(1)
})

test('usuario com onboarding concluído pode reabrir pelo menu', async ({ page }) => {
  await mockSession(page, { onboardingCompletedAt: '2026-03-10T10:00:00.000Z' })

  await page.goto('/home')
  await expect(page.getByText('Comece pela primeira lista')).not.toBeVisible()

  await page.getByRole('button', { name: 'Abrir menu da conta' }).click()
  await page.getByRole('menuitem', { name: 'Ver tutorial' }).click()

  await expect(page.getByText('Comece pela primeira lista')).toBeVisible()
})
