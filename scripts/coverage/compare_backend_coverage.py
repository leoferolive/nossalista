#!/usr/bin/env python3

import sys
import defusedxml.ElementTree as element_tree


METRICS = ("LINE", "BRANCH")


def load_metrics(path: str) -> dict[str, float]:
    root = element_tree.parse(path).getroot()
    values: dict[str, float] = {}

    for metric in METRICS:
        counter = root.find(f"counter[@type='{metric}']")
        if counter is None:
            values[metric] = 0.0
            continue

        missed = int(counter.attrib.get("missed", "0"))
        covered = int(counter.attrib.get("covered", "0"))
        total = missed + covered
        values[metric] = (covered / total * 100.0) if total else 0.0

    return values


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: compare_backend_coverage.py <current-jacoco-xml> <base-jacoco-xml>")
        return 2

    current = load_metrics(sys.argv[1])
    base = load_metrics(sys.argv[2])

    failures = []
    for metric in METRICS:
        if current[metric] + 1e-9 < base[metric]:
            failures.append(
                f"{metric.lower()} coverage decreased: current={current[metric]:.2f}% base={base[metric]:.2f}%"
            )

    if failures:
        print("Backend coverage no-decrease gate failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("Backend coverage no-decrease gate passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
