# Certificate Pinning (MPorT Tech Pro)

OkHttp **SPKI SHA-256** pinning protects the API client against rogue CAs / MITM when enabled.

## Architecture

| Piece | Role |
|-------|------|
| `CertificatePinning.kt` | Host → pin list; builds `CertificatePinner` |
| `NetworkModule` | Applies pinner to OkHttp when configured |
| `BuildConfig.ENABLE_CERT_PINNING` | Feature flag (Gradle / CI) |
| `scripts/fetch-cert-pins.sh` | Generates pins from live TLS certs |

## Enable (production)

1. **Generate pins** (machine with network access to the API):
   ```bash
   ./scripts/fetch-cert-pins.sh api.mandalanet.id
   ```
2. **Paste** leaf + at least one backup (intermediate) into
   `app/.../core/security/CertificatePinning.kt` → `HOST_PINS`.
3. **Build with flag**:
   ```bash
   ./gradlew :app:assembleRelease -PenableCertPinning=true -PapiBaseUrl=https://api.mandalanet.id/
   ```
   Or set in CI workflow:
   ```yaml
   -PenableCertPinning=true
   ```
4. **Smoke-test** login / API calls on a real device. Wrong pins → SSL handshake failures.

## Rules of thumb

- Always pin **≥ 2** certificates (leaf + intermediate or next leaf).
- Rotate pins **before** the old leaf expires; ship an app update first.
- Keep **debug builds unpinned** (default) so local HTTP LAN backends still work.
- Do **not** enable the flag while `HOST_PINS` is still empty/placeholder — pinning is skipped until real `sha256/...` values exist (`CertificatePinning.isConfigured()`).

## Disable

Omit `-PenableCertPinning` or set `false`. Debug variant always forces `ENABLE_CERT_PINNING=false`.
