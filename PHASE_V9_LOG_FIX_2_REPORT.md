# V9 Log-Driven Fix 2

## Root cause
GitHub Actions Release build failed at `:app:compileReleaseKotlin`.

`NetworkToolScreens.kt` used `rememberSaveable` without importing
`androidx.compose.runtime.saveable.rememberSaveable`.

This produced the primary compiler errors at lines 241, 242, 479, 480 and 679.
The later `it` and `@Composable invocations` errors were cascading type/lambda
errors caused by the unresolved `rememberSaveable` calls.

## Fix
Added:

```kotlin
import androidx.compose.runtime.saveable.rememberSaveable
```

No Gradle, dependency, workflow, signing, or R8 configuration was changed.
