# MPorT Tech Pro — Deep Runtime Audit V3

Baseline: project was already BUILD SUCCESS. This audit intentionally avoids dependency/workflow churn.

## Fixed

### Network Scanner
- Added a single active `scanJob`; starting another scan cancels the previous scan.
- `quickScan()` no longer launches a nested scan job, reducing race conditions and stale UI updates.

### Room sync atomicity
- Added DAO `replaceAll()` methods annotated with `@Transaction`.
- Customer and ticket sync now replace caches atomically, avoiding an observable empty/partial state between `clear()` and `insertAll()`.

### Coroutine cancellation
- Repository sync/history operations now rethrow `CancellationException` instead of converting cancellation into an application error.
- MikroTik ViewModel also preserves cancellation semantics.

### Speed-test history race
- Added process-local synchronization around add/clear.
- Writes now use `commit()` so a later concurrent operation cannot be reordered behind an asynchronous `apply()` write.

### MikroTik input safety
- Port is validated to the legal TCP range 1..65535.
- Credentials are persisted only after `RouterSession.connect()` returns.

### Dashboard battery
- Added a small cooperative delay between monitoring cycles to avoid an unnecessarily tight polling loop.

## Important remaining limitation
`RouterSession` is still explicitly a placeholder and does not perform a real RouterOS API handshake. It therefore cannot prove router authentication/connectivity. This was not falsely “fixed” without implementing the RouterOS protocol.

## Not changed without proof
- GitHub Actions versions/workflows
- Gradle/dependency versions
- Broad Compose state restoration architecture
- R8 rules

Device testing is still recommended for Android Wi-Fi restrictions, process death, and release-only R8 behavior.
