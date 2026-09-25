# Como aplicar o leoferolive design no NossaLista

Este kit re-skina o **NossaLista** com a identidade unificada (Violeta + Teal,
fontes self-hosted) **sem reescrever nenhum componente**. O app inteiro já usa as
variáveis `--nl-*`; nós apenas trocamos os *valores* dessas variáveis.

> ⏱️ ~5 minutos · risco baixo · 100% reversível (é só remover 1 linha).

## O que tem neste kit
```
integrations/nossalista/
├─ leoferolive-tokens.css   ← o override (cores light+dark + @font-face)
├─ fonts/
│  ├─ Fraunces.ttf
│  └─ PlusJakartaSans.ttf
└─ COMO-APLICAR.md          ← este guia
```

---

## Passo 1 — Copiar os arquivos pro repo do NossaLista
No `frontend/` do NossaLista, crie uma pasta e copie tudo pra lá:

```
frontend/src/styles/leoferolive-tokens.css
frontend/src/styles/fonts/Fraunces.ttf
frontend/src/styles/fonts/PlusJakartaSans.ttf
```

(As `url()` dentro do CSS já apontam pra `./fonts/...`, então mantenha o
`leoferolive-tokens.css` e a pasta `fonts/` juntos.)

## Passo 2 — Importar no final do `src/index.css`
Abra `frontend/src/index.css` e faça **duas coisas**:

**a)** Remova (ou comente) a primeira linha, o import do Google Fonts — agora as
fontes são locais:
```css
/* @import url('https://fonts.googleapis.com/css2?family=Fraunces...'); */
```

**b)** Adicione esta linha **no final do arquivo** (precisa ser depois dos blocos
`:root` existentes pra vencer na cascata):
```css
@import './styles/leoferolive-tokens.css';
```

Pronto. Salve e rode `npm run dev` — o app inteiro (botões, cards, inputs, badges,
modais, dark mode) aparece na nova identidade. **Nenhum componente foi tocado.**

---

## Por que funciona
O `index.css` do NossaLista define toda a marca em variáveis (`--nl-accent`,
`--nl-bg`, `--nl-text`…) e as classes `.nl-btn`, `.nl-card`, etc. as consomem.
O `tailwind.config.js` também lê essas variáveis (`nl-primary`, `nl-accent`…).
Como o CSS em cascata usa o **último** valor declarado, importar nosso arquivo por
último sobrescreve só os valores — a estrutura continua igual.

| Antes (NossaLista) | Agora (leoferolive design) |
|---|---|
| `--nl-accent` = coral `#ff7a59` | violeta `#7c3aed` (ação/CTA) |
| `--nl-primary` = teal `#18b7a1` | teal `#14b8a6` (status/progresso) |
| fundo "papel" quente + grid | slate neutro limpo, brilho suave |
| fontes via Google CDN | Fraunces + Plus Jakarta self-hosted |

## Ajustes opcionais
- **Quer o grid de "papel" de volta?** No `leoferolive-tokens.css`, troque
  `--nl-paper-line: transparent;` por algo como `rgba(86,79,104,0.06);`.
- **Sombras mais suaves/fortes?** Edite `--nl-card-shadow`.
- As cores semânticas (`--nl-danger`, `--nl-success`) e o `--nl-focus` já
  acompanham a nova marca.

## Reverter
Remova a linha `@import './styles/leoferolive-tokens.css';` (e descomente o
import do Google Fonts, se quiser as fontes via CDN de novo). Volta ao original.

---

## Próximos apps
O **NossaGrana** e o **Miles Guard** seguem a mesma receita — só muda o prefixo
das variáveis de cada um. Quando quiser, eu gero os kits `integrations/nossagrana/`
e `integrations/miles-guard/` no mesmo formato.

> **Destino ideal (quando estabilizar):** transformar este `leoferolive-tokens.css`
> + fontes num pacote único (`@leoferolive/design`) publicado, que os três apps
> instalam via `npm`. Aí a marca tem uma fonte de verdade versionada, e atualizar
> todos os apps é trocar a versão do pacote. Posso montar essa estrutura quando você quiser.
