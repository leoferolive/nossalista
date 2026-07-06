#!/usr/bin/env bash
set -euo pipefail
TMP=$(mktemp -d)
cat > "$TMP/cur.json" <<'EOF'
{
  "coverage_line":   {"value": 82.5, "direction": "higher_is_better"},
  "pmd_violations":  {"value": 3,    "direction": "lower_is_better"}
}
EOF
cat > "$TMP/base.json" <<'EOF'
{
  "coverage_line":   {"value": 80.0, "direction": "higher_is_better"},
  "pmd_violations":  {"value": 5,    "direction": "lower_is_better"}
}
EOF
python3 scripts/quality/ratchet.py check "$TMP/cur.json" "$TMP/base.json"
echo "OK: smoke test ratchet passou"
rm -rf "$TMP"
