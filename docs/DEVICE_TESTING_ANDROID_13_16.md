# MPorT Tech Pro — Android 13–16 Device Test Checklist

## Permissions
- Fresh install and deny notification/Wi-Fi/location permissions.
- Retry each affected feature.
- Permanently deny, grant from Settings, return to the app.
- Verify no crash when a service is unavailable.

## Network stress
- Start/cancel Network Scanner repeatedly.
- Start/cancel DNS repeatedly with slow or invalid hosts.
- Cancel Speed Test during download and upload.
- Background and foreground the app during every operation.

## MikroTik
- Offline IP
- Closed API port
- Invalid username/password
- Valid plain API connection on port 8728
- Verify failure never leaves a false Connected state.

## Process death
- Enter Ping/DNS/Traceroute input.
- Kill the app from system settings or developer options.
- Reopen and verify durable input behavior.
- Verify no socket/job/scan/speed test is restored as still running.

## Release
- Install the signed minified build.
- Repeat login, dashboard, Wi-Fi, diagnostics, scanner, speed test, and MikroTik smoke tests.
