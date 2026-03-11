import { describe, it, expect } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { ForgotPassword } from './ForgotPassword'
import { ThemeProvider } from '../contexts/ThemeContext'

describe('ForgotPassword page', () => {
  it('explica que o fluxo ainda nao esta disponivel', () => {
    render(
      <ThemeProvider>
        <MemoryRouter initialEntries={['/forgot-password?redirect=%2Fjoin%2Fabc123']}>
          <Routes>
            <Route path="/forgot-password" element={<ForgotPassword />} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    )

    expect(screen.getByText(/a redefinicao completa ainda depende do backend/i)).toBeInTheDocument()
    expect(
      screen
        .getAllByRole('link', { name: 'Voltar para login' })
        .every((link) => link.getAttribute('href') === '/?auth=login')
    ).toBe(true)
  })
})
