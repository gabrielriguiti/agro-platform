#!/usr/bin/env bash
# Exporta o contrato OpenAPI da API em execução para packages/api-client/openapi.json.
# Uso: suba a API (ex.: profile local, porta 8080) e rode: scripts/export-openapi.sh [url-base]
set -euo pipefail
BASE="${1:-http://localhost:8080}"
OUT="$(dirname "$0")/../packages/api-client/openapi.json"
curl -fsS "$BASE/v3/api-docs" | python3 -c '
import json, sys
doc = json.load(sys.stdin)
doc.pop("servers", None)  # a URL do servidor varia por ambiente
json.dump(doc, sys.stdout, indent=2, ensure_ascii=False)
print()
' > "$OUT"
echo "Contrato exportado para $OUT"
