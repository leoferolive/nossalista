import { useEffect, useRef, useState, type ReactNode } from 'react'
import { ResponsiveSheet } from './ResponsiveSheet'
import { useIsMobile } from '../hooks/useIsMobile'

interface ActionMenuItem {
  label: string
  icon?: ReactNode
  tone?: 'default' | 'danger'
  onSelect: () => void
}

interface ResponsiveActionMenuProps {
  triggerLabel: string
  title: string
  items: ActionMenuItem[]
}

export function ResponsiveActionMenu({ triggerLabel, title, items }: ResponsiveActionMenuProps) {
  const isMobile = useIsMobile()
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!isOpen || isMobile) {
      return undefined
    }

    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isMobile, isOpen])

  const content = (
    <>
      {items.map((item) => (
        <button
          key={item.label}
          type="button"
          onClick={() => {
            setIsOpen(false)
            item.onSelect()
          }}
          className={`nl-action-row ${item.tone === 'danger' ? 'nl-action-row-danger' : ''}`}
        >
          <span className="flex items-center gap-3">
            {item.icon && <span aria-hidden="true">{item.icon}</span>}
            <span>{item.label}</span>
          </span>
        </button>
      ))}
    </>
  )

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className="nl-btn-secondary"
        aria-label={triggerLabel}
      >
        <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
          <circle cx="10" cy="4" r="1.5" />
          <circle cx="10" cy="10" r="1.5" />
          <circle cx="10" cy="16" r="1.5" />
        </svg>
        <span>Mais</span>
      </button>

      {isMobile ? (
        <ResponsiveSheet
          isOpen={isOpen}
          onClose={() => setIsOpen(false)}
          title={title}
          description="Acoes secundarias da lista, organizadas para caber melhor no celular."
        >
          <div className="space-y-2">{content}</div>
        </ResponsiveSheet>
      ) : null}

      {!isMobile && isOpen ? (
        <div
          className="absolute right-0 z-30 mt-2 w-56 overflow-hidden rounded-2xl border border-nl-border bg-nl-surface p-2 shadow-earthen-strong"
          role="menu"
        >
          <div className="space-y-1" role="none">
            {items.map((item) => (
              <button
                key={item.label}
                type="button"
                role="menuitem"
                onClick={() => {
                  setIsOpen(false)
                  item.onSelect()
                }}
                className={`nl-action-row ${item.tone === 'danger' ? 'nl-action-row-danger' : ''}`}
              >
                <span className="flex items-center gap-3">
                  {item.icon && <span aria-hidden="true">{item.icon}</span>}
                  <span>{item.label}</span>
                </span>
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  )
}
