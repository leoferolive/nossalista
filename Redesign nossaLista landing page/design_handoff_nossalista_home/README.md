# Handoff: NossaLista — Landing / Página inicial (redesign)

## Overview
Redesign completo da **página inicial pública (landing) do NossaLista** — o app de listas de
compras/tarefas/desejos compartilhadas em tempo real. O objetivo do redesign foi resolver um
problema de hierarquia da versão anterior: vários "pills" de recurso pareciam botões e competiam
com os CTAs reais, deixando confuso onde clicar. Esta versão tem **um único CTA sólido** + um link
de texto secundário, e apresenta os recursos como uma lista discreta de ícone+texto (nunca botões).

Direção visual escolhida: **hero imersivo escuro** com brilhos violeta + teal e um card de lista
"em vidro" (glass) mostrando colaboração ao vivo. O restante da página é claro/neutro.

## About the Design Files
Os arquivos deste pacote são **referências de design feitas em HTML** — um protótipo que mostra a
aparência e o comportamento pretendidos, **não código de produção para copiar diretamente**. A
tarefa é **recriar este design no código real do NossaLista** (React + Vite + Tailwind, com Fraunces
+ Plus Jakarta Sans), usando os componentes e padrões já existentes no projeto. Onde o app já tiver
um componente equivalente (botão, card, avatar, checkbox), use o do projeto em vez de recriar.

- `NossaLista Home.dc.html` — a página completa. É um "Design Component" que depende do runtime
  deste ambiente para renderizar; sirva como referência de layout/estilo e leia os valores exatos
  no HTML inline. Todas as medidas e cores estão neste README também.

## Fidelity
**Alta fidelidade (hi-fi).** Cores, tipografia, espaçamentos e estados finais estão definidos.
Recrie pixel-a-pixel usando as libs/padrões do codebase. As cores da seção clara vêm do design
system **leoferolive** (tokens abaixo); as cores da seção escura do hero estão fixas em hex (também
abaixo) porque são um tratamento específico do hero.

---

## Design Tokens

### Cores — marca (design system leoferolive)
| Token | Light | Dark (data-theme="dark") | Uso |
|---|---|---|---|
| `--accent` (violeta) | `#7c3aed` | `#a78bfa` | ação primária, marca |
| `--accent-strong` | `#6326c9` | `#8b5cf6` | gradiente/hover do primário |
| `--accent-soft` | `rgba(124,58,237,0.12)` | `rgba(167,139,250,0.16)` | fundo de ícone violeta |
| `--primary` (teal) | `#14b8a6` | `#2dd4bf` | status, "concluído", 2º stop do gradiente |
| `--primary-strong` | `#0d9488` | `#14b8a6` | — |
| `--info` (azul) | `#2f6fed` | `#6398f0` | ícone "convite por link" |

### Cores — neutros e superfícies (light / dark)
| Token | Light | Dark |
|---|---|---|
| `--bg` | `#f7f6fb` | `#14101e` |
| `--bg-soft` | `#efedf5` | `#181426` |
| `--surface-strong` | `#ffffff` | `#211b35` |
| `--surface-muted` | `#f1eff7` | `#271f3d` |
| `--text` | `#1e1a2b` | `#ece9f4` |
| `--text-muted` | `#564f68` | `#a39db5` |
| `--text-faint` | `#968fa8` | `#6f6982` |
| `--border` | `rgba(40,25,70,0.10)` | `rgba(255,255,255,0.09)` |
| `--border-strong` | `rgba(40,25,70,0.18)` | `rgba(255,255,255,0.17)` |

### Cores fixas do hero escuro (independentes do tema — o hero é sempre escuro)
- Base do hero: `#14101e`
- Brilhos (radial-gradients sobrepostos):
  - `radial-gradient(1100px 600px at 14% -12%, rgba(124,58,237,0.38), transparent 60%)`
  - `radial-gradient(900px 520px at 96% 8%, rgba(20,184,166,0.20), transparent 55%)`
  - `radial-gradient(760px 520px at 50% 118%, rgba(124,58,237,0.16), transparent 62%)`
- Card glass: fundo `rgba(33,27,53,0.78)`, `backdrop-filter: blur(14px)`, borda `rgba(255,255,255,0.12)`,
  sombra `0 30px 70px rgba(0,0,0,0.45)`. Halo atrás: `radial-gradient(50% 50% at 50% 50%, rgba(124,58,237,0.35), transparent 70%)`.
