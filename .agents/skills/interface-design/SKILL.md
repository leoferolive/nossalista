---
name: interface-design
description: Design and refine product interfaces with a persistent system for spacing, depth, surfaces, typography, and component patterns. Use when Codex is asked to build or restyle dashboards, apps, authenticated flows, admin panels, settings pages, tables, forms, list/detail views, or to audit/extract a reusable interface system from existing UI code. Do not use for marketing sites or brand campaigns.
---

# Interface Design

## Overview

Establish a clear interface direction, keep it consistent across components, and persist the decisions in `.interface-design/system.md` when the product benefits from cross-session memory.

Prefer product UI over promotional pages. Favor explicit decisions, reusable patterns, and theme parity instead of ad hoc styling.

## Workflow

### 1. Check the context

- Confirm the request is for application UI such as dashboards, internal tools, CRUD screens, settings, tables, forms, or collaborative product surfaces.
- If the request is for a landing page or campaign page, prefer another frontend skill instead of this one.
- Inspect existing tokens, shared primitives, and modal/form/theme infrastructure before inventing local styles.

### 2. Load or establish the system

- If `.interface-design/system.md` exists, read it first and treat it as the source of truth for visual decisions.
- If it does not exist, infer a starting direction from the product context and summarize the proposed decisions before implementing major UI changes.
- Keep the decision set small and concrete:
  - direction/personality
  - foundation and contrast
  - depth strategy
  - spacing base and scale
  - radius and typography behavior
  - component patterns that must stay stable

Use [system-template.md](references/system-template.md) when creating or refreshing the file. Use [directions.md](references/directions.md) when choosing a fitting visual direction.

### 3. Announce decisions before building

Before building a new component or page, state the governing choices in compact form. Example:

```text
Direction: Precision & Density
Depth: borders-only
Spacing: 8px base
Card pattern: 16px padding, faint border, no heavy shadow
```

Keep the statement short. The goal is consistency, not ceremony.

### 4. Build with system discipline

- Reuse existing primitives first.
- Keep spacing and sizing on a deliberate scale.
- Make depth and surface treatment consistent across cards, panels, menus, and modals.
- Preserve equal visual quality in light and dark themes.
- Prefer stable component patterns over one-off exceptions.
- When adding a new reusable pattern, decide whether it belongs in the system file.

### 5. Save memory when the direction stabilizes

- Offer to save or update `.interface-design/system.md` once the direction has proven useful across more than one component or screen.
- Record decisions as durable patterns, not implementation trivia.
- Update the system file when a new pattern becomes canonical or an old one is intentionally replaced.

### 6. Audit or extract when requested

For audits:
- Compare implemented UI against `.interface-design/system.md`.
- Report concrete drifts: spacing, radius, surface elevation, typography, hierarchy, interaction density, and theme mismatch.
- Prioritize bugs and consistency regressions over subjective taste.

For extraction:
- Read representative UI files.
- Infer repeated values and patterns.
- Distill them into `.interface-design/system.md` without copying every implementation detail.
- Prefer a small, opinionated system over a noisy inventory.

## NossaLista Notes

When using this skill inside NossaLista:
- Treat `paper tech editorial` as the default product language.
- Keep `light` and `dark` as first-class themes with equal refinement.
- Preserve distinct login and signup flows on the public landing experience.
- Prefer global theme, form, and modal primitives before creating local variants.

## References

- [directions.md](references/directions.md): choose a direction based on product shape and UX goals.
- [system-template.md](references/system-template.md): create or refresh `.interface-design/system.md`.
