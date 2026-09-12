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
