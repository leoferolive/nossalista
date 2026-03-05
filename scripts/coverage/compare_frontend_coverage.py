#!/usr/bin/env python3

import json
import sys


METRICS = ("lines", "branches", "functions", "statements")


def load_metrics(path: str) -> dict[str, float]:
    with open(path, encoding="utf-8") as handle:
        data = json.load(handle)
    totals = data.get("total", {})
    values = {}
    for metric in METRICS:
        values[metric] = float(totals.get(metric, {}).get("pct", 0.0))
    return values


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: compare_frontend_coverage.py <current-summary-json> <base-summary-json>")
        return 2

    current = load_metrics(sys.argv[1])
    base = load_metrics(sys.argv[2])

    failures = []
    for metric in METRICS:
        if current[metric] + 1e-9 < base[metric]:
            failures.append(
                f"{metric} coverage decreased: current={current[metric]:.2f}% base={base[metric]:.2f}%"
            )

    if failures:
        print("Frontend coverage no-decrease gate failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("Frontend coverage no-decrease gate passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
