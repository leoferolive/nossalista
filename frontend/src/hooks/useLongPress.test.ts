import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useLongPress } from './useLongPress'

describe('useLongPress', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('deve disparar onLongPress após 1 segundo (1000ms)', () => {
    const onLongPress = vi.fn()
    const { result } = renderHook(() => useLongPress({ onLongPress, delay: 1000 }))

    // Simular mouse down
    act(() => {
      result.current.onMouseDown()
    })

    // Verificar que ainda não disparou
    expect(onLongPress).not.toHaveBeenCalled()

    // Avançar 999ms - ainda não deve disparar
    act(() => {
      vi.advanceTimersByTime(999)
    })
    expect(onLongPress).not.toHaveBeenCalled()

    // Avançar mais 1ms (completa 1000ms)
    act(() => {
      vi.advanceTimersByTime(1)
    })

    // Agora deve ter disparado
    expect(onLongPress).toHaveBeenCalledTimes(1)
  })

  it('NÃO deve disparar onLongPress se soltar antes de 1 segundo', () => {
    const onLongPress = vi.fn()
    const { result } = renderHook(() => useLongPress({ onLongPress, delay: 1000 }))

    // Simular mouse down
    act(() => {
      result.current.onMouseDown()
    })

    // Avançar apenas 500ms
    act(() => {
      vi.advanceTimersByTime(500)
    })

    // Simular mouse up (soltar antes de 1 segundo)
    const mockEvent = {
      preventDefault: vi.fn(),
      stopPropagation: vi.fn(),
    } as unknown as React.MouseEvent

    act(() => {
      result.current.onMouseUp(mockEvent)
    })

    // Avançar o restante do tempo
    act(() => {
      vi.advanceTimersByTime(500)
    })

    // Não deve ter disparado
    expect(onLongPress).not.toHaveBeenCalled()
  })

  it('deve cancelar long-press se mouse sair do elemento', () => {
    const onLongPress = vi.fn()
    const { result } = renderHook(() => useLongPress({ onLongPress, delay: 1000 }))

    // Simular mouse down
    act(() => {
      result.current.onMouseDown()
    })

    // Avançar 500ms
    act(() => {
      vi.advanceTimersByTime(500)
    })

    // Mouse leave
    act(() => {
      result.current.onMouseLeave()
    })

    // Avançar o restante
    act(() => {
      vi.advanceTimersByTime(500)
    })

    // Não deve ter disparado
    expect(onLongPress).not.toHaveBeenCalled()
  })

  it('deve prevenir click após long-press bem-sucedido', () => {
    const onLongPress = vi.fn()
    const { result } = renderHook(() => useLongPress({ onLongPress, delay: 1000 }))

    // Simular mouse down
    act(() => {
      result.current.onMouseDown()
    })

    // Completar long-press
    act(() => {
      vi.advanceTimersByTime(1000)
    })

    expect(onLongPress).toHaveBeenCalledTimes(1)

    // Simular mouse up APÓS long-press
    const mockEvent = {
      preventDefault: vi.fn(),
      stopPropagation: vi.fn(),
    } as unknown as React.MouseEvent

    act(() => {
      result.current.onMouseUp(mockEvent)
    })

    // Deve ter chamado preventDefault e stopPropagation
    expect(mockEvent.preventDefault).toHaveBeenCalled()
    expect(mockEvent.stopPropagation).toHaveBeenCalled()
  })

  it('NÃO deve prevenir click se NÃO foi long-press', () => {
    const onLongPress = vi.fn()
    const { result } = renderHook(() => useLongPress({ onLongPress, delay: 1000 }))

    // Simular mouse down
    act(() => {
      result.current.onMouseDown()
    })

    // Soltar ANTES de completar
    const mockEvent = {
      preventDefault: vi.fn(),
      stopPropagation: vi.fn(),
    } as unknown as React.MouseEvent

    act(() => {
      result.current.onMouseUp(mockEvent)
    })

    // NÃO deve ter chamado preventDefault/stopPropagation
    expect(mockEvent.preventDefault).not.toHaveBeenCalled()
    expect(mockEvent.stopPropagation).not.toHaveBeenCalled()
  })
})
