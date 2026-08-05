import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ForgotPassword } from './ForgotPassword'
import { ThemeProvider } from '../contexts/ThemeContext'
import { authApi } from '../api/authApi'

vi.mock('../api/authApi', () => ({
  authApi: { forgotPassword: vi.fn() },
}))

describe('ForgotPassword page', () => {
  beforeEach(() => {
    vi.mocked(authApi.forgotPassword).mockReset()
  })
  it('mostra formulario de email para redefinicao de senha', () => {
    render(
      <ThemeProvider>
        <MemoryRouter initialEntries={['/forgot-password']}>
          <Routes>
            <Route path="/forgot-password" element={<ForgotPassword />} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    )

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /enviar link de redefinicao/i })).toBeInTheDocument()
    expect(screen.getAllByText(/voltar para login/i).length).toBeGreaterThanOrEqual(1)
  })

  it('normaliza o e-mail e confirma o envio da solicitação', async () => {
    vi.mocked(authApi.forgotPassword).mockResolvedValueOnce(undefined)
    render(
      <ThemeProvider>
        <MemoryRouter initialEntries={['/forgot-password']}>
          <Routes>
            <Route path="/forgot-password" element={<ForgotPassword />} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    )

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: ' LEO@TEST.COM ' } })
    fireEvent.click(screen.getByRole('button', { name: /enviar link de redefinicao/i }))

    await waitFor(() => expect(authApi.forgotPassword).toHaveBeenCalledWith('leo@test.com'))
    expect(
      await screen.findByText(/se o email estiver cadastrado, voce recebera um link/i)
    ).toBeInTheDocument()
  })
})
