#!/usr/bin/env python3
"""Compara métricas atuais com a baseline armazenada.

Uso:
  ratchet.py check <metrics.json> <baseline.json>
  ratchet.py update <metrics.json> <baseline.json>

Retorna 0 se todas as métricas atendem ou superam a baseline,
1 se alguma regrediu, 2 em erro de uso.

Cada métrica é declarada com direção ('higher_is_better' | 'lower_is_better').
"""
import json
import sys
from pathlib import Path


def load(path: Path) -> dict:
    if not path.exists():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def check(current: dict, baseline: dict) -> list[str]:
    failures: list[str] = []
    for name, meta in current.items():
        if name not in baseline:
            continue  # nova métrica, será incorporada no próximo update
        cur = meta["value"]
        base = baseline[name]["value"]
        direction = meta.get("direction", "higher_is_better")
        if direction == "higher_is_better" and cur + 1e-9 < base:
            failures.append(f"{name}: {cur} < baseline {base}")
        elif direction == "lower_is_better" and cur > base + 1e-9:
            failures.append(f"{name}: {cur} > baseline {base}")
    return failures


def main() -> int:
    if len(sys.argv) != 4:
        print(__doc__)
        return 2
    mode, metrics_path, baseline_path = sys.argv[1], Path(sys.argv[2]), Path(sys.argv[3])
    current = load(metrics_path)
    baseline = load(baseline_path)

    if mode == "update":
        baseline_path.parent.mkdir(parents=True, exist_ok=True)
        baseline_path.write_text(json.dumps(current, indent=2, sort_keys=True), encoding="utf-8")
        print(f"Baseline atualizada: {baseline_path}")
        return 0

    if mode == "check":
        if not baseline:
            print(f"Baseline ausente em {baseline_path} — rodando como primeira execução, gravando.")
            baseline_path.parent.mkdir(parents=True, exist_ok=True)
            baseline_path.write_text(json.dumps(current, indent=2, sort_keys=True), encoding="utf-8")
            return 0
        failures = check(current, baseline)
        if failures:
            print("Regressões detectadas:")
            for f in failures:
                print(f"  - {f}")
            return 1
        return 0

    print(f"Modo desconhecido: {mode}")
    return 2


if __name__ == "__main__":
    sys.exit(main())
