import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import { VerifyEmail } from './VerifyEmail'
import { authApi } from '../api/authApi'
import { ThemeProvider } from '../contexts/ThemeContext'

vi.mock('../api/authApi', () => ({
  authApi: {
    verifyEmail: vi.fn(),
  },
}))

describe('VerifyEmail page (Q2.7)', () => {
  beforeEach(() => {
    vi.mocked(authApi.verifyEmail).mockReset()
  })

  const renderPage = (initialEntry: string) =>
    render(
      <ThemeProvider>
        <MemoryRouter initialEntries={[initialEntry]}>
          <Routes>
            <Route path="/verify-email" element={<VerifyEmail />} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    )

  it('confirma o token e mostra sucesso', async () => {
    vi.mocked(authApi.verifyEmail).mockResolvedValueOnce(undefined)

    renderPage('/verify-email?token=valid-token')

    await waitFor(() => expect(screen.getByText('E-mail Confirmado')).toBeInTheDocument())
    expect(authApi.verifyEmail).toHaveBeenCalledWith('valid-token')
  })

  it('mostra erro quando o token é inválido', async () => {
    vi.mocked(authApi.verifyEmail).mockRejectedValueOnce(new Error('Token expirado'))

    renderPage('/verify-email?token=bad-token')

    await waitFor(() => expect(screen.getByText('Verificação Falhou')).toBeInTheDocument())
    expect(screen.getByText('Token expirado')).toBeInTheDocument()
  })

  it('mostra erro quando o token está ausente na URL', async () => {
    renderPage('/verify-email')

    await waitFor(() => expect(screen.getByText('Verificação Falhou')).toBeInTheDocument())
    expect(authApi.verifyEmail).not.toHaveBeenCalled()
  })
})
