# Release checklist — MPorT Tech Pro

## 1. Pre-build
- [ ] Version label set (workflow input or tag `vX.Y.Z`)
- [ ] Production API base URL confirmed (`https://api.mandalanet.id/` unless overridden)
- [ ] Keystore secrets present in GitHub Actions (not in git)
- [ ] `ENABLE_CERT_PINNING` left false unless real pins configured

## 2. CI build
- [ ] Run **workflow_dispatch** on `release.yml` with version + API URL
- [ ] Job `assembleRelease` succeeds
- [ ] Signed APK/AAB artifact uploaded

## 3. Smoke test on device/emulator (release build)
- [ ] Cold start → splash → login / guest
- [ ] Dashboard live link + device counts
- [ ] Network Monitor tabs (overview / devices / graph chart)
- [ ] Speed Test full run + history from store
- [ ] WiFi Analyzer: permission prompt → scan → channel bars
- [ ] Alerts list real offline devices → detail carries selected alert
- [ ] Jobs shows Room tickets (empty state OK if DB empty)
- [ ] Tickets create/sync if staff
- [ ] Profile logout uses scoped coroutine (no crash)
- [ ] Bottom nav Alerts badge updates after scan

## 4. ProGuard / R8
- [ ] No missing class crashes on login or Retrofit
- [ ] Room entities readable
- [ ] Gson DTOs intact

## 5. Do not change
- `usesCleartextTraffic` left as-is per product decision (option 13)
