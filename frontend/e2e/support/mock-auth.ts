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
  await page.addInitScript(
    ({ seededUser }) => {
      localStorage.setItem('authToken', `mock-token-${seededUser.id}`)
      localStorage.setItem('user', JSON.stringify(seededUser))
      sessionStorage.removeItem('pendingInviteCode')
    },
    { seededUser: user }
  )
}
