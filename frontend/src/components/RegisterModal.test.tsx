import type { ComponentProps } from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { RegisterModal } from './RegisterModal'
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
    register: vi.fn(),
  },
}))

function renderModal(props?: Partial<ComponentProps<typeof RegisterModal>>) {
  const onClose = props?.onClose ?? vi.fn()
  const onSwitchToLogin = props?.onSwitchToLogin

  render(
    <MemoryRouter>
      <RegisterModal onClose={onClose} onSwitchToLogin={onSwitchToLogin} />
    </MemoryRouter>
  )

  return { onClose }
}

describe('RegisterModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza campos principais e deixa submit desabilitado ate preencher o minimo', () => {
    renderModal()

    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Senha')).toBeInTheDocument()
    expect(screen.getByLabelText('Confirmar senha')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Criar conta e continuar' })).toBeDisabled()
  })

  it('normaliza username para minusculas e envia cadastro com redirect para login', async () => {
    const user = userEvent.setup()
    const { onClose } = renderModal()

    vi.mocked(authApi.register).mockResolvedValue({
      id: 'user-1',
      username: 'leo_user',
      email: 'leo@test.com',
      name: 'Leo',
      avatarUrl: null,
      authProvider: 'EMAIL',
      createdAt: '2026-03-04T12:00:00',
    })

    await user.type(screen.getByLabelText('Nome'), 'Leo Oliveira')
    await user.type(screen.getByLabelText('Username'), 'Leo_User')
    await user.type(screen.getByLabelText('Email'), 'Leo@Test.com')
    await user.type(screen.getByLabelText('Senha'), '123456')
    await user.type(screen.getByLabelText('Confirmar senha'), '123456')
    await user.click(screen.getByRole('button', { name: 'Criar conta e continuar' }))

    await waitFor(() => {
      expect(authApi.register).toHaveBeenCalledWith({
        name: 'Leo Oliveira',
        username: 'leo_user',
        email: 'leo@test.com',
        password: '123456',
      })
    })

    expect(mockNavigate).toHaveBeenCalledWith('/?auth=login&registered=1&email=leo%40test.com', {
      replace: true,
    })
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('mostra erro quando username e invalido', async () => {
    const user = userEvent.setup()
    renderModal()

    await user.type(screen.getByLabelText('Username'), 'Le!')
    await user.type(screen.getByLabelText('Email'), 'leo@test.com')
    await user.type(screen.getByLabelText('Senha'), '123456')
    await user.type(screen.getByLabelText('Confirmar senha'), '123456')
    await user.click(screen.getByRole('button', { name: 'Criar conta e continuar' }))

    expect(
      await screen.findByText(
        'Use um username com 3 a 50 caracteres, minusculas, numeros, hifen ou underscore.'
      )
    ).toBeInTheDocument()
    expect(authApi.register).not.toHaveBeenCalled()
  })

  it('mostra erro quando as senhas nao conferem', async () => {
    const user = userEvent.setup()
    renderModal()

    await user.type(screen.getByLabelText('Username'), 'leo_user')
    await user.type(screen.getByLabelText('Email'), 'leo@test.com')
    await user.type(screen.getByLabelText('Senha'), '123456')
    await user.type(screen.getByLabelText('Confirmar senha'), 'abcdef')
    await user.click(screen.getByRole('button', { name: 'Criar conta e continuar' }))

    expect(await screen.findByText('As senhas nao conferem.')).toBeInTheDocument()
    expect(authApi.register).not.toHaveBeenCalled()
  })

  it('mostra erro retornado pela API quando o cadastro falha', async () => {
    const user = userEvent.setup()
    renderModal()

    vi.mocked(authApi.register).mockRejectedValueOnce(new Error('Email ja cadastrado'))

    await user.type(screen.getByLabelText('Username'), 'leo_user')
    await user.type(screen.getByLabelText('Email'), 'leo@test.com')
    await user.type(screen.getByLabelText('Senha'), '123456')
    await user.type(screen.getByLabelText('Confirmar senha'), '123456')
    await user.click(screen.getByRole('button', { name: 'Criar conta e continuar' }))

    expect(await screen.findByText('Email ja cadastrado')).toBeInTheDocument()
  })

  it('usa callback de troca para login quando onSwitchToLogin esta presente', async () => {
    const user = userEvent.setup()
    const onSwitchToLogin = vi.fn()
    renderModal({ onSwitchToLogin })

    await user.click(screen.getByRole('button', { name: 'Entrar agora' }))

    expect(onSwitchToLogin).toHaveBeenCalledTimes(1)
  })

  it('fecha o modal ao clicar no botao fechar e ao pressionar Escape', () => {
    const onClose = vi.fn()
    renderModal({ onClose })

    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }))
    fireEvent.keyDown(document, { key: 'Escape' })

    expect(onClose).toHaveBeenCalledTimes(1)
  })
})
