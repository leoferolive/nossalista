import { expect, test } from '@playwright/test'

test('loads application shell', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
})
