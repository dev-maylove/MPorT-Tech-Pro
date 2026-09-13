# MPorT Tech Pro — R8 / ProGuard rules (release)

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, Exceptions

# Application entry points
-keep class com.mporttech.pro.MPorTTechApplication { *; }
-keep class com.mporttech.pro.MainActivity { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class com.mporttech.pro.core.database.** { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}

# Kotlin metadata / coroutines
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Compose (R8 full mode)
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# OkHttp / Retrofit / Gson
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Enums used in Parcelable / Room / Gson
-keepclassmembers enum * { *; }

-keep class com.mporttech.pro.features.discovery.** { *; }

# Full Core V2
-keep class com.mporttech.pro.domain.** { *; }
-keep class com.mporttech.pro.di.** { *; }
-keep class com.mporttech.pro.core.security.** { *; }
-keep class com.mporttech.pro.core.network.** { *; }
-keep class com.mporttech.pro.core.common.** { *; }
-keep class com.mporttech.pro.features.mikrotik.** { *; }
-keep class com.mporttech.pro.features.network.** { *; }
-keep class com.mporttech.pro.features.diagnostics.** { *; }
-keep class com.mporttech.pro.features.dashboard.DashboardViewModel { *; }

# Security Crypto / Google Tink (EncryptedSharedPreferences)
# R8 complains about errorprone annotations referenced only in Tink signatures
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }

# Gson DTOs (reflection)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.mporttech.pro.data.remote.dto.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit
-keep,allowobfuscation interface * {
  @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Tools / live network / speed test / wifi (reflection-safe)
-keep class com.mporttech.pro.features.tools.** { *; }
-keep class com.mporttech.pro.features.speedtest.** { *; }
-keep class com.mporttech.pro.features.wifi.** { *; }
-keep class com.mporttech.pro.features.scanner.** { *; }
-keep class com.mporttech.pro.core.database.** { *; }
-keep class com.mporttech.pro.data.repository.** { *; }
