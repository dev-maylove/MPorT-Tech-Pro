# V10 Two Errors Fixed

## 1. Debug build manifest merger
Root cause: main manifest set `usesCleartextTraffic=false` while debug manifest set it to `true`, without an explicit merger override.

Fix:
- Added `xmlns:tools` in `app/src/debug/AndroidManifest.xml`.
- Added `tools:replace="android:usesCleartextTraffic"` to the debug application element.
- Added debug-only `res/xml/network_security_config.xml` with `cleartextTrafficPermitted="true"`.

Release keeps the main network-security policy with cleartext disabled.

## 2. Speed Test: `Ping timed out — server did not respond`
Root causes hardened:
- Selected server hostname was eagerly resolved to an IP, including a potential DNS operation on the UI thread.
- IP URLs plus a manually supplied Host header are less reliable for virtual-hosted speed-test servers.
- The engine only tried the configured ping path, even though compatible servers may not expose that exact path.
- HTTP speed-test servers require debug cleartext permission when using `http://`.

Fix:
- Keep the original hostname in the URL and let network DNS run during the actual background request.
- Removed eager DNS-to-IP conversion from server selection/probing.
- Added ping path fallbacks: configured path -> `/speedtest/latency.txt` -> `/`.
- Added debug-only cleartext network security configuration.

No Gradle, dependency, GitHub Actions, signing, Hilt/KSP, Room, or R8 configuration was changed.
