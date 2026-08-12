
#!/usr/bin/env python3
"""
Génère une clé de licence signée (HS256), sans dépendre de Maven/Java —
juste Python 3 stdlib. Reproduit exactement le JWT que LicenseKeyGeneratorTool
(io.jsonwebtoken) produit, vérifiable par com.monokek.licensing.LicenseService
côté serveur (utile pour un déploiement Docker où Maven n'est pas installé).

Usage:
    ./generate-license.py "Nom Du Restaurant" 2026-09-10
    ./generate-license.py "Nom Du Restaurant" 2026-09-10 --key MA_CLE_DE_SIGNATURE

Sans --key, lit LICENSE_SIGNING_KEY depuis le fichier .env à côté de ce script
(doit être la clé réellement configurée sur le serveur du client).
"""
import argparse
import base64
import hashlib
import hmac
import json
import sys
import time
from datetime import datetime, timezone
from pathlib import Path


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def read_signing_key_from_env() -> str:
    env_path = Path(__file__).resolve().parent / ".env"
    if not env_path.exists():
        sys.exit(f"Aucun .env trouvé ({env_path}). Utilise --key pour passer la clé directement.")
    for line in env_path.read_text().splitlines():
        line = line.strip()
        if line.startswith("LICENSE_SIGNING_KEY="):
            return line.split("=", 1)[1].strip()
    sys.exit("LICENSE_SIGNING_KEY introuvable dans .env.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("name", help='Nom du restaurant / client, ex: "Le Bon Coin"')
    parser.add_argument("expiry", help="Date d'expiration, format YYYY-MM-DD")
    parser.add_argument("--key", help="Clé de signature (sinon lue depuis .env)")
    args = parser.parse_args()

    signing_key = args.key or read_signing_key_from_env()

    expiry_dt = datetime.strptime(args.expiry, "%Y-%m-%d").replace(tzinfo=timezone.utc)
    now = int(time.time())
    exp = int(expiry_dt.timestamp())

    header = {"alg": "HS256"}
    payload = {"sub": args.name, "iat": now, "exp": exp}

    header_b64 = b64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    payload_b64 = b64url(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    signing_input = f"{header_b64}.{payload_b64}".encode("ascii")

    signature = hmac.new(signing_key.encode("utf-8"), signing_input, hashlib.sha256).digest()
    signature_b64 = b64url(signature)

    print(f"{header_b64}.{payload_b64}.{signature_b64}")


if __name__ == "__main__":
    main()
