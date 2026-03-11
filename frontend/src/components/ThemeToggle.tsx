import { useTheme } from '../contexts/ThemeContext'

interface ThemeToggleProps {
  className?: string
}

export function ThemeToggle({ className }: ThemeToggleProps) {
  const { theme, setTheme } = useTheme()

  return (
    <div className={['nl-theme-toggle', className].filter(Boolean).join(' ')}>
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
