import { expect, Page } from '@playwright/test'

export interface FullstackUser {
  name: string
  username: string
  email: string
  password: string
}

export function buildFullstackUser(prefix: string): FullstackUser {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 10000)}`
  const safePrefix = prefix.toLowerCase().replace(/[^a-z0-9]/g, '')
  return {
    name: `E2E ${safePrefix}`,
    username: `${safePrefix}${suffix}`.slice(0, 30),
    email: `${safePrefix}${suffix}@test.com`,
    password: '123456',
  }
}

export async function registerFromLanding(page: Page, user: FullstackUser) {
  await page.goto('/?auth=register')
  await page.getByLabel('Nome').fill(user.name)
  await page.getByLabel('Username').fill(user.username)
  await page.getByLabel('Email').fill(user.email)
  await page.getByLabel('Senha').fill(user.password)
  await page.getByLabel('Confirmar senha').fill(user.password)
  await page.getByRole('button', { name: 'Criar conta e continuar' }).click()
  await expect(page).toHaveURL(
    new RegExp(`/\\?auth=login&registered=1&email=${encodeURIComponent(user.email)}`)
  )
}

export async function loginFromLanding(page: Page, user: FullstackUser) {
  await page.getByLabel('Email').fill(user.email)
  await page.getByLabel('Senha').fill(user.password)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page).toHaveURL(/\/home$/)
}

export async function createListFromHome(page: Page, listName: string) {
  await page.getByRole('button', { name: 'Nova Lista' }).click()
  await page.getByLabel('Nome da lista').fill(listName)
  await page
    .getByRole('button', { name: /Compras/i })
    .first()
    .click()
  await page.getByRole('button', { name: 'Criar Lista' }).click({ force: true })
}

export async function skipOnboardingIfVisible(page: Page) {
  const skipButton = page.getByRole('button', { name: 'Pular' })
  if (await skipButton.isVisible()) {
    await skipButton.click()
  }
}
