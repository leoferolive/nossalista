import React from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useWebSocketContext } from '../contexts/WebSocketContext'

export function WebSocketConnectionManager() {
  const { isAuthenticated } = useAuth()
  const { connect, disconnect } = useWebSocketContext()

  React.useEffect(() => {
    if (!isAuthenticated) {
      disconnect()
      return
    }

    connect()
    return () => {
      disconnect()
    }
  }, [isAuthenticated, connect, disconnect])

  return null
}
