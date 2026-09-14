# MPorT Tech Pro — Deep Runtime Audit V6

Baseline: the supplied project is already known to build successfully. This audit intentionally avoids speculative Gradle/dependency/workflow changes.

## Scope
- unsafe `!!` and casts
- coroutine cancellation handling
- socket/stream lifecycle
- executor lifecycle
- SharedPreferences/session concurrency
- Android 13–16 permission denial paths
- Navigation back-stack duplication
- Compose stale state/recomposition risks
- Room edge cases
- network timeout consistency

## Fixed in V6
1. Replaced three framework service unsafe-cast paths with typed `getSystemService(Class)` access.
   - `LiveNetworkInfo.snapshot()` now returns an offline-safe `LiveLinkInfo` when a required framework service is unavailable instead of throwing a `ClassCastException`.
   - `DiscoveryEngine.signalFlow()` exits its flow cleanly if Wi-Fi service is unavailable.
   - `WifiAnalyzer` uses typed service lookup and fails with a clear invariant error rather than an unsafe cast.
2. Re-scanned Kotlin sources for executable `!!`; no source-level non-null assertion was found in the audited baseline.
3. Re-scanned network code for existing socket `.use`/disconnect patterns and did not apply broad refactors without reproducible evidence.

## Findings requiring device/release verification
- `catch (Exception)` exists in multiple UI/network helper paths. Not every instance is a coroutine cancellation bug; automatic mass insertion of `CancellationException` would be unsafe without compile/runtime context.
- `HttpURLConnection.read()` cancellation remains bounded by configured read timeout in some speed-test paths.
- Android Wi-Fi permission behavior depends on device/ROM/location/Wi-Fi state and must be tested on Android 13–16.
- Release-only R8 behavior cannot be proven by static source inspection alone; test a minified release APK.
- Process death requires device tests because transient sockets/jobs must not be restored as active.

## Recommended verification matrix
1. Android 13, 14, 15, 16: deny permissions, deny permanently, then grant from Settings.
2. Start/stop Network Scanner repeatedly; rotate and background the app.
3. Cancel Speed Test during DNS/connect/read/upload.
4. Test RouterOS wrong host, closed port, timeout, wrong credentials, success, reconnect.
5. Install minified release APK and exercise all network tools.
6. Kill process from developer options while Ping/DNS/Traceroute screens are open, then restore.