- Texto no escuro: título `#ece9f4`; parágrafo `rgba(236,233,244,0.68)`; microcopy `rgba(236,233,244,0.45)`.
- Base do CTA final escuro: `#1a1428` + brilhos `rgba(124,58,237,0.5)` e `rgba(20,184,166,0.35)`.
- **IMPORTANTE:** o hero e o CTA final são sempre escuros — ao alternar o tema do site, apenas as
  seções claras (como funciona / recursos / rodapé) mudam para dark; o hero permanece igual.

### Tipografia
- Display/headings: **Fraunces** (serif), `letter-spacing: -0.02em a -0.03em`, weight 700.
- UI/body/números: **Plus Jakarta Sans**, weights 400–800.
- Escala usada: h1 `clamp(40px, 4.8vw, 62px)` / line-height 1.02; h2 seção `clamp(28px, 3.2vw, 40px)`;
  card title 24px; h3 recurso 19px; body 19px (hero) e 14.5px (cards); kicker/eyebrow 11–12px
  uppercase com `letter-spacing: 0.14–0.16em`, weight 800.

### Espaçamento, raio, sombra (design system)
- Base 4px: `4 8 12 16 24 32 48 64`.
- Raio: `--radius-sm 8px` (botões/inputs), `--radius-md 12px` (cards), `--radius-lg 16px` (painéis),
  `999px` (pills). Cards de destaque do hero usam 20px.
- Sombras: `--shadow-card`, `--shadow-card-strong` (neutras); botão primário usa `--shadow-button`
  (glow tingido de accent).
- Container central: `max-width: 1120px`, padding lateral `40px`.

### Motion
- Transições `150–220ms` com `cubic-bezier(0.2,0.8,0.2,1)` (ease-out).
- Botão primário: hover `translateY(-1px)` + sombra mais profunda.
- Card interativo: hover `translateY(-3px)`.
- Respeitar `prefers-reduced-motion`.

---

## Screens / Views

Uma única página (landing), max-width do conteúdo 1120px, responsiva. Ordem das seções:

### 1. Nav (dentro do hero escuro)
- Layout: flex, `align-items:center`, `gap:16px`, `flex-wrap`, padding `22px 40px`.
- Esquerda: marca = quadrado 34×34 raio 10 com gradiente `135deg #7c3aed→#6326c9` e ícone de carrinho
  (Lucide, branco) + wordmark "Nossa**Lista**" (Fraunces 20px; "Lista" em `#a78bfa`).
- Centro: links "Recursos", "Como funciona" (14px, weight 600, `rgba(236,233,244,0.66)`).
- Direita: botão de tema (40×40, ícone lua), link "Entrar" (texto), e botão **Criar conta** (DS Button
  `size="sm"`, primário violeta).

### 2. Hero (fundo escuro imersivo)
- Layout: grid `repeat(auto-fit, minmax(min(100%,400px),1fr))`, `gap:56px`, `align-items:center`,
  padding `60px 40px 84px`. Colapsa para 1 coluna abaixo de ~880px.
- **Coluna de texto:**
  - Eyebrow pill "TEMPO REAL": fundo `rgba(45,212,191,0.12)`, borda `rgba(45,212,191,0.3)`, texto
    `#2dd4bf`, com bolinha teal com glow. Altura 32px, uppercase, tracking 0.14em.
  - H1: "A lista que todo mundo **vê ao mesmo tempo.**" — 2ª linha com gradiente de texto
    `linear-gradient(120deg,#a78bfa,#2dd4bf)` (background-clip:text).
  - Parágrafo: "Compras, tarefas e desejos num lugar só. Você marca um item e todo mundo vê na hora —
    no mercado, em casa, onde estiver." (max 46ch).
  - Ações (`gap:24px`, wrap): **1** botão primário `size="lg"` "Criar conta grátis" + **1** link de
    texto "Já tenho conta →" (branco, com ícone seta). — *Esta é a correção-chave de hierarquia:
    apenas um botão sólido, o resto é link.*
  - Trust line: check teal + "Grátis para sempre · sem cartão de crédito" (13px, `rgba(236,233,244,0.45)`).
  - **Recursos (NÃO são botões):** linha com `border-top` acima, 3 itens flex de ícone+texto:
    "Tudo centralizado" (ícone layers, violeta), "Sincroniza na hora" (ícone refresh, teal),
    "Convite por link" (ícone link, azul). Ícone = quadrado 32×32 raio 9 com fundo tingido a 13–14%.
