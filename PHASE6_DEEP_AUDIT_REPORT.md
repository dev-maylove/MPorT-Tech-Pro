# MPorT Tech Pro — Deep Audit / Phase 6

Baseline: user-confirmed original project builds successfully. No workflow versions were changed.

## Confirmed fixes
1. **Release cleartext HTTP exposure**: main manifest/config now fail closed; HTTP LAN exceptions are debug-source-set only.
2. **Potential NPE**: removed `pingResult!!` in ToolScreens.
3. **Wi-Fi connector logic**: secured SSIDs no longer claim unsupported password-sheet behavior; handoff to system Wi-Fi settings. Open-network request no longer binds the entire app process to a local-only network.
4. **Session credential exposure**: local session JSON no longer stores passwords; technician passwords are salted SHA-256 for newly written records. Existing plaintext records remain readable for migration compatibility.
5. **Demo admin exposure**: `admin/admin123` is accepted only when `BuildConfig.ALLOW_OFFLINE_DEMO_LOGIN` is true (debug), never in release.

## High-risk items inspected but intentionally not changed without runtime evidence
- Hilt/KSP/Room versions: project already builds successfully.
- GitHub Actions versions: left untouched.
- Lint policy: left unchanged to avoid converting a successful baseline into a CI-breaking change.
- R8 rules: inspected; no speculative dependency/version changes made.

## Remaining test recommendations
Run on device/emulator: Wi-Fi permissions on Android 13-16, secure/open Wi-Fi flow, release minification, login migration from an existing install, Room v1->v2 migration, process recreation/navigation.
