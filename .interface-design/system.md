# Fresh Lists System

## Direction

- Personality: `Playful Editorial`
- Foundation: `tinted`
- Depth: `subtle-shadows`

## Tokens

### Spacing

- Base: `8px`
- Scale: `8, 12, 16, 24, 32, 48, 64`

### Colors

- Foreground: `#202532`
- Secondary: `#2b6f6b`
- Muted: `#667085`
- Faint: `#fdf1ea`
- Accent: `#ff7a59`

### Radius

- Scale: `12px, 18px, 28px`

### Typography

- Font family: `Plus Jakarta Sans + Fraunces`
- Scale: `12, 14, 16, 18, 24, 32, 48`
- Weights: `500, 600, 700, 800`

## Core Patterns

### Button Primary

- Height: `48px`
- Padding: `0 20px`
- Radius: `16px`
- Typography: `14px / 800`
- Usage: `principal CTA, submit, creation flow`

### Card Default

- Border or shadow: `1px warm border + soft ambient shadow`
- Padding: `24px`
- Radius: `28px`
- Background: `clean elevated surface`
- Usage: `main product containers and hero shells`

### Home List Card (Mobile First)

- Structure: `3 zones (header, informative body, footer affordance)`
- Touch model: `entire card is the primary touch target`
- Informative indicators: `never reuse interactive checkbox visuals`
- Title behavior: `allow up to 2 lines before truncation`
- Usage: `home list discovery and quick entry`

### Input Default

- Height: `52px`
- Padding: `0 16px`
- Radius: `16px`
- Border/focus treatment: `soft border + coral/teal ring`
- Usage: `all auth, modal and settings inputs`

### Authenticated Mobile Shell

- Header model: `3-zone mobile shell`
- Top row: `eyebrow + optional back + utility icons + account entry`
- Action rule: `only one primary action visible in the header on mobile`
- Secondary actions: `move to action strip or overflow sheet`
- Menu pattern: `bottom sheet on mobile, dropdown on desktop`
- Theme pattern: `stays visible on desktop header, moves into account sheet on mobile`
- Usage: `home, profile, list detail and future authenticated product flows`
- Density mode: `mobile compact (reduced header/card/sheet/action-row heights while preserving 44px tap targets)`

### Loading Indicators

- Spinner: `nl-spinner` (3rem), `nl-spinner-sm` (1.25rem), `nl-spinner-lg` (4rem)
- Colors: `coral (top) + teal (right)`, breathing scale animation
- Usage: `replaces generic animate-spin rounded-full border spinners`

### Skeleton Loading

- Class: `nl-skeleton`
- Shimmer: `warm gradient sweep (surface-strong → accent 8% → surface-strong), 1.8s`
- Usage: `replaces animate-pulse bg-nl-surface-strong skeletons`

### Toast Notifications

- Container: `nl-card-soft + shadow-earthen`
- Icons: `SVG (checkmark-circle, x-circle, info-circle)`
- Stacking: `top: calc(1rem + index * 4.5rem)`
- Z-index: `z-[70]` (above sheets at z-60)
- Exit: `fade-out animation before unmount`

### Icon System

- Interactive icons: `SVG inline only — never text characters`
- Data-driven emoji: `keep (🛒 ✅ 🎁 📝 for list types)`
- Buttons with gradients: `always use nl-btn-* classes, never raw Tailwind gradients`

## Decisions

| Decision                                                                       | Rationale                                                                       | Date         |
| ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------- | ------------ |
| `Fresh Lists replaces paper tech editorial as the active identity.`            | `The product needed a lighter, happier and more legible brand expression.`      | `2026-03-10` |
| `Landing stays intentionally minimal while the product remains utility-first.` | `Public acquisition and daily product use need different information density.`  | `2026-03-10` |
| `Coral drives action and teal signals progress/status.`                        | `The pairing feels vivid and collaborative without becoming childish.`          | `2026-03-10` |
| `Authenticated mobile navigation now uses compact headers and bottom sheets.`  | `Mobile needed content-first hierarchy and menus that never overflow sideways.` | `2026-03-11` |
| `Guided onboarding and account menus must reuse global nl tokens and surfaces.` | `Avoids visual drift and keeps readability parity in both themes.`             | `2026-03-12` |
| `Informational states must not reuse interactive checkbox affordances.`         | `Prevents false affordance and keeps interaction semantics trustworthy.`        | `2026-03-12` |
| `Home list cards now follow a strict 3-zone mobile pattern.`                   | `Improves scanability, touch clarity and consistent hierarchy on small screens.` | `2026-03-12` |
| `Product microcopy follows PT-BR diacritics and AA-oriented badge contrast.`   | `Improves readability and avoids perceived quality regressions in mobile use.` | `2026-03-12` |
| `Mobile compact mode is the default for list-heavy authenticated screens.` | `List and action-heavy flows need less vertical friction without changing identity.` | `2026-03-13` |
| `Loading indicators use nl-spinner; skeletons use nl-skeleton shimmer.` | `Generic Tailwind spinners and pulse skeletons lacked brand identity.` | `2026-03-18` |
| `Toasts use nl-card-soft, SVG icons, stacking, and z-[70].` | `Text icons and flat colors made toasts look AI-generated.` | `2026-03-18` |
| `Interactive icons are SVG inline; text characters (✎ ⌫ ↗ ↺ ⌂ •) replaced.` | `Text characters render inconsistently and lack visual control.` | `2026-03-18` |
| `Raw Tailwind gradients (orange/amber/red) replaced by nl-btn-* classes.` | `Prevents color drift and ensures theme parity in dark mode.` | `2026-03-18` |
| `Google button uses CSS variables for full theme parity.` | `Hardcoded #fff and #1f1f1f created a jarring white rectangle in dark mode.` | `2026-03-18` |
