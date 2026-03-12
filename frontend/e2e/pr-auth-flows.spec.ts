import { expect, test } from '@playwright/test'

test('@pr cadastro via modal redireciona para login com mensagem de sucesso', async ({ page }) => {
  const email = `cadastro-${Date.now()}@test.com`
  const username = `cadastro${Date.now()}`.slice(0, 24)

  await page.route('**/api/auth/register', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        id: `user-${Date.now()}`,
        username,
        email,
        name: 'Cadastro PR',
        avatarUrl: null,
        authProvider: 'EMAIL',
        createdAt: new Date().toISOString(),
      }),
    })
  })

  await page.goto('/?auth=register')
  await page.getByLabel('Nome').fill('Cadastro PR')
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Email').fill(email)
  await page.locator('#register-modal-password').fill('123456')
  await page.locator('#register-modal-confirm-password').fill('123456')
  await page.getByRole('button', { name: 'Criar conta e continuar' }).click()

  await expect(page).toHaveURL(/\/\?auth=login/)
  await expect(page.getByRole('alert')).toHaveCount(0)
  await expect(page.getByRole('heading', { level: 2, name: 'Entrar no NossaLista' })).toBeVisible()
  await expect(page.getByText('Conta criada com sucesso. Agora e so entrar.')).toBeVisible()
})

test('@pr login inválido exibe erro sem navegar', async ({ page }) => {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ detail: 'Credenciais inválidas' }),
    })
  })

  await page.goto('/?auth=login')
  await page.getByLabel('Email').fill('erro@test.com')
  await page.getByLabel('Senha').fill('senha-invalida')
  await page.getByRole('button', { name: 'Entrar' }).click()

  await expect(page).toHaveURL(/\/\?auth=login$/)
  await expect(page.getByText('Email ou senha inválidos')).toBeVisible()
})

test('@pr join por convite inexistente mostra tela 404', async ({ page }) => {
  await page.goto('/join/INVITE-INEXISTENTE')
  await expect(
    page.getByRole('heading', { level: 1, name: 'Convite não encontrado' })
  ).toBeVisible()
})

test('@pr join por convite expirado mostra tela 410', async ({ page }) => {
  await page.route('**/api/lists/join/EXPIRED-CODE', async (route) => {
    await route.fulfill({
      status: 410,
      contentType: 'application/json',
      body: JSON.stringify({ detail: 'Convite expirado' }),
    })
  })

  await page.goto('/join/EXPIRED-CODE')
  await expect(
    page.getByRole('heading', { level: 1, name: 'Link de convite expirado' })
  ).toBeVisible()
})
