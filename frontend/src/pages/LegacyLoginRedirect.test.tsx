import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { LegacyLoginRedirect } from './LegacyLoginRedirect'

function LandingProbe() {
  const location = useLocation()
  return <p data-testid="landing-search">{location.search}</p>
}

describe('LegacyLoginRedirect', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('redireciona /login legado para a landing com auth=login', async () => {
    render(
      <MemoryRouter initialEntries={['/login?registered=1&email=leo%40test.com']}>
        <Routes>
          <Route path="/login" element={<LegacyLoginRedirect />} />
          <Route path="/" element={<LandingProbe />} />
        </Routes>
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByTestId('landing-search')).toHaveTextContent(
        '?auth=login&registered=1&email=leo%40test.com'
      )
    })
  })

  it('converte redirect de convite legado para pendingInviteCode', async () => {
    render(
      <MemoryRouter initialEntries={['/login?redirect=%2Fjoin%2FINVITE-123']}>
        <Routes>
          <Route path="/login" element={<LegacyLoginRedirect />} />
          <Route path="/" element={<LandingProbe />} />
        </Routes>
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByTestId('landing-search')).toHaveTextContent('?auth=login')
    })

    expect(sessionStorage.getItem('pendingInviteCode')).toBe('INVITE-123')
  })
})
