import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

function preservePendingInviteFromLegacyRedirect(redirectPath: string | null) {
  if (!redirectPath?.startsWith('/join/')) {
    return
  }

  const inviteCode = redirectPath.slice('/join/'.length)
  if (inviteCode) {
    sessionStorage.setItem('pendingInviteCode', inviteCode)
  }
}

export function LegacyLoginRedirect() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    preservePendingInviteFromLegacyRedirect(searchParams.get('redirect'))

    const nextSearchParams = new URLSearchParams()
    nextSearchParams.set('auth', 'login')

    if (searchParams.get('registered') === '1') {
      nextSearchParams.set('registered', '1')
    }

    const email = searchParams.get('email')
    if (email) {
      nextSearchParams.set('email', email)
    }

    navigate(`/?${nextSearchParams.toString()}`, { replace: true })
  }, [navigate, searchParams])

  return (
    <div className="nl-page flex items-center justify-center">
      <div className="nl-card px-6 py-4 text-nl-muted">Redirecionando...</div>
    </div>
  )
}
