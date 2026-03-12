import { expect, test } from '@playwright/test'
import {
  buildFullstackUser,
  createListFromHome,
  loginFromLanding,
  registerFromLanding,
  skipOnboardingIfVisible,
} from './support/fullstack'

test('@fullstack preview público de convite carrega dados reais da lista', async ({
  page,
  browser,
}) => {
  const user = buildFullstackUser('invitepreview')
  const listName = `Lista Convite ${Date.now()}`

  await registerFromLanding(page, user)
  await loginFromLanding(page, user)
  await skipOnboardingIfVisible(page)
  await createListFromHome(page, listName)

  await page.getByRole('link', { name: `Abrir lista ${listName}` }).click()
  await page.getByRole('button', { name: 'Convidar' }).click()
  await page.getByRole('button', { name: 'Gerar Link' }).click()

  const inviteLink = (await page.locator('p.break-all').textContent())?.trim()
  expect(inviteLink).toBeTruthy()

  const publicContext = await browser.newContext()
  const publicPage = await publicContext.newPage()

  await publicPage.goto(inviteLink!)
  await expect(publicPage.getByRole('heading', { level: 1, name: listName })).toBeVisible()
  await expect(publicPage.getByText('Modo Leitura')).toBeVisible()

  await publicContext.close()
})

test('@fullstack rotas protegidas redirecionam para landing quando deslogado', async ({ page }) => {
  await page.goto('/home')
  await expect(page).toHaveURL('http://127.0.0.1:4173/')

  await page.goto('/lists/00000000-0000-0000-0000-000000000000')
  await expect(page).toHaveURL('http://127.0.0.1:4173/')
})
