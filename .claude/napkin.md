# Napkin Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
1. **[2026-09-22] Preserve rolling OAuth compatibility during mixed-version deploys.**
   Do instead: dual-write the legacy and current code fields until every older pod is retired, then remove the compatibility path deliberately.
2. **[2026-09-22] Run the repository quality gate before every commit.**
   Do instead: execute `./scripts/quality.sh --pre-commit` and fix any regression before committing.

## Shell & Command Reliability
1. **[2026-09-22] Backend commands run from the backend module.**
   Do instead: use Maven through `backend/mvnw` or the repository-documented backend command.
