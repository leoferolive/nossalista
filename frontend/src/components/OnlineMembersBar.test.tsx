import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { OnlineMembersBar } from './OnlineMembersBar'
import { OnlineMember } from '../types/OnlineMember'

describe('OnlineMembersBar', () => {
  const selfMember: OnlineMember = {
    userId: 'user-self',
    username: 'leo',
    name: 'Leo',
    avatarUrl: null,
  }

  it('exibe "Apenas você online agora" quando só o próprio usuário está online', () => {
    render(<OnlineMembersBar members={[selfMember]} currentUserId={selfMember.userId} />)

    expect(screen.getByText('Apenas você online agora')).toBeInTheDocument()
  })

  it('exibe "Online agora: 2" com dois avatares', () => {
    const secondMember: OnlineMember = {
      userId: 'user-2',
      username: 'maria',
      name: 'Maria',
      avatarUrl: 'https://example.com/maria.png',
    }

    render(
      <OnlineMembersBar members={[selfMember, secondMember]} currentUserId={selfMember.userId} />
    )

    expect(screen.getByText('Online agora: 2')).toBeInTheDocument()
    expect(screen.getAllByRole('img')).toHaveLength(1)
    expect(screen.getByText('LE')).toBeInTheDocument()
  })

  it('exibe 5 avatares + "+1" quando há 6 membros online', () => {
    const members: OnlineMember[] = Array.from({ length: 6 }, (_, index) => ({
      userId: `user-${index + 1}`,
      username: `user${index + 1}`,
      name: `User ${index + 1}`,
      avatarUrl: `https://example.com/user-${index + 1}.png`,
    }))

    render(<OnlineMembersBar members={members} currentUserId={members[0].userId} />)

    expect(screen.getByText('Online agora: 6')).toBeInTheDocument()
    expect(screen.getAllByRole('img')).toHaveLength(5)
    expect(screen.getByText('+1')).toBeInTheDocument()
  })

  it('exibe iniciais quando avatarUrl é null', () => {
    const memberWithoutAvatar: OnlineMember = {
      userId: 'user-2',
      username: 'maria',
      name: 'Maria Silva',
      avatarUrl: null,
    }

    render(
      <OnlineMembersBar
        members={[selfMember, memberWithoutAvatar]}
        currentUserId={selfMember.userId}
      />
    )

    expect(screen.getByText('MS')).toBeInTheDocument()
  })
})
