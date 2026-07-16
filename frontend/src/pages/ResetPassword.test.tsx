import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ResetPassword } from './ResetPassword'
import { ThemeProvider } from '../contexts/ThemeContext'
import { authApi } from '../api/authApi'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('../api/authApi', () => ({
  authApi: {
    resetPassword: vi.fn(),
  },
}))

function renderResetPassword(initialEntry: string) {
  return render(
    <ThemeProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/reset-password" element={<ResetPassword />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>
  )
}

describe('ResetPassword page', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    vi.mocked(authApi.resetPassword).mockReset()
  })

  it('mostra tela de link invalido quando nao ha token na URL', () => {
    renderResetPassword('/reset-password')

    expect(screen.getByText('Link Invalido')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Solicitar Novo Link' })).toHaveAttribute(
      'href',
      '/forgot-password'
    )
  })

  it('valida que a nova senha tenha pelo menos 6 caracteres', async () => {
    renderResetPassword('/reset-password?token=abc123')

    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: '123' } })
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), { target: { value: '123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    expect(
      await screen.findByText('A senha precisa ter pelo menos 6 caracteres.')
    ).toBeInTheDocument()
    expect(authApi.resetPassword).not.toHaveBeenCalled()
  })

  it('valida que as senhas conferem', async () => {
    renderResetPassword('/reset-password?token=abc123')

    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'segredo1' } })
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), {
      target: { value: 'segredo2' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    expect(await screen.findByText('As senhas nao conferem.')).toBeInTheDocument()
    expect(authApi.resetPassword).not.toHaveBeenCalled()
  })

  it('redefine a senha com sucesso e navega para o login', async () => {
    vi.mocked(authApi.resetPassword).mockResolvedValueOnce(undefined)

    renderResetPassword('/reset-password?token=abc123')

    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'segredo1' } })
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), {
      target: { value: 'segredo1' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    await waitFor(() =>
      expect(authApi.resetPassword).toHaveBeenCalledWith('abc123', 'segredo1')
    )
    await waitFor(() =>
      expect(mockNavigate).toHaveBeenCalledWith('/?auth=login&reset=1', { replace: true })
    )
  })

  it('mostra erro do servidor quando a redefinicao falha', async () => {
    vi.mocked(authApi.resetPassword).mockRejectedValueOnce(new Error('Token invalido ou expirado.'))

    renderResetPassword('/reset-password?token=abc123')

    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'segredo1' } })
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), {
      target: { value: 'segredo1' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }))

    expect(await screen.findByText('Token invalido ou expirado.')).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()
  })
})
