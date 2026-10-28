import React from 'react'

/**
 * Launch do embed do Langflow (spike, docs/langflow-embed.md): o backend grava o cookie
 * `nl_lf_token` e responde 302 para `/langflow/`.
 */
export const LANGFLOW_LAUNCH_PATH = '/api/langflow/launch'

interface LangflowLaunchLinkProps {
  role?: 'menuitem'
}

/**
 * Link de navegação completa para o Langflow, na mesma aba.
 *
 * <p>Não usa react-router: o cookie do Langflow só é gravado pela resposta HTTP do launch,
 * então a navegação precisa ser de página inteira. Sem {@code target}, para não abrir outra aba.</p>
 *
 * <p>Exemplo: {@code <LangflowLaunchLink role="menuitem" />} dentro do menu da conta desktop.</p>
 */
export const LangflowLaunchLink: React.FC<LangflowLaunchLinkProps> = ({ role }) => (
  <a href={LANGFLOW_LAUNCH_PATH} role={role} className="nl-action-row">
    <span className="flex items-center gap-3">
      <svg
        className="h-4 w-4"
        viewBox="0 0 20 20"
        fill="none"
        stroke="currentColor"
        strokeWidth={1.6}
        aria-hidden="true"
      >
        <rect x="2.5" y="3" width="5" height="4" rx="1" />
        <rect x="12.5" y="13" width="5" height="4" rx="1" />
        <path strokeLinecap="round" d="M7.5 5H10a2.5 2.5 0 012.5 2.5V13" />
      </svg>
      <span>Langflow</span>
    </span>
  </a>
)
