#!/usr/bin/env bash
set -euo pipefail

GREEN="\033[0;32m"
RED="\033[0;31m"
RESET="\033[0m"

METRICS_FILE="$1"
BASELINE_FILE="$2"

python3 - "$METRICS_FILE" "$BASELINE_FILE" <<'PY'
import json, sys
from pathlib import Path

cur  = json.loads(Path(sys.argv[1]).read_text())
base_path = Path(sys.argv[2])
base_text = base_path.read_text() if base_path.exists() else ""
base = json.loads(base_text) if base_text.strip() else {}

G, R, Z = "\033[0;32m", "\033[0;31m", "\033[0m"
print(f"{'Métrica':<35} {'Atual':>10} {'Baseline':>10}  Status")
print("-" * 70)
any_fail = False
for name, meta in sorted(cur.items()):
    v = meta["value"]
    direction = meta.get("direction", "higher_is_better")
    b = base.get(name, {}).get("value", "—")
    if b == "—":
        status = f"{G}✓ novo{Z}"
    elif direction == "higher_is_better":
        ok = v + 1e-9 >= b
        status = f"{G}✓{Z}" if ok else f"{R}✗ regrediu{Z}"
        any_fail |= not ok
    else:
        ok = v <= b + 1e-9
        status = f"{G}✓{Z}" if ok else f"{R}✗ regrediu{Z}"
        any_fail |= not ok
    print(f"{name:<35} {str(v):>10} {str(b):>10}  {status}")
print("-" * 70)
sys.exit(1 if any_fail else 0)
PY
