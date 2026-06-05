#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
BASELINE_DIR="$REPO_ROOT/.quality-baseline"
OUT_DIR="$REPO_ROOT/.quality-output"

MODE="full"
UPDATE_BASELINE=0
for arg in "$@"; do
    case "$arg" in
        --pre-commit) MODE="pre-commit" ;;
        --full)       MODE="full" ;;
        --update-baseline) UPDATE_BASELINE=1 ;;
        -h|--help)
            cat <<USAGE
Uso: ./scripts/quality.sh [--pre-commit | --full] [--update-baseline]

  --pre-commit       subset rápido (≤ 30s alvo); lint + typecheck + static analysis
  --full             roda tudo (default); cobertura + ratchet
  --update-baseline  grava a baseline atual após um --full bem-sucedido
USAGE
            exit 0 ;;
        *) echo "Arg desconhecido: $arg"; exit 2 ;;
    esac
done

FAILED=0

echo "▶ Backend ($MODE)…"
if ! "$SCRIPT_DIR/quality/run-backend.sh" "$MODE"; then
    # Em pre-commit, falha do runner é fatal imediatamente (gate rápido).
    # Em full, deixamos o ratchet/render mostrar quais métricas regrediram.
    if [[ "$MODE" == "pre-commit" ]]; then
        exit 1
    fi
    FAILED=1
fi
echo "▶ Frontend ($MODE)…"
if ! "$SCRIPT_DIR/quality/run-frontend.sh" "$MODE"; then
    if [[ "$MODE" == "pre-commit" ]]; then
        exit 1
    fi
    FAILED=1
fi

if [[ "$MODE" == "pre-commit" ]]; then
    echo "✓ pre-commit gate ok"
    exit 0
fi

# Full: ratchet
for stack in backend frontend; do
    METRICS="$OUT_DIR/$stack.json"
    BASELINE="$BASELINE_DIR/$stack.json"

    echo
    echo "── $stack ──"
    "$SCRIPT_DIR/quality/render-table.sh" "$METRICS" "$BASELINE" || FAILED=1

    if [[ $UPDATE_BASELINE -eq 1 ]]; then
        python3 "$SCRIPT_DIR/quality/ratchet.py" update "$METRICS" "$BASELINE"
    else
        python3 "$SCRIPT_DIR/quality/ratchet.py" check "$METRICS" "$BASELINE" || FAILED=1
    fi
done

exit $FAILED
