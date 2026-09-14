# MPorT Tech Pro — Phase 13 / Deep Runtime Audit V8

## Baseline
The input project was already reported by the user as BUILD SUCCESS. This phase does not downgrade Gradle, dependencies, AGP, Kotlin, or working GitHub Actions versions.

## Static test suite additions
A lightweight JVM unit-test baseline was added for deterministic, Android-independent network parsing:
- invalid/duplicate port list normalization
- timeout-style ping output parsing

Existing `testImplementation("junit:junit:4.13.2")` is retained; no speculative test framework migration was made.

## Scanner cancellation
Previous cancellation fixes are retained. A new scan must cancel the prior job, and repository cancellation must be rethrown rather than converted to a normal error.

## DNS failure handling
The bounded two-thread resolver from V7 is retained. Empty queries return a controlled result. `CancellationException` is rethrown. Platform DNS is isolated because `InetAddress` can remain blocking after interruption.

## Room transactions
`replaceAll()` remains atomic for customer and ticket synchronization. Empty replacement remains valid and does not require destructive migration.

## Session/login
No password is written into the active session JSON. Debug-only demo login remains guarded by `BuildConfig.ALLOW_OFFLINE_DEMO_LOGIN`; release remains false.

## RouterOS protocol
Static review confirms bounded word size, socket/read/connect timeouts, stream `.use`, and socket closure. **Bug fixed in V8:** `RouterSession.connect()` previously caught `Exception`, which could swallow coroutine cancellation and reset state as if it were an I/O failure. `CancellationException` is now rethrown before normal failure handling. The `finally` block still closes the socket.

## ViewModel/Compose state
No mass state refactor was made. Input state restored by V5 remains limited to durable user input; sockets, jobs, and in-flight operations are not restored after process death.

## Release readiness
Static review only:
- release minification/shrinking enabled
- release signing required by current task logic
- release demo login disabled
- certificate pinning remains disabled until real SPKI pins exist
- existing ProGuard rules preserved to avoid speculative release-only regressions

## Android 13–16 device checklist
1. Fresh install: deny each permission.
2. Retry feature after denial.
3. Permanently deny and grant from Settings, then return to app.
4. Wi-Fi scan with Wi-Fi off/on and Location state variations.
5. Repeated scanner start/stop.
6. DNS start/cancel stress.
7. Speed test cancel during download and upload.
8. MikroTik: offline host, closed port, wrong credentials, valid RouterOS API.
9. Background/foreground during every long operation.
10. Force-stop/process recreation and verify no false "running" state.
11. Install minified signed Release and repeat core tests.

## Recommended real commands
Run in the environment that already builds this project:
- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`
- `./gradlew assembleDebug`
- signed Release task used by the existing successful CI

## Scope limitations
No real Android device, RouterOS router, or signed minified APK was available in this audit environment. Therefore this report does not claim runtime/device/Release success; it records static findings and source changes only.
