#!/usr/bin/env bash
# Fetch OkHttp SPKI certificate pins for a host (leaf + chain).
# Usage: ./scripts/fetch-cert-pins.sh api.mandalanet.id
set -euo pipefail

HOST="${1:-api.mandalanet.id}"
PORT="${2:-443}"

echo "=== Certificate SPKI pins for ${HOST}:${PORT} ==="
echo "Paste these into CertificatePinning.HOST_PINS as sha256/<value>"
echo

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Capture chain (may need -showcerts)
echo | openssl s_client -connect "${HOST}:${PORT}" -servername "${HOST}" -showcerts 2>/dev/null \
  | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/{print}' > "${TMP}/chain.pem" || true

if [[ ! -s "${TMP}/chain.pem" ]]; then
  echo "ERROR: could not fetch certificates from ${HOST}:${PORT}" >&2
  exit 1
fi

# Split into individual certs
csplit -s -f "${TMP}/cert-" -b '%02d.pem' "${TMP}/chain.pem" '/-----BEGIN CERTIFICATE-----/' '{*}' 2>/dev/null || true

i=0
for f in "${TMP}"/cert-*.pem; do
  [[ -f "$f" ]] || continue
  grep -q "BEGIN CERTIFICATE" "$f" || continue
  SUBJ="$(openssl x509 -in "$f" -noout -subject 2>/dev/null | sed 's/^subject=//')"
  ISSUER="$(openssl x509 -in "$f" -noout -issuer 2>/dev/null | sed 's/^issuer=//')"
  PIN="$(openssl x509 -in "$f" -pubkey -noout 2>/dev/null \
    | openssl pkey -pubin -outform der 2>/dev/null \
    | openssl dgst -sha256 -binary \
    | base64)"
  echo "--- cert[$i] ---"
  echo "Subject : $SUBJ"
  echo "Issuer  : $ISSUER"
  echo "Pin     : sha256/${PIN}"
  echo
  i=$((i + 1))
done

if [[ "$i" -eq 0 ]]; then
  echo "ERROR: no certificates parsed" >&2
  exit 1
fi

echo "Kotlin snippet:"
echo "\"${HOST}\" to listOf("
for f in "${TMP}"/cert-*.pem; do
  [[ -f "$f" ]] || continue
  grep -q "BEGIN CERTIFICATE" "$f" || continue
  PIN="$(openssl x509 -in "$f" -pubkey -noout 2>/dev/null \
    | openssl pkey -pubin -outform der 2>/dev/null \
    | openssl dgst -sha256 -binary \
    | base64)"
  echo "    \"sha256/${PIN}\","
done
echo "),"
