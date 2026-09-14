# Phase 9 — Deep Runtime Audit V4

Baseline: project supplied by the user had already built successfully. Gradle versions and successful GitHub Actions workflow versions were not downgraded or changed.

## Confirmed fix: MikroTik RouterOS API
`RouterSession` was a placeholder that marked a router as connected without opening a network connection or authenticating.

It now:
- validates host and TCP port;
- opens a TCP socket with an 8 second connect timeout;
- applies an 8 second read timeout;
- implements RouterOS API sentence framing and variable-length word encoding;
- authenticates using `/login` with username/password for current RouterOS API servers;
- reads `/system/identity/print` and `/system/resource/print`;
- reports identity, board/platform, version and uptime;
- clears in-memory session state when connection/authentication fails;
- closes input/output streams and socket in all paths.

### Important compatibility note
This implementation targets the current plain RouterOS API protocol on TCP 8728. It does not claim TLS/API-SSL support (8729), certificate validation configuration, or legacy challenge-response login. A router requiring one of those modes will return a connection/authentication failure rather than being falsely reported as connected.

## DNS timeout/cancellation
The source-level audit confirmed Android `InetAddress.getAllByName()` is a blocking system resolver call. A coroutine timeout around it does not reliably cancel the underlying resolver thread. No speculative replacement was made in V4 because a real cancellable DNS client would require a deliberate resolver implementation and test coverage.

## Network scanner concurrency
V3's single active scan job remains the relevant fix. No second concurrent scan path was introduced.

## Speed test cancellation
The engine already closes most streams/connections and checks an atomic cancellation flag, but blocking `HttpURLConnection` reads can only react at read boundaries/timeouts. V4 does not claim instant cancellation without a structural engine redesign and runtime test.

## Login/session consistency
Existing session persistence already strips passwords from the active session. No mass rewrite was made without a reproducible runtime defect.

## Process death / Compose state restoration
No project-wide refactor was applied without a reproducible state-loss crash. This remains a device/runtime test area.

## Release/R8
No speculative ProGuard changes were made. Release-only behavior must be validated from an actual minified release build and stack trace.
