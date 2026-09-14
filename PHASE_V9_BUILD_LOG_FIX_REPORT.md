# V9 Build Log Fix — WifiConnector.kt

## Root cause
GitHub Actions Release build failed at `:app:kspReleaseKotlin` because Kotlin reported:

`WifiConnector.kt:75:10 Unexpected tokens (use ';' to separate expressions on the same line)`

The source used an invalid trailing-lambda form for `Handler.postDelayed`:

```kotlin
mainHandler.postDelayed {
    ...
}, 45_000)
```

## Fix
Changed to the valid two-argument call:

```kotlin
mainHandler.postDelayed({
    ...
}, 45_000)
```

No Gradle, dependency, signing, R8/ProGuard, or GitHub Actions configuration was changed.
