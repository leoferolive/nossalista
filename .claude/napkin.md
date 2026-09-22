# Napkin Runbook

## Curation Rules

- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)

1. **[2026-09-22] Run the repository quality gate before committing.**
   Do instead: run `./scripts/quality.sh --pre-commit` and fix regressions before every commit.

## Domain Behavior Guardrails

1. **[2026-09-22] OAuth authorization-request cookies must fail closed.**
   Do instead: validate HMAC and every time boundary before deserializing the request.
