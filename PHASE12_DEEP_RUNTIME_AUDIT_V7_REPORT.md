# MPorT Tech Pro — Phase 12: Deep Runtime Audit V7

## Baseline
The input project is treated as BUILD SUCCESS. Gradle versions, dependencies and the three working GitHub Actions workflows were not changed.

## Scope
- Network stress and repeated start/stop
- Scanner cancellation storms
- Speed test cancellation review
- MikroTik reconnect/timeout review
- DNS failure matrix
- Permission denial/recovery review
- Room empty/corrupt data review
- Background/foreground and process-death review
- Debug vs Release checklist

## Verified fixes

### 1. Scanner cancellation propagation
`NetworkRepositoryImpl` previously caught `Exception` around suspend work. In a cancellation path this can convert cooperative cancellation into `Result.Error`, leaving a cancelled scan reported as a normal scan failure.

Fix: `CancellationException` is now rethrown before normal error handling for both network-info and device-scan operations.

### 2. DNS cancellation storm hardening
The dedicated DNS executor introduced in V5 used an unbounded cached thread pool. Repeated DNS start/cancel operations while platform DNS blocks can create an unnecessary number of daemon resolver threads.

Fix: changed to a bounded fixed pool of 2 daemon threads. Cancellation still cancels the queued/running Future when possible. Android's platform resolver may ignore interruption while already blocked; this limitation is explicitly retained rather than falsely claiming instant cancellation.

## Failure matrix

| Feature | Stress/failure | Expected behavior |
|---|---|---|
| Network scanner | repeated Scan | previous ViewModel job is cancelled; latest request owns UI state |
| Network scanner | cancellation | cancellation propagates; it is not converted to `Result.Error` |
| DNS | repeated start/cancel | at most two resolver workers execute concurrently; queued work remains bounded by executor queue semantics |
| DNS | invalid/empty host | structured error/empty-query result, no crash |
| Speed test | cancel during blocking I/O | cancellation flag is checked; blocking reads remain timeout-bound |
| MikroTik | timeout/auth failure | no false connected state; session is cleared by V4 behavior |
| Room | empty data | repositories should expose empty lists rather than require non-empty rows |
| Room | corrupt DB | no destructive schema change was introduced without runtime evidence |
| Permissions | deny | feature should remain unavailable with UI feedback, not crash |
| Process death | active network task | transient task/socket state is not restored as if still running |
| Release | R8/minified | existing conservative rules retained; real minified build remains the final validation |

## Areas intentionally not changed
- SpeedTestEngine blocking I/O was not rewritten without benchmark/runtime logs.
- MikroTik protocol was not expanded to TLS/8729 without real router/certificate test data.
- Room corruption recovery was not made destructive (e.g. fallback-to-destructive-migration).
- Permission behavior was not rewritten because vendor/API behavior requires device testing.
- GitHub Actions were not touched.

## Remaining validation recommended
1. Stress-tap Scan 20–50 times.
2. Start/cancel DNS repeatedly on an unreachable resolver scenario.
3. Cancel Speed Test during both download and upload.
4. Test MikroTik wrong password, closed port and timeout.
5. Deny permissions, permanently deny, then grant from Settings.
6. Background/foreground the app during active scans.
7. Force-stop/process-death and reopen diagnostic screens.
8. Install a minified signed Release APK and exercise all network features.
