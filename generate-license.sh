#!/usr/bin/env bash
# Génère une clé de licence signée pour un client, à partir de LICENSE_SIGNING_KEY
# défini dans .env (ce doit être la clé réellement configurée sur le serveur du client).
#
# Usage: ./generate-license.sh "Nom Du Restaurant" 2026-09-10
set -euo pipefail
cd "$(dirname "$0")"

if [ $# -ne 2 ]; then
    echo "Usage: $0 \"Nom Du Restaurant\" YYYY-MM-DD" >&2
    exit 1
fi

if [ ! -f .env ]; then
    echo "Aucun .env trouvé dans $(pwd)." >&2
    exit 1
fi

KEY=$(grep -E '^LICENSE_SIGNING_KEY=' .env | cut -d= -f2-)
if [ -z "$KEY" ]; then
    echo "LICENSE_SIGNING_KEY introuvable dans .env." >&2
    exit 1
fi

command -v mvn >/dev/null 2>&1 || export PATH="/opt/apache-maven-3.6.3/bin:$PATH"

mvn -q exec:java \
    -Dexec.mainClass=com.monokek.licensing.LicenseKeyGeneratorTool \
    -Dexec.classpathScope=runtime \
    -Dexec.args="$KEY \"$1\" $2"
