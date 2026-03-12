import { expect, test } from '@playwright/test'

function nowIso() {
  return new Date().toISOString()
}

test('shell autenticado mobile usa sheets e reduz ruido no topo', async ({ browser }) => {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 } })
  const page = await context.newPage()

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

    if (path === '/api/auth/login' && method === 'POST') {
      return json(200, {
        id: 'user-demo',
        username: 'leo',
        email: 'leo@test.com',
        name: 'Leo Oliveira',
        avatarUrl: null,
        onboardingCompletedAt: null,
        token: 'token-mobile-shell',
      })
    }

    if (path === '/api/users/me/onboarding/complete' && method === 'POST') {
      return json(200, { completedAt: nowIso() })
    }

    if (path === '/api/push/vapid-public-key' && method === 'GET') {
      return json(404, { message: 'Not configured' })
    }

    if (path === '/api/lists' && method === 'GET') {
      return json(200, [
        {
          id: 'list-demo-1',
          name: 'Mercado da Semana',
          type: { id: 1, name: 'Compras', slug: 'compras' },
          owner: { id: 'user-demo', username: 'leo', name: 'Leo Oliveira', avatarUrl: null },
          inviteCode: 'MOCKSHOP',
          isOwner: true,
          itemsCount: 2,
          createdAt: nowIso(),
          updatedAt: nowIso(),
        },
      ])
    }

    if (path === '/api/lists/list-demo-1' && method === 'GET') {
      return json(200, {
        id: 'list-demo-1',
        name: 'Mercado da Semana',
        type: { id: 1, name: 'Compras', slug: 'compras' },
        owner: { id: 'user-demo', username: 'leo', name: 'Leo Oliveira', avatarUrl: null },
        inviteCode: 'MOCKSHOP',
        isOwner: true,
        itemsCount: 2,
        createdAt: nowIso(),
        updatedAt: nowIso(),
      })
    }

    if (path === '/api/lists/list-demo-1/items' && method === 'GET') {
      return json(200, [
        {
          id: 'item-1',
          listId: 'list-demo-1',
          name: 'Tomate sweet grape',
          checked: true,
          quantity: 2,
          position: 0,
          createdBy: 'user-demo',
          createdAt: nowIso(),
          updatedAt: nowIso(),
        },
        {
          id: 'item-2',
          listId: 'list-demo-1',
          name: 'Queijo minas',
          checked: false,
          quantity: 1,
          position: 1,
          createdBy: 'user-demo',
          createdAt: nowIso(),
          updatedAt: nowIso(),
        },
      ])
    }

    if (path === '/api/lists/list-demo-1/state' && method === 'GET') {
      return json(200, {
        listId: 'list-demo-1',
        revision: 2,
        updatedAt: nowIso(),
        itemsCount: 2,
      })
    }

    if (path === '/api/lists/list-demo-1/members' && method === 'GET') {
      return json(200, [
        {
          user: { id: 'user-demo', username: 'leo', name: 'Leo Oliveira', avatar_url: null },
          role: 'OWNER',
          joined_at: nowIso(),
        },
      ])
    }

    if (path === '/api/users/search' && method === 'GET') {
      return json(200, [])
    }

    if (path === '/api/lists/list-demo-1/invite-link' && method === 'POST') {
      return json(200, {
        invite_code: 'MOCKSHOP',
        invite_link: 'http://127.0.0.1:4173/join/MOCKSHOP',
        expires_at: nowIso(),
      })
    }

    if (path === '/api/lists/list-demo-1/activity' && method === 'GET') {
      return json(200, {
        content: [],
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 1,
        last: true,
      })
    }

    return json(200, {})
  })

  await page.goto('/?auth=login')
  await page.getByLabel('Email').fill('teste@gmail.com')
  await page.getByLabel('Senha').fill('123')
  await page.getByRole('button', { name: 'Entrar' }).click()

  await expect(page).toHaveURL(/\/home$/)
  const skipButton = page.getByRole('button', { name: 'Pular' })
  if (await skipButton.isVisible().catch(() => false)) {
    await skipButton.click()
  }

  await expect(page.getByRole('button', { name: 'Claro' })).toHaveCount(0)
  await expect(page.getByLabel('Abrir menu da conta')).toBeVisible()

  await page.getByLabel('Abrir menu da conta').click()
  const accountSheet = page.getByRole('dialog', { name: 'Conta' })
  await expect(accountSheet).toBeVisible()
  await expect(accountSheet.getByRole('button', { name: 'Claro' })).toBeVisible()

  const accountBounds = await accountSheet.boundingBox()
  expect(accountBounds?.x ?? -1).toBeGreaterThanOrEqual(0)

  await page.keyboard.press('Escape')
  await page.getByLabel('Notificações').click()
  const notificationsSheet = page.getByRole('dialog', { name: 'Notificações' })
  await expect(notificationsSheet).toBeVisible()
  const notificationBounds = await notificationsSheet.boundingBox()
  expect(notificationBounds?.x ?? -1).toBeGreaterThanOrEqual(0)
  await page.keyboard.press('Escape')

  await page.getByRole('link', { name: /abrir lista mercado da semana/i }).click()
  await expect(page).toHaveURL(/\/lists\/list-demo-1$/)
  await expect(page.getByLabel('Mais ações da lista')).toBeVisible()
  await page.getByLabel('Mais ações da lista').click()
  await expect(page.getByRole('dialog', { name: 'Mais ações da lista' })).toBeVisible()

  await context.close()
})
