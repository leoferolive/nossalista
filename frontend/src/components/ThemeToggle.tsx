import { useTheme } from '../contexts/ThemeContext'

interface ThemeToggleProps {
  className?: string
  fullWidth?: boolean
}

export function ThemeToggle({ className, fullWidth = false }: ThemeToggleProps) {
  const { theme, setTheme } = useTheme()

  return (
    <div
      className={['nl-theme-toggle', fullWidth ? 'nl-theme-toggle-full' : '', className]
        .filter(Boolean)
        .join(' ')}
    >
      <button type="button" data-active={theme === 'light'} onClick={() => setTheme('light')}>
        <span aria-hidden="true">Sun</span>
        Claro
      </button>
      <button type="button" data-active={theme === 'dark'} onClick={() => setTheme('dark')}>
        <span aria-hidden="true">Moon</span>
        Escuro
      </button>
    </div>
  )
}
