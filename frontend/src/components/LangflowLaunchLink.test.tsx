import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { LangflowLaunchLink } from './LangflowLaunchLink'

describe('LangflowLaunchLink', () => {
  it('aponta para o launch do backend e não abre outra aba', () => {
    render(<LangflowLaunchLink />)

    const link = screen.getByRole('link', { name: 'Langflow' })
    expect(link).toHaveAttribute('href', '/api/langflow/launch')
    expect(link).not.toHaveAttribute('target')
  })

  it('aceita role de item de menu quando renderizado dentro do menu da conta', () => {
    render(<LangflowLaunchLink role="menuitem" />)

    expect(screen.getByRole('menuitem', { name: 'Langflow' })).toHaveAttribute(
      'href',
      '/api/langflow/launch'
    )
  })
})
