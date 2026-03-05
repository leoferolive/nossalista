import { describe, it, expect } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { ForgotPassword } from './ForgotPassword'

describe('ForgotPassword page', () => {
  it('explica que o fluxo ainda nao esta disponivel', () => {
    render(
      <MemoryRouter initialEntries={['/forgot-password?redirect=%2Fjoin%2Fabc123']}>
        <Routes>
          <Route path="/forgot-password" element={<ForgotPassword />} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText(/ainda nao foi implementada no backend/i)).toBeInTheDocument()
    expect(
      screen
        .getAllByRole('link', { name: 'Voltar para login' })
        .every((link) => link.getAttribute('href') === '/login?redirect=%2Fjoin%2Fabc123')
    ).toBe(true)
  })
})
