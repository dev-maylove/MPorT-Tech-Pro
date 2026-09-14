# MPorT Tech Pro — Deep Runtime Audit V2

Baseline: the supplied project is already known to build successfully. No GitHub Actions or dependency versions were changed.

## Findings fixed

### 1. Room migration reliability
`MIGRATION_1_2` previously swallowed every exception around `ALTER TABLE`. That could hide malformed SQL, table-name mistakes, or storage errors and leave a partially migrated schema. The migration now checks `PRAGMA table_info(table)` and only adds a missing column. Real SQL failures are no longer silently suppressed.

### 2. Coroutine cancellation in DashboardViewModel
The monitoring loop and manual refresh caught broad `Exception`. Explicit `CancellationException` propagation was added so lifecycle cancellation is never converted into UI error handling.

## Audited areas
- Compose lifecycle / effects / remembered coroutine scopes: no `GlobalScope` found.
- Hilt: `@HiltViewModel` and singleton database provisioning inspected; no compile-risk change made.
- Room: version 2 migration path inspected and hardened.
- Android 13–16: manifest declares `NEARBY_WIFI_DEVICES`, Wi-Fi analyzer uses API 33 receiver flags, and UI requests Wi-Fi/location permissions where needed.
- Wi-Fi receiver: scan wait is already bounded with `withTimeoutOrNull`; receiver unregisters on result and cancellation.
- Navigation: back actions generally guard empty previous entries where custom screens need it; no workflow/build changes made.
- R8: rules are conservative and heavily keep reflection-sensitive packages. No speculative shrinking-rule changes made.

## Remaining validation that requires a real device/CI log
1. Run debug and minified release builds.
2. Exercise Wi-Fi scan after denying and then granting permissions.
3. Upgrade an installed v1 database to v2 and verify customers/tickets are preserved.
4. Test process recreation and back navigation on Android 13, 14, 15 and 16.
5. Verify minified release features: Room, Gson DTO parsing, Hilt ViewModels and MikroTik/network tools.
