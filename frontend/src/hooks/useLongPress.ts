import { useCallback, useEffect, useRef } from 'react'

interface UseLongPressOptions {
  onLongPress: () => void
  delay?: number // milliseconds (default: 1000ms = 1 segundo)
}

/**
 * Hook para detectar long-press (pressionamento longo)
 * Usado para abrir menu de opções em dispositivos móveis
 *
 * @example
 * const longPressProps = useLongPress({
 *   onLongPress: () => setMenuOpen(true),
 *   delay: 1000
 * });
 *
 * <div {...longPressProps}>Item</div>
 */
export function useLongPress({ onLongPress, delay = 1000 }: UseLongPressOptions) {
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const isLongPressTriggered = useRef(false)

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [])

  const start = useCallback(() => {
    isLongPressTriggered.current = false
    timeoutRef.current = setTimeout(() => {
      isLongPressTriggered.current = true
      onLongPress()
    }, delay)
  }, [onLongPress, delay])

  const clear = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
      timeoutRef.current = null
    }
  }, [])

  const stop = useCallback(
    (e: React.MouseEvent | React.TouchEvent) => {
      clear()
      // Se foi long press, previne o evento de click
      if (isLongPressTriggered.current) {
        e.preventDefault()
        e.stopPropagation()
      }
    },
    [clear]
  )

  return {
    onMouseDown: start,
    onMouseUp: stop,
    onMouseLeave: clear,
    onTouchStart: start,
    onTouchEnd: stop,
    onContextMenu: (e: React.MouseEvent) => {
      // Prevenir menu de contexto padrão no long press
      e.preventDefault()
    },
  }
}
