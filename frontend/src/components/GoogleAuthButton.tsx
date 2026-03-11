interface GoogleAuthButtonProps {
  label?: string
  onClick: () => void
  className?: string
}

function GoogleLogo() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill="#4285F4"
        d="M23.49 12.27c0-.79-.07-1.54-.2-2.27H12.24v4.29h6.31a5.41 5.41 0 0 1-2.34 3.55v2.95h3.79c2.22-2.04 3.49-5.04 3.49-8.52Z"
      />
      <path
        fill="#34A853"
        d="M12.24 24c3.24 0 5.96-1.08 7.95-2.93l-3.79-2.95c-1.05.71-2.4 1.14-4.16 1.14-3.19 0-5.89-2.15-6.86-5.04H1.47v3.05A12 12 0 0 0 12.24 24Z"
      />
      <path
        fill="#FBBC05"
        d="M5.38 14.22a7.2 7.2 0 0 1 0-4.44V6.73H1.47a12 12 0 0 0 0 10.54l3.91-3.05Z"
      />
      <path
        fill="#EA4335"
        d="M12.24 4.77c1.85 0 3.51.64 4.81 1.89l3.6-3.6C18.19.79 15.47 0 12.24 0A12 12 0 0 0 1.47 6.73l3.91 3.05c.97-2.89 3.67-5.01 6.86-5.01Z"
      />
    </svg>
  )
}

export function GoogleAuthButton({
  label = 'Continuar com Google',
  onClick,
  className,
}: GoogleAuthButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={['nl-btn-google', className].filter(Boolean).join(' ')}
    >
      <span className="nl-btn-google__mark">
        <GoogleLogo />
      </span>
      <span>{label}</span>
    </button>
  )
}
