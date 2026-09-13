# Development guide

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease   # requires keystore secrets
```

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md).

## Adding a feature (V2)

1. Domain model in `domain/model`
2. Repository interface in `domain/repository`
3. UseCase in `domain/usecase/...`
4. Impl in `data/repository`
5. Bind in `di/RepositoryModule`
6. Screen + ViewModel in `features/<name>/presentation`
7. Register route in `ui/navigation/AppNavigation` and `navigation/Screen.kt`
