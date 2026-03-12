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

## Decisions

| Decision                                                                       | Rationale                                                                       | Date         |
| ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------- | ------------ |
| `Fresh Lists replaces paper tech editorial as the active identity.`            | `The product needed a lighter, happier and more legible brand expression.`      | `2026-03-10` |
| `Landing stays intentionally minimal while the product remains utility-first.` | `Public acquisition and daily product use need different information density.`  | `2026-03-10` |
| `Coral drives action and teal signals progress/status.`                        | `The pairing feels vivid and collaborative without becoming childish.`          | `2026-03-10` |
| `Authenticated mobile navigation now uses compact headers and bottom sheets.`  | `Mobile needed content-first hierarchy and menus that never overflow sideways.` | `2026-03-11` |
