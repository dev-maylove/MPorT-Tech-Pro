# MPorT Tech Pro — Deep Runtime Audit V5

Baseline: project configuration and workflows were preserved because the supplied baseline already builds successfully.

## 1. DNS cancellation
`InetAddress.getAllByName()` is a blocking platform resolver. The previous implementation ran it directly on `Dispatchers.IO`, where a slow resolver could consume a shared IO worker and coroutine cancellation did not make the caller responsive.

Fixed:
- DNS work is isolated in a dedicated daemon executor.
- The coroutine uses `suspendCancellableCoroutine`.
- Cancellation cancels/interupts the submitted Future and immediately stops waiting for the result.
- `CancellationException` is rethrown rather than converted into a DNS error.

Limitation: Android's underlying resolver is platform code and may ignore interruption. No source-only change can guarantee termination of an in-flight system DNS call. The fix prevents it from blocking the app's shared IO dispatcher and makes the UI coroutine cancellable.

## 2. Process death / state restoration
Persisted user-entered inputs for Ping, Traceroute and DNS with `rememberSaveable`. Transient running/results state remains non-persistent deliberately: restoring a half-completed network operation after process death would be misleading and unsafe.

## 3. Release/R8 static audit
Existing rules were retained. They are intentionally conservative and cover Room, Hilt, Retrofit/Gson, Security Crypto and app network features. No speculative shrinking rule was removed because the project is already build-successful and source-only analysis cannot prove a release-only R8 failure.

## 4. Findings requiring real release/device tests
- Verify minified release APK/AAB on device.
- Verify DNS behavior on Android 13–16 and vendor ROMs.
- Verify process recreation via Developer Options / `adb shell am kill`.
- Verify MikroTik API against actual RouterOS devices.
- Verify speed-test cancellation under a deliberately stalled network.
