import { Link, useNavigate } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { CopyableCode } from '../components/CopyableCode'

const MCP_URL = 'https://nossalista.leoferolive.com.br/mcp'
const TOKEN_PLACEHOLDER = 'nlmcp_SEU_TOKEN_AQUI'

const claudeCodeCommand = `claude mcp add --transport http nossalista ${MCP_URL} \\
  --header "Authorization: Bearer ${TOKEN_PLACEHOLDER}"`

const claudeDesktopConfig = `{
  "mcpServers": {
    "nossalista": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "${MCP_URL}",
        "--header",
        "Authorization: Bearer ${TOKEN_PLACEHOLDER}"
      ]
    }
  }
}`

const cursorConfig = `{
  "mcpServers": {
    "nossalista": {
      "url": "${MCP_URL}",
      "headers": {
        "Authorization": "Bearer ${TOKEN_PLACEHOLDER}"
      }
    }
  }
}`

interface ClientGuideProps {
  title: string
  description: string
  children: React.ReactNode
}

function ClientGuide({ title, description, children }: ClientGuideProps) {
  return (
    <section className="nl-card space-y-4 p-5">
      <div>
        <h2 className="font-display text-xl font-semibold text-nl-text">{title}</h2>
        <p className="mt-1 text-sm text-nl-muted">{description}</p>
      </div>
      {children}
    </section>
  )
}

function BeforeYouStartSection() {
  return (
    <section className="nl-card space-y-3 p-5">
      <h2 className="font-display text-xl font-semibold text-nl-text">Antes de começar</h2>
      <p className="text-sm leading-6 text-nl-muted">
        Você precisa de um <strong className="text-nl-text">Personal Access Token</strong> — gerado
        na tela{' '}
        <Link to="/connections" className="font-medium text-nl-accent hover:underline">
          Conexões (API/Assistentes)
        </Link>
        . Cada assistente conectado usa o header{' '}
        <code className="rounded bg-nl-surface-muted px-1.5 py-0.5 font-mono text-xs">
          Authorization: Bearer nlmcp_...
        </code>{' '}
        para se autenticar em seu nome.
      </p>
      <div className="nl-alert" role="alert">
        O valor completo do token só é exibido uma única vez, no momento em que você o cria. Se
        perdê-lo, revogue-o e gere um novo.
      </div>
      <p className="text-sm leading-6 text-nl-muted">
        Tokens de escopo <strong className="text-nl-text">somente leitura</strong> podem consultar
        suas listas, mas não criar, editar ou excluir nada. Para deixar o assistente criar listas,
        adicionar itens ou fazer qualquer alteração, crie o token com escopo{' '}
        <strong className="text-nl-text">leitura e escrita</strong>.
      </p>
    </section>
  )
}

function CreateTokenCallToAction() {
  return (
    <section className="nl-card-soft p-5 text-center">
      <p className="text-sm text-nl-muted">Ainda não criou um token?</p>
      <Link to="/connections" className="nl-btn-primary mt-3 inline-flex">
        Ir para Conexões
      </Link>
    </section>
  )
}

/**
 * Página de ajuda "Conectar seu assistente": passo a passo para ligar um
 * cliente MCP (Claude Code, Claude Desktop, Cursor) à conta do usuário via
 * Personal Access Token. Conteúdo alinhado com `docs/mcp.md` (fonte técnica
 * completa, incluindo tools disponíveis e limitações).
 */
export const ConnectAssistant: React.FC = () => {
  const navigate = useNavigate()

  return (
    <div className="nl-page">
      <div className="nl-container max-w-3xl">
        <AppHeader
          eyebrow="Conexões"
          title="Conectar seu assistente"
          subtitle="Ligue Claude Code, Claude Desktop ou Cursor à sua conta para criar e gerenciar listas por conversa."
          onBack={() => navigate('/connections')}
        />

        <div className="space-y-5">
          <BeforeYouStartSection />

          <ClientGuide
            title="Claude Code"
            description="Rode este comando no terminal, substituindo o token pelo valor gerado na tela Conexões."
          >
            <CopyableCode label="Terminal" code={claudeCodeCommand} />
          </ClientGuide>

          <ClientGuide
            title="Claude Desktop"
            description={
              'Claude Desktop ainda não fala o protocolo do NossaLista nativamente para servidores ' +
              'remotos — adicione esta configuração em claude_desktop_config.json (usa a ponte mcp-remote).'
            }
          >
            <CopyableCode label="claude_desktop_config.json" code={claudeDesktopConfig} />
          </ClientGuide>

          <ClientGuide
            title="Cursor"
            description="Adicione esta configuração em .cursor/mcp.json (ou nas configurações de MCP do Cursor)."
          >
            <CopyableCode label=".cursor/mcp.json" code={cursorConfig} />
          </ClientGuide>

          <CreateTokenCallToAction />
        </div>
      </div>
    </div>
  )
}
