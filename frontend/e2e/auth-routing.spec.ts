import { expect, test } from '@playwright/test'

test('@pr rotas protegidas deslogado redirecionam para landing', async ({ page }) => {
  await page.goto('/home')
  await expect(page).toHaveURL('http://127.0.0.1:4173/')

  await page.goto('/lists/list-demo-1')
  await expect(page).toHaveURL('http://127.0.0.1:4173/')
})

test('@pr rota /login legado redireciona para landing e preserva convite', async ({ page }) => {
  await page.goto('/login?redirect=%2Fjoin%2FMOCKSHOP')
  await expect(page).toHaveURL('http://127.0.0.1:4173/?auth=login')

  const pendingInviteCode = await page.evaluate(() => sessionStorage.getItem('pendingInviteCode'))
  expect(pendingInviteCode).toBe('MOCKSHOP')
})

test('@pr join deslogado abre login na landing e guarda inviteCode', async ({ page }) => {
  const nowIso = () => new Date().toISOString()

  await page.route('**/api/lists/join/MOCKSHOP', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'list-demo-1',
        name: 'Mercado da Semana',
        type_slug: 'compras',
        type_name: 'Compras',
        owner_username: 'leo',
        owner_name: 'Leo',
        owner_avatar_url: null,
        items: [],
        invite_code: 'MOCKSHOP',
        expires_at: nowIso(),
        mode: 'READ_ONLY',
      }),
    })
  })

  await page.goto('/join/MOCKSHOP')
  await page.getByRole('button', { name: 'Entrar' }).first().click()

  await expect(page).toHaveURL('http://127.0.0.1:4173/?auth=login')
  const pendingInviteCode = await page.evaluate(() => sessionStorage.getItem('pendingInviteCode'))
  expect(pendingInviteCode).toBe('MOCKSHOP')
  await expect(page.getByRole('heading', { level: 2, name: 'Entrar no NossaLista' })).toBeVisible()
})

test('@pr login por email com invite pendente entra automaticamente na lista', async ({ page }) => {
  const nowIso = () => new Date().toISOString()
  let joinRequestCount = 0

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
        id: 'user-1',
        username: 'leo',
        email: 'leo@test.com',
        name: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
        token: 'token-e2e',
      })
    }

    if (path === '/api/lists/join/MOCKSHOP' && method === 'POST') {
      joinRequestCount += 1
      return json(200, {
        id: 'list-joined',
        message: 'Entrou!',
      })
    }

    if (path === '/api/lists' && method === 'GET') {
      return json(200, [
        {
          id: 'list-joined',
          name: 'Mercado da Semana',
          type: { id: 1, name: 'Compras', slug: 'compras' },
          owner: { id: 'user-2', username: 'ana', name: 'Ana', avatarUrl: null },
          inviteCode: 'MOCKSHOP',
          isOwner: false,
          itemsCount: 0,
          createdAt: nowIso(),
          updatedAt: nowIso(),
        },
      ])
    }

    if (path === '/api/lists/list-joined' && method === 'GET') {
      return json(200, {
        id: 'list-joined',
        name: 'Mercado da Semana',
        type: { id: 1, name: 'Compras', slug: 'compras' },
        owner: { id: 'user-2', username: 'ana', name: 'Ana', avatarUrl: null },
        inviteCode: 'MOCKSHOP',
        isOwner: false,
        itemsCount: 0,
        createdAt: nowIso(),
        updatedAt: nowIso(),
      })
    }

    if (path === '/api/lists/list-joined/items' && method === 'GET') {
      return json(200, [])
    }

    if (path === '/api/lists/list-joined/state' && method === 'GET') {
      return json(200, {
        listId: 'list-joined',
        revision: 0,
        updatedAt: nowIso(),
        itemsCount: 0,
      })
    }

    if (path === '/api/lists/list-joined/activity' && method === 'GET') {
      return json(200, {
        content: [],
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 1,
        last: true,
      })
    }

    if (path === '/api/lists/list-joined/members' && method === 'GET') {
      return json(200, [
        {
          user: { id: 'user-1', username: 'leo', name: 'Leo', avatar_url: null },
          role: 'MEMBER',
          joined_at: nowIso(),
        },
      ])
    }

    if (path === '/api/users/search' && method === 'GET') {
      return json(200, [])
    }

    if (path === '/api/lists/list-joined/invite-link' && method === 'POST') {
      return json(200, {
        invite_code: 'MOCKSHOP',
        invite_link: 'http://localhost:4173/join/MOCKSHOP',
        expires_at: nowIso(),
      })
    }

    return json(200, {})
  })

  await page.goto('/?auth=login')
  await page.evaluate(() => {
    sessionStorage.setItem('pendingInviteCode', 'MOCKSHOP')
  })
  await page.getByLabel('Email').fill('leo@test.com')
  await page.getByLabel('Senha').fill('123456')
  await page.getByRole('button', { name: 'Entrar' }).click()

  await expect.poll(() => joinRequestCount).toBeGreaterThan(0)
  await expect(page).toHaveURL(/\/lists\/list-joined$/)
  const pendingInviteCode = await page.evaluate(() => sessionStorage.getItem('pendingInviteCode'))
  expect(pendingInviteCode).toBeNull()
})

test('@pr landing mobile nao deve ter overflow horizontal', async ({ browser }) => {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 } })
  const page = await context.newPage()

  await page.goto('/')
  await page.waitForLoadState('networkidle')

  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - window.innerWidth
  )
  expect(overflow).toBeLessThanOrEqual(0)

  await context.close()
})
