import { expect, test } from '@playwright/test'
import {
  buildFullstackUser,
  createListFromHome,
  loginFromLanding,
  registerFromLanding,
  skipOnboardingIfVisible,
} from './support/fullstack'

test('@fullstack cadastro, login e criação de lista funcionam ponta a ponta', async ({ page }) => {
  const user = buildFullstackUser('authlist')
  const listName = `Lista Fullstack ${Date.now()}`

  await registerFromLanding(page, user)
  await loginFromLanding(page, user)
  await skipOnboardingIfVisible(page)

  await createListFromHome(page, listName)

  await expect(page.getByRole('heading', { level: 1, name: 'Minhas Listas' })).toBeVisible()
  await expect(page.getByRole('link', { name: `Abrir lista ${listName}` })).toBeVisible()
})

test('@fullstack CRUD básico de item persiste após reload', async ({ page }) => {
  const user = buildFullstackUser('cruditem')
  const listName = `Lista Itens ${Date.now()}`
  const itemName = `Arroz ${Date.now()}`
  const editedName = `${itemName} premium`

  await registerFromLanding(page, user)
  await loginFromLanding(page, user)
  await skipOnboardingIfVisible(page)

  await createListFromHome(page, listName)
  await page.getByRole('link', { name: `Abrir lista ${listName}` }).click()

  await page.getByPlaceholder('Adicionar novo item…').fill(itemName)
  await page.getByRole('button', { name: 'Adicionar' }).click()
  await expect(page.getByRole('button', { name: itemName })).toBeVisible()

  const createdRow = page.locator('[data-testid^="list-item-"]', { hasText: itemName }).first()
  await createdRow.locator('[role="checkbox"]').click()
  await page.getByRole('button', { name: itemName }).click()
  await page.getByTestId('edit-item-name').fill(editedName)
  await page.getByRole('button', { name: 'Salvar' }).click()
  await expect(page.getByRole('button', { name: editedName })).toBeVisible()

  await page.reload()
  await expect(page.getByRole('button', { name: editedName })).toBeVisible()
  await expect(
    page.locator('[data-testid^="list-item-"]', { hasText: editedName }).locator('[role="checkbox"]')
  ).toHaveAttribute('aria-checked', 'true')

  const editedRow = page.locator('[data-testid^="list-item-"]', { hasText: editedName }).first()
  await editedRow.click({ delay: 1200 })
  await page.getByTestId('item-option-delete').click()
  await page.getByTestId('delete-confirm-button').click()

  await expect(page.getByText(editedName)).not.toBeVisible()
})
