import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render } from '@testing-library/react'
import { WebSocketConnectionManager } from './WebSocketConnectionManager'

const mockConnect = vi.fn()
const mockDisconnect = vi.fn()
let mockAuthenticated = true

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: mockAuthenticated,
  }),
}))

vi.mock('../contexts/WebSocketContext', () => ({
  useWebSocketContext: () => ({
    connect: mockConnect,
    disconnect: mockDisconnect,
  }),
}))

describe('WebSocketConnectionManager', () => {
  beforeEach(() => {
    mockConnect.mockReset()
    mockDisconnect.mockReset()
    mockAuthenticated = true
  })

  it('conecta websocket quando usuário está autenticado', () => {
    render(<WebSocketConnectionManager />)
    expect(mockConnect).toHaveBeenCalledTimes(1)
    expect(mockDisconnect).not.toHaveBeenCalled()
  })

  it('desconecta websocket quando usuário não está autenticado', () => {
    mockAuthenticated = false
    render(<WebSocketConnectionManager />)
    expect(mockDisconnect).toHaveBeenCalledTimes(1)
    expect(mockConnect).not.toHaveBeenCalled()
  })
})
