#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/frontend"
OUT_DIR="$REPO_ROOT/.quality-output"
mkdir -p "$OUT_DIR"

MODE="${1:-full}"
cd "$FRONTEND_DIR"

if [[ "$MODE" == "pre-commit" ]]; then
    npm run lint
    npm run typecheck
    exit 0
fi

# full
ESLINT_REPORT="$OUT_DIR/eslint-report.json"
# ESLint 9 flat config ignora --ext; o pattern de arquivos vem do eslint.config.js
# (files: ['**/*.{ts,tsx}']). Manter --ext aqui criava divergência silenciosa com
# `npm run lint`.
npx eslint . --format json --output-file "$ESLINT_REPORT" || true
npm run typecheck
npm run test:coverage

python3 - <<PY
import json
from pathlib import Path

metrics = {}

# ESLint violations
report = json.loads(Path("$ESLINT_REPORT").read_text())
errors = sum(f["errorCount"] for f in report)
warnings = sum(f["warningCount"] for f in report)
metrics["frontend_eslint_errors"]   = {"value": errors,   "direction": "lower_is_better"}
metrics["frontend_eslint_warnings"] = {"value": warnings, "direction": "lower_is_better"}

# Vitest coverage-summary.json
cov_path = Path("$FRONTEND_DIR/coverage/coverage-summary.json")
if cov_path.exists():
    cov = json.loads(cov_path.read_text())["total"]
    metrics["frontend_coverage_line"]   = {"value": round(cov["lines"]["pct"],     2), "direction": "higher_is_better"}
    metrics["frontend_coverage_branch"] = {"value": round(cov["branches"]["pct"],  2), "direction": "higher_is_better"}
    metrics["frontend_coverage_func"]   = {"value": round(cov["functions"]["pct"], 2), "direction": "higher_is_better"}

out = Path("$OUT_DIR") / "frontend.json"
out.write_text(json.dumps(metrics, indent=2, sort_keys=True))
print(f"Wrote {out}")
PY

# ESLint deve falhar build se houver erros (lint-staged já cobre pre-commit)
if grep -q '"errorCount":[1-9]' "$ESLINT_REPORT"; then
    echo "ESLint encontrou errors — falhando."
    exit 1
fi
