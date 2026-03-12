import { expect, test } from '@playwright/test'
import { seedMockAuthSession } from './support/mock-auth'

const ownerSessionUser = {
  id: 'user-demo',
  username: 'leo',
  email: 'leo@test.com',
  displayName: 'Leo Oliveira',
  avatarUrl: null,
  onboardingCompletedAt: '2026-03-10T10:00:00.000Z',
}

async function dismissOnboardingIfVisible(page: Parameters<typeof test>[0]['page']) {
  const skipOnboardingButton = page.getByRole('button', { name: 'Pular' })
  if (await skipOnboardingButton.isVisible()) {
    await skipOnboardingButton.click()
    await expect(skipOnboardingButton).toBeHidden()
  }
}

async function createListFromHome(page: Parameters<typeof test>[0]['page'], listName: string) {
  await page.goto('/home')
  await dismissOnboardingIfVisible(page)
  await page.getByRole('button', { name: 'Nova Lista' }).click()
  await page.getByLabel('Nome da lista').fill(listName)
  await page
    .getByRole('button', { name: /Compras/i })
    .first()
    .click()
  await page.getByRole('button', { name: 'Criar Lista' }).click({ force: true })
  await dismissOnboardingIfVisible(page)

  if (/\/lists\//.test(page.url())) {
    await page.goto('/home')
    await dismissOnboardingIfVisible(page)
  }
}

async function openDeleteListAction(page: Parameters<typeof test>[0]['page']) {
  const moreActions = page
    .getByRole('button', { name: /Mais( ações da lista| acoes da lista)?/i })
    .first()
  if ((await moreActions.count()) > 0) {
    await moreActions.scrollIntoViewIfNeeded()
    await moreActions.click({ force: true })

    const deleteFromMenu = page
      .getByRole('menuitem', { name: /^Excluir lista$/i })
      .or(page.getByRole('button', { name: /^Excluir lista$/i }))
      .first()
    if (await deleteFromMenu.isVisible()) {
      await deleteFromMenu.click()
      return
    }
  }

  const directDeleteAction = page
    .getByRole('button', { name: /^Excluir lista$|^Excluir$/i })
    .first()
  if (!(await directDeleteAction.isVisible())) {
    await expect(directDeleteAction).toBeVisible({ timeout: 10000 })
  }
  await directDeleteAction.click()
}

test('@pr home permite criar lista manualmente fora do onboarding', async ({ page }) => {
  const listName = `Lista PR ${Date.now()}`
  await seedMockAuthSession(page, ownerSessionUser)

  await createListFromHome(page, listName)

  await expect(page.getByRole('heading', { level: 1, name: 'Minhas Listas' })).toBeVisible()
  await expect(page.getByRole('link', { name: `Abrir lista ${listName}` })).toBeVisible()
})

test('@pr lista cobre ciclo de item: adicionar, marcar, editar e excluir', async ({ page }) => {
  const itemName = `Item PR ${Date.now()}`
  const editedName = `${itemName} editado`
  await seedMockAuthSession(page, ownerSessionUser)

  await page.goto('/lists/list-demo-1')
  await page.getByPlaceholder('Adicionar novo item…').fill(itemName)
  await page.getByRole('button', { name: 'Adicionar' }).click()
  await expect(page.getByRole('button', { name: itemName })).toBeVisible()

  const createdRow = page.locator('[data-testid^="list-item-"]', { hasText: itemName }).first()
  await createdRow.locator('[role="checkbox"]').click()
  await expect(page.getByText('Sincronizado', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: itemName }).click()
  await page.getByTestId('edit-item-name').fill(editedName)
  await page.getByRole('button', { name: 'Salvar' }).click()
  await expect(page.getByRole('button', { name: editedName })).toBeVisible()

  const editedRow = page.locator('[data-testid^="list-item-"]', { hasText: editedName }).first()
  await editedRow.click({ delay: 1200 })
  await page.getByTestId('item-option-delete').click()
  await page.getByTestId('delete-confirm-button').click()

  await expect(page.locator('[data-testid^="list-item-"]', { hasText: editedName })).toHaveCount(0)
})

test('@pr lista permite gerar e copiar link de convite', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: {
        writeText: async () => undefined,
      },
    })
  })
  await seedMockAuthSession(page, ownerSessionUser)
  await page.goto('/lists/list-demo-1')

  await page.getByRole('button', { name: 'Convidar' }).click()
  await page.getByRole('button', { name: 'Gerar Link' }).click()

  await expect(page.getByText('Link ativo')).toBeVisible()
  await expect(page.getByText('/join/')).toBeVisible()

  await page.getByRole('button', { name: 'Copiar Link' }).click()
  await expect(page.getByRole('button', { name: 'Copiado!' })).toBeVisible()
})

test('@pr convite por username atualiza resumo de convidados na lista', async ({ page }) => {
  await seedMockAuthSession(page, ownerSessionUser)
  await page.goto('/lists/list-demo-1')

  await page.getByRole('button', { name: 'Convidar' }).click()
  await page.getByPlaceholder('Buscar usuario...').fill('leo')
  await page.getByRole('button', { name: /leo/i }).first().click()
  await page.getByRole('button', { name: 'Convidar por username' }).click()

  await expect(page.getByText('Convidados nesta sessao: @leo')).toBeVisible()
})

test('@pr dono consegue excluir lista e retornar para home', async ({ page }) => {
  const listName = `Lista apagar ${Date.now()}`
  await seedMockAuthSession(page, ownerSessionUser)

  await createListFromHome(page, listName)
  await expect(page.getByRole('link', { name: `Abrir lista ${listName}` })).toBeVisible()
  await page.getByRole('link', { name: `Abrir lista ${listName}` }).click()
  await dismissOnboardingIfVisible(page)

  const actionsVisible =
    (await page
      .getByRole('button', { name: /Mais( ações da lista| acoes da lista)?/i })
      .first()
      .isVisible()
      .catch(() => false)) ||
    (await page
      .getByRole('button', { name: /^Excluir lista$|^Excluir$/i })
      .first()
      .isVisible()
      .catch(() => false))

  if (!actionsVisible) {
    await page.goto('/lists/list-demo-1')
    await dismissOnboardingIfVisible(page)
  }

  await openDeleteListAction(page)
  await page
    .getByRole('button', { name: /^Excluir$/ })
    .last()
    .click()

  await expect(page).toHaveURL(/\/home$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Minhas Listas' })).toBeVisible()
})
