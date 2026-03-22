import { describe, it, expect } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { ForgotPassword } from './ForgotPassword'
import { ThemeProvider } from '../contexts/ThemeContext'

describe('ForgotPassword page', () => {
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
    expect(
      screen.getByRole('button', { name: /enviar link de redefinicao/i })
    ).toBeInTheDocument()
    expect(screen.getAllByText(/voltar para login/i).length).toBeGreaterThanOrEqual(1)
  })
})
