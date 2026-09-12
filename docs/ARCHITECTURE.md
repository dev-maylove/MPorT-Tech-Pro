# MPorT Tech Pro — Full Core Architecture V2

## Status

Phase A foundation is in place. Existing feature screens continue to work via **bridge adapters** so the app keeps building while packages migrate.

## Package map (current → V2)

| Current | V2 target |
|---------|-----------|
| `core/database` | `core/database` ✓ |
| `features/tools/LiveNetworkInfo` | `core/network` + `data/repository` (bridged) |
| `features/speedtest/*` | `features/speedtest/{presentation,engine,server,history}` |
| `features/networktools/*` | `features/diagnostics/{ping,dns,traceroute}` |
| `features/discovery/*` | `features/network/scanner` + devices |
| `features/wifi/*` | `features/wifi/{information,analyzer,monitor}` |
| `ui/navigation` | `navigation/` (Screen.kt added) |
| `ui/theme` | `core/ui/theme` (alias later) |
| `ui/i18n` | `core/ui` or `features/settings` |

## Layers

```
Presentation (Compose Screen + ViewModel)
        ↓
Domain (UseCase)
        ↓
Domain Repository Interface
        ↓
Data Repository Implementation
        ↓
Data Source (Room / LiveNetworkInfo / Engine)
```

## Phase plan

### Phase A — Foundation (done in this pass)
- [x] `core/common` — Result, UiState, UiEvent, Constants, DispatcherProvider
- [x] `core/network` — NetworkState, NetworkInfoProvider
- [x] `domain/model` + `domain/repository` + sample use cases
- [x] `data/repository` bridge implementations
- [x] `di` — AppModule, RepositoryModule
- [x] `navigation/Screen.kt`
- [x] docs/ARCHITECTURE.md

### Phase B — Working features (done)
- [x] DashboardViewModel + GetNetworkInfoUseCase
- [x] NetworkScannerViewModel + ScanNetworkUseCase
- [x] Diagnostics package (ping/dns/traceroute) + PingViewModel
- [ ] Room entity for speed history (optional next)

### Phase C — Advanced (partial)
- [x] Network scanner presentation + ViewModel
- [x] Diagnostics V2 entry points
- [ ] Full engine move out of features/tools

### Phase D — MikroTik + Security (foundation)
- [x] SecureStorage (EncryptedSharedPreferences)
- [x] CredentialManager
- [x] MikroTikScreen + ViewModel + RouterSession
- [ ] Real RouterOS API binary protocol

## Rules

1. **Never break existing routes** while migrating — keep string routes until Screen.* replaces them.
2. New code prefers UseCase → Repository.
3. Feature engines stay free of Android UI types where possible.
4. Credentials only via encrypted storage (Phase D security).
