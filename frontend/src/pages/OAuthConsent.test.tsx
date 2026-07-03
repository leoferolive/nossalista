import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { OAuthConsent } from './OAuthConsent'
import { oauthConsentApi } from '../api/oauthConsentApi'
import type { PendingAuthorization } from '../api/oauthConsentApi'
import { ApiError } from '../types/ApiError'

vi.mock('../api/oauthConsentApi', () => ({
  oauthConsentApi: {
    get: vi.fn(),
    approve: vi.fn(),
    deny: vi.fn(),
  },
}))

const pendingAuthorization: PendingAuthorization = {
  requestId: 'req-1',
  clientId: 'claude-ai',
  clientName: 'Claude (claude.ai)',
  scope: 'READ_WRITE',
  redirectUriHost: 'claude.ai',
}

function renderConsent(requestId: string | null = 'req-1') {
  const path = requestId ? `/oauth/consent?request_id=${requestId}` : '/oauth/consent'
  return render(
    <MemoryRouter initialEntries={[path]}>
      <OAuthConsent />
    </MemoryRouter>
  )
}

describe('OAuthConsent', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    ;(oauthConsentApi.get as any).mockResolvedValue(pendingAuthorization)
  })

  afterEach(() => {
    cleanup()
  })

  it('carrega e exibe os dados do pedido de autorização', async () => {
    renderConsent()

    await waitFor(() => {
      expect(screen.getByText('Claude (claude.ai)')).toBeInTheDocument()
    })
    expect(screen.getByText('Leitura e escrita')).toBeInTheDocument()
    expect(screen.getByText(/Você será redirecionado para/)).toBeInTheDocument()
  })

  it('exibe erro quando request_id está ausente', async () => {
    renderConsent(null)

    await waitFor(() => {
      expect(screen.getByText('Pedido de autorização inválido.')).toBeInTheDocument()
    })
    expect(oauthConsentApi.get).not.toHaveBeenCalled()
  })

  it('exibe mensagem específica quando o pedido não existe mais (404)', async () => {
    ;(oauthConsentApi.get as any).mockRejectedValue(new ApiError('not found', 404))
    renderConsent()

    await waitFor(() => {
      expect(screen.getByText(/já foi respondido ou expirou/)).toBeInTheDocument()
    })
  })

  it('aprova o consentimento ao clicar em Autorizar', async () => {
    ;(oauthConsentApi.approve as any).mockResolvedValue({
      redirectUrl: 'https://claude.ai/api/mcp/auth_callback?code=abc&state=xyz',
    })
    renderConsent()

    await waitFor(() => expect(screen.getByText('Autorizar')).toBeInTheDocument())
    fireEvent.click(screen.getByText('Autorizar'))

    await waitFor(() => {
      expect(oauthConsentApi.approve).toHaveBeenCalledWith('req-1')
    })
  })

  it('nega o consentimento ao clicar em Negar', async () => {
    ;(oauthConsentApi.deny as any).mockResolvedValue({
      redirectUrl: 'https://claude.ai/api/mcp/auth_callback?error=access_denied&state=xyz',
    })
    renderConsent()

    await waitFor(() => expect(screen.getByText('Negar')).toBeInTheDocument())
    fireEvent.click(screen.getByText('Negar'))

    await waitFor(() => {
      expect(oauthConsentApi.deny).toHaveBeenCalledWith('req-1')
    })
  })
})
