plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// ── Version (overridable from CI: -PversionName=1.2.3 -PversionCode=10203) ──
val appVersionName: String = (project.findProperty("versionName") as String?)
    ?.takeIf { it.isNotBlank() }
    ?: "1.0.0"
val appVersionCode: Int = (project.findProperty("versionCode") as String?)
    ?.toIntOrNull()
    ?: 1

// ── API base URL (CI: -PapiBaseUrl=https://api.mandalanet.id/) ──
// Trailing slash required for Retrofit.
fun normalizeApiBaseUrl(raw: String): String {
    val t = raw.trim()
    return if (t.endsWith("/")) t else "$t/"
}
val appApiBaseUrl: String = (project.findProperty("apiBaseUrl") as String?)
    ?.takeIf { it.isNotBlank() }
    ?.let { normalizeApiBaseUrl(it) }
    ?: "https://api.mandalanet.id/"

// ── Signing: env (CI) → keystore.properties (local) → gradle properties ──
// Parse keystore.properties without java.util.Properties (avoids Kotlin DSL unresolved ref)
val keystoreProps: Map<String, String> = run {
    val file = rootProject.file("keystore.properties")
    if (!file.exists()) return@run emptyMap()
    file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val idx = line.indexOf('=')
            line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        }
}

fun resolveSigningValue(key: String): String? {
    System.getenv(key)?.takeIf { it.isNotBlank() }?.let { return it }
    keystoreProps[key]?.takeIf { it.isNotBlank() }?.let { return it }
    return (project.findProperty(key) as? String)?.takeIf { it.isNotBlank() }
}

// Detect pure-debug / meta tasks so debug CI works without keystore.
// Tasks like "assemble", "build", "assembleRelease", "bundleRelease" REQUIRE signing.
val taskNamesLower = gradle.startParameter.taskNames.map { it.lowercase() }
// Empty task list = IDE sync / configuration only → do not require keystore
val isPureDebugOrMeta = taskNamesLower.isEmpty() || taskNamesLower.all { t ->
    t.contains("debug") ||
        t == "tasks" || t == "help" || t == "properties" ||
        t.contains("dependencies") || t.contains("signingreport") ||
        t.endsWith(":help")
}
val requiresReleaseSigning = !isPureDebugOrMeta


// ── Certificate pinning (CI: -PenableCertPinning=true) ──
// Requires real SPKI pins in CertificatePinning.HOST_PINS before enabling.
// Cert pinning disabled for now — set true later when real SPKI pins are in CertificatePinning.HOST_PINS
val appEnableCertPinning: Boolean = false
// (project.findProperty("enableCertPinning") as String?)?.equals("true", ignoreCase = true) == true

android {
    namespace = "com.mporttech.pro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mporttech.pro"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        // API base — overridable via -PapiBaseUrl=... (CI workflow)
        buildConfigField("String", "API_BASE_URL", "\"${appApiBaseUrl}\"")
        buildConfigField("boolean", "ALLOW_OFFLINE_DEMO_LOGIN", "false")
        buildConfigField("boolean", "ENABLE_CERT_PINNING", "false")
    }

    // AGP 8+ recommended way for output name prefix

    signingConfigs {
        create("release") {
            val storeFilePath = resolveSigningValue("KEYSTORE_PATH")
                ?: resolveSigningValue("storeFile")
            val storePass = resolveSigningValue("KEYSTORE_PASSWORD")
                ?: resolveSigningValue("storePassword")
            val alias = resolveSigningValue("KEY_ALIAS")
                ?: resolveSigningValue("keyAlias")
            val keyPass = resolveSigningValue("KEY_PASSWORD")
                ?: resolveSigningValue("keyPassword")

            if (requiresReleaseSigning) {
                // Release MUST be signed — fail fast, never produce unsigned APK/AAB
                require(!storeFilePath.isNullOrBlank()) {
                    "Release signing requires KEYSTORE_PATH (or storeFile).\n" +
                        "Set env var, keystore.properties, or -PKEYSTORE_PATH=..."
                }
                require(!storePass.isNullOrBlank()) {
                    "Release signing requires KEYSTORE_PASSWORD (or storePassword)."
                }
                require(!alias.isNullOrBlank()) {
                    "Release signing requires KEY_ALIAS (or keyAlias)."
                }
                require(!keyPass.isNullOrBlank()) {
                    "Release signing requires KEY_PASSWORD (or keyPassword)."
                }

                val path: String = storeFilePath
                val keystoreFile = file(path)
                val resolved = if (keystoreFile.exists()) {
                    keystoreFile
                } else {
                    rootProject.file(path)
                }
                require(resolved.exists()) {
                    "Keystore file not found: $path\n" +
                        "  tried: ${keystoreFile.absolutePath}\n" +
                        "  tried: ${rootProject.file(path).absolutePath}"
                }

                storeFile = resolved
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            } else if (
                !storeFilePath.isNullOrBlank() &&
                !storePass.isNullOrBlank() &&
                !alias.isNullOrBlank() &&
                !keyPass.isNullOrBlank()
            ) {
                val path: String = storeFilePath
                val keystoreFile = file(path)
                val resolved = if (keystoreFile.exists()) keystoreFile else rootProject.file(path)
                if (resolved.exists()) {
                    storeFile = resolved
                    storePassword = storePass
                    keyAlias = alias
                    keyPassword = keyPass
                }
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            isDebuggable = true
            // Local LAN default; CI can still override via -PapiBaseUrl
            buildConfigField(
                "String",
                "API_BASE_URL",
                if (project.hasProperty("apiBaseUrl")) "\"${appApiBaseUrl}\"" else "\"http://192.168.1.102:8000/\""
            )
            buildConfigField("boolean", "ALLOW_OFFLINE_DEMO_LOGIN", "true")
            buildConfigField("boolean", "ENABLE_CERT_PINNING", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("String", "API_BASE_URL", "\"${appApiBaseUrl}\"")
            buildConfigField("boolean", "ALLOW_OFFLINE_DEMO_LOGIN", "false")
            // Enable when production cert pins are configured in NetworkModule
            buildConfigField("boolean", "ENABLE_CERT_PINNING", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        // Don't fail CI on residual lint; still run checks for visibility
        abortOnError = false
        checkReleaseBuilds = false
        warningsAsErrors = false
        // Runtime permission checks exist; WifiManager APIs still trip this lint
        disable += "MissingPermission"
        disable += "ObsoleteSdkInt"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Satisfy R8 refs from Tink used by security-crypto
    compileOnly("com.google.errorprone:error_prone_annotations:2.28.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
