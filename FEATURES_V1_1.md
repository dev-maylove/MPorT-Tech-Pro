# MPorT Tech Pro V1.1 Network Tools

## WiFi Analyzer
Uses Android WifiManager scan results. Android 13+ may require NEARBY_WIFI_DEVICES and location-related permissions depending on API and device behavior.

## Authorized Network Scanner
Requires explicit `authorized=true`, accepts only RFC1918 private ranges, and scans a bounded host range. It is intended only for networks owned by or explicitly authorized for the technician.

## Speed Test
Measures download throughput against an HTTPS endpoint selected by the operator. No third-party speed-test credentials are embedded.
