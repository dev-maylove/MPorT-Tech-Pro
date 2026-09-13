# MPorT Tech Pro

**Architecture:** Full Core V2 (see `docs/ARCHITECTURE.md`).

# MPorT Tech Pro
Android Kotlin application for authorized WiFi/network technicians.

## Build

```bash
# Debug (always works, no keystore needed)
gradle :app:assembleDebug

# Signed release — keystore REQUIRED (unsigned release is not allowed)
# 1. cp keystore.properties.example keystore.properties
# 2. Fill KEYSTORE_PATH / passwords
# 3. Place the .keystore file (or use absolute path)
gradle :app:assembleRelease

# Optional: override version
gradle :app:assembleRelease -PversionName=1.2.3 -PversionCode=10203
```

> **Policy:** Release builds are **always signed**.  
> If keystore / credentials are missing, the build **fails** — no unsigned APK/AAB is produced.

## Signing Config

Priority order:

1. **Environment variables** (GitHub Actions)
2. **`keystore.properties`** in project root (local)
3. **Gradle properties** (`-PKEYSTORE_...`)

| Key                  | Alternative     |
|----------------------|-----------------|
| `KEYSTORE_PATH`      | `storeFile`     |
| `KEYSTORE_PASSWORD`  | `storePassword` |
| `KEY_ALIAS`          | `keyAlias`      |
| `KEY_PASSWORD`       | `keyPassword`   |

### Local

```bash
cp keystore.properties.example keystore.properties
# edit + put release.keystore
gradle :app:assembleRelease
# → MPorT-Tech-Pro-v1.0.0-release.apk (signed)
```

`keystore.properties`, `*.keystore`, `*.jks` are gitignored.

### GitHub Secrets (required for release workflow)

| Secret              | Description                        |
|---------------------|------------------------------------|
| `KEYSTORE_BASE64`   | `base64 -w0 release.keystore`      |
| `KEYSTORE_PASSWORD` | Keystore password                  |
| `KEY_ALIAS`         | Key alias                          |
| `KEY_PASSWORD`      | Key password                       |

Missing any of these → release job fails (no unsigned artifact).

### Workflows

| File | Trigger | Output |
|------|---------|--------|
| `android.yml` | push / PR to `main` | Debug APK |
| `release.yml` | tag `v*` or manual | **Signed** APK + AAB only |

### Auto version from tag

```bash
git tag v1.2.3 && git push origin v1.2.3
```

- `versionName` = `1.2.3`
- `versionCode` = `10203`
- Files: `MPorT-Tech-Pro-v1.2.3.apk` / `.aab`
- GitHub Release auto-created (prerelease if tag contains `-`)

## Security
Use network diagnostics only on networks and systems you own or are authorized to administer. MikroTik credentials should not be embedded in the APK.


## Language / Bahasa
In-app bilingual UI: **Indonesia** and **English**.

- Open **Profile** → **App language / Bahasa aplikasi**
- Choice is saved on device (SharedPreferences)
- Bottom navigation and main screens follow the selected language


## API Base URL (single source of truth)

| Build | Default URL | Override |
|-------|-------------|----------|
| **release** | `https://api.mandalanet.id/` | Gradle `-PapiBaseUrl=...` or CI input `api_base_url` |
| **debug** | `http://192.168.1.102:8000/` | Same `-PapiBaseUrl=...` if set |

Runtime code always reads **`BuildConfig.API_BASE_URL`** via `Constants.API_BASE_URL` (Retrofit in `NetworkModule`).  
Do not hardcode alternate bases in feature code.

### GitHub Actions release
Workflow: `.github/workflows/release.yml`  
Inputs: **Version label** (`version_name`), **Production API base URL** (`api_base_url`).

Required secrets (never commit keystores):
- `KEYSTORE_BASE64` or equivalent
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

## Certificate pinning (optional)

`BuildConfig.ENABLE_CERT_PINNING` (default **false**).  
When `true`, `NetworkModule` applies OkHttp `CertificatePinner` for the API host.  
**Before enabling in production**, replace the placeholder SHA-256 pins with real pins from your certificate chain.

## R8 / ProGuard (release)

- `minifyEnabled` on release uses `app/proguard-rules.pro`
- Keep rules cover Room, Retrofit/Gson DTOs, Hilt, Compose, speedtest, tools, wifi
- Smoke-test after each release APK: login, speed test, WiFi scan, tickets Room, navigation

## Signing — never commit secrets

`.gitignore` excludes: `keystore.properties`, `*.jks`, `*.keystore`, `*.p12`, `keystore/`  
Use `keystore.properties.example` as template only.

See also: `docs/RELEASE_CHECKLIST.md`

## Certificate Pinning

Optional OkHttp SPKI pinning — see [docs/CERT_PINNING.md](docs/CERT_PINNING.md).

```bash
./scripts/fetch-cert-pins.sh api.mandalanet.id
# paste pins into CertificatePinning.HOST_PINS
./gradlew :app:assembleRelease -PenableCertPinning=true
```