- **Coluna do card (ilustração):** card "glass" escuro (specs em Design Tokens), max-width 410px:
  - Header: 3 bolinhas (violeta/teal/faint) + "COMPARTILHADA" (kicker) + pill "● 2 online" (teal).
  - Título "Mercado da semana" (24px).
  - 4 linhas de item com checkbox 22×22 raio 7: 1º concluído (quadrado com gradiente teal + check,
    texto riscado/faint), demais com borda `rgba(255,255,255,0.22)`. Itens: "Tomate sweet grape"
    (feito), "Queijo minas", "Iogurte natural", "Lembrete de feira".
  - Rodapé do card: avatar "A" + "Ana marcou o primeiro item — todo mundo viu na hora."

### 3. Como funciona (fundo claro)
- Kicker "COMO FUNCIONA" + h2 "Três passos e pronto", centralizados.
- 3 cards flex (`min-width:250px`) separados por ícones de seta →. Cada card: label "Passo N"
  (Fraunces, cor violeta/teal/azul), h3, parágrafo:
  1. **Crie sua lista** — "Compras, tarefas ou desejos. Dê um nome e pronto."
  2. **Convide quem quiser** — "Mande um link ou o usuário. A pessoa entra e já edita."
  3. **Marquem juntos** — "Cada alteração aparece na hora para todo mundo."

### 4. Recursos (fundo claro)
- Grid `repeat(auto-fit, minmax(min(100%,300px),1fr))`, `gap:20px`. 3 cards (surface-strong, borda,
  shadow-card, padding 26px). Cada um: ícone 48×48 raio 14 tingido + h3 19px + parágrafo 14.5px:
  - **Tudo centralizado** (violeta) — "Suas listas param de viver em papéis soltos e conversas
    perdidas. Uma casa só para organizar a vida."
  - **Tempo real de verdade** (teal) — "Alguém marca um item e todo mundo vê na hora. Sem recarregar,
    sem 'quem já comprou o leite?'."
  - **Compartilhe em segundos** (azul) — "Convide por link ou usuário. Família, colegas de casa ou
    amigos entram e começam a editar juntos."

### 5. CTA final (bloco escuro)
- Bloco raio 16, padding `52px 32px`, centralizado, fundo escuro `#1a1428` + brilhos violeta/teal.
- H2 "Organize junto, hoje" (`#ece9f4`), parágrafo "Crie sua primeira lista compartilhada. É grátis,
  para sempre." e um botão sólido "Criar conta grátis" com gradiente `135deg #9268ee→#6326c9`.

### 6. Rodapé
- `border-top`, padding `26px 40px`, flex: wordmark "NossaLista" + links "Privacidade", "Termos",
  "Contato" (13px, muted).

---

## Interactions & Behavior
- **CTAs**: "Criar conta grátis" → fluxo de cadastro; "Já tenho conta" / "Entrar" → login. Um único
  botão sólido por seção para a hierarquia ficar clara.
- **Toggle de tema** (ícone lua no nav): alterna `data-theme="light|dark"` no elemento raiz. Apenas
  as seções claras respondem; hero e CTA final permanecem escuros por design.
- **Hover**: botão primário sobe 1px + sombra; cards de recurso podem subir 3px (opcional).
- **Responsivo**: hero e grids usam `auto-fit`/`flex-wrap`, colapsando para 1 coluna no mobile. Sem
  media queries — tudo via grid/flex fluido. Tipografia em `clamp()`.
- **Estados**: focar em foco visível (ring 2px violeta) e `prefers-reduced-motion`.

## State Management
- `theme: 'light' | 'dark'` (persistir em localStorage é recomendável no app real).
- Nenhum data-fetching na landing; o card de lista é ilustrativo (dados estáticos de exemplo).

## Assets
- **Ícones**: Lucide (line, 24×24, stroke 2, round). No app real, usar `lucide-react`. Ícones usados:
  carrinho (marca), lua (tema), seta-direita, check, layers (centralizar), refresh-cw (sync),
  link, e o quadrado de checkbox custom.
- **Fontes**: Fraunces + Plus Jakarta Sans (já usadas no NossaLista).
- Sem imagens raster — toda a "ilustração" é o card de lista construído em HTML/CSS.

## Files
- `NossaLista Home.dc.html` — página completa (referência de layout, cores e copy exatos).
- Tokens de origem no design system leoferolive: `tokens/colors.css`, `typography.css`, `spacing.css`.
  Reaproveite os equivalentes já existentes no repositório do NossaLista.
