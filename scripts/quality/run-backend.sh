#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
BACKEND_DIR="$REPO_ROOT/backend"
OUT_DIR="$REPO_ROOT/.quality-output"
mkdir -p "$OUT_DIR"

MODE="${1:-full}"   # full | pre-commit

cd "$BACKEND_DIR"

if [[ "$MODE" == "pre-commit" ]]; then
    # subset rápido — sem cobertura, só lint/static analysis
    ./mvnw -q -P strict-quality \
        checkstyle:check pmd:check spotbugs:check \
        -DskipTests -DskipITs
    exit 0
fi

# full: roda tudo (skip dep-check local — requer NVD API key, lento, roda no CI)
./mvnw -q -P strict-quality verify -DskipITs -Ddependency-check.skip=true

# O caminho do XML do JaCoCo depende da config do plugin no pom.xml; tentamos os dois locais comuns.
JACOCO_XML="$BACKEND_DIR/target/site/jacoco/jacoco.xml"
if [[ ! -f "$JACOCO_XML" ]]; then
    JACOCO_XML="$BACKEND_DIR/target/jacoco-report/jacoco.xml"
fi
PMD_XML="$BACKEND_DIR/target/pmd.xml"

python3 - <<PY
import json, sys
from pathlib import Path
import xml.etree.ElementTree as ET

jacoco_path = Path("$JACOCO_XML")
metrics = {}

if jacoco_path.exists():
    # O report do JaCoCo declara um DTD externo (report.dtd) que ET tenta buscar.
    # Removemos o DOCTYPE para parsing seguro sem rede.
    import re
    raw = jacoco_path.read_text(encoding="utf-8")
    raw = re.sub(r"<!DOCTYPE[^>]*>", "", raw, count=1)
    jacoco = ET.fromstring(raw)

    def counter(name):
        el = jacoco.find(f"counter[@type='{name}']")
        if el is None:
            return 0.0
        missed = int(el.attrib.get("missed", "0"))
        covered = int(el.attrib.get("covered", "0"))
        total = missed + covered
        return (covered / total * 100.0) if total else 0.0

    metrics["backend_coverage_line"]   = {"value": round(counter("LINE"),   2), "direction": "higher_is_better"}
    metrics["backend_coverage_branch"] = {"value": round(counter("BRANCH"), 2), "direction": "higher_is_better"}

pmd_violations = 0
pmd_path = Path("$PMD_XML")
if pmd_path.exists():
    pmd_root = ET.parse(str(pmd_path)).getroot()
    ns = {"p": "http://pmd.sourceforge.net/report/2.0.0"}
    pmd_violations = len(pmd_root.findall(".//p:violation", ns))
metrics["backend_pmd_violations"] = {"value": pmd_violations, "direction": "lower_is_better"}

out = Path("$OUT_DIR") / "backend.json"
out.write_text(json.dumps(metrics, indent=2, sort_keys=True))
print(f"Wrote {out}")
PY
