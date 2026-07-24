import { Page } from '@playwright/test'

export interface MockAuthUser {
  id: string
  username: string
  email: string
  displayName: string
  avatarUrl: string | null
  onboardingCompletedAt: string | null
}

const defaultUser: MockAuthUser = {
  id: 'user-demo',
  username: 'leo',
  email: 'leo@test.com',
  displayName: 'Leo Oliveira',
  avatarUrl: null,
  onboardingCompletedAt: null,
}

export async function seedMockAuthSession(page: Page, user: MockAuthUser = defaultUser) {
  await page.context().addCookies([
    {
      name: 'nl_mock_session',
      value: user.id,
      url: 'http://127.0.0.1:4173',
      httpOnly: true,
      sameSite: 'Lax',
    },
  ])
  await page.addInitScript(() => {
    sessionStorage.removeItem('pendingInviteCode')
  })
}
