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

// ── Signing: env (CI) → keystore.properties (local) → gradle properties ──
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = java.util.Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun resolveSigningValue(key: String): String? =
    System.getenv(key)
        ?: keystoreProperties.getProperty(key)
        ?: (project.findProperty(key) as String?)
            ?.takeIf { it.isNotBlank() }

// Detect whether any release task is requested (so debug builds still work without keystore)
val isReleaseTask = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true) || it.contains("Bundle", ignoreCase = true)
}

android {
    namespace = "com.mporttech.pro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mporttech.pro"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        // Output: MPorT-Tech-Pro-v1.2.3-release.apk / .aab
        setProperty("archivesBaseName", "MPorT-Tech-Pro-v${appVersionName}")
    }

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

            if (isReleaseTask) {
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

                val keystoreFile = file(storeFilePath!!)
                val resolved = if (keystoreFile.exists()) {
                    keystoreFile
                } else {
                    rootProject.file(storeFilePath)
                }
                require(resolved.exists()) {
                    "Keystore file not found: $storeFilePath\n" +
                        "  tried: ${keystoreFile.absolutePath}\n" +
                        "  tried: ${rootProject.file(storeFilePath).absolutePath}"
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
                // Credentials present even on non-release tasks — configure for completeness
                val keystoreFile = file(storeFilePath)
                val resolved = if (keystoreFile.exists()) keystoreFile else rootProject.file(storeFilePath)
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
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Always attach release signing config — never unsigned
            signingConfig = signingConfigs.getByName("release")
        }
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
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
