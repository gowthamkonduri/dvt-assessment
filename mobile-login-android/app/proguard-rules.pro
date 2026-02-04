# ProGuard/R8 rules for Mobile Login Android app
# These rules ensure proper code shrinking while preserving necessary classes

# ==================== Kotlin ====================
# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# ==================== Coroutines ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep StateFlow and Flow
-keep class kotlinx.coroutines.flow.** { *; }

# ==================== AndroidX Lifecycle / ViewModel ====================
# Keep ViewModel classes (constructor needed for Hilt)
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep LiveData observers
-keepclassmembers class * {
    @androidx.lifecycle.OnLifecycleEvent *;
}

# ==================== Hilt / Dagger ====================
# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep @Inject annotated constructors
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep @HiltViewModel annotated classes
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ==================== App-specific classes ====================
# Keep data classes and sealed interfaces (needed for when expressions)
-keep class com.dvt.mobilelogin.LoginUiState { *; }
-keep class com.dvt.mobilelogin.LoginEvent { *; }
-keep class com.dvt.mobilelogin.LoginEvent$* { *; }
-keep class com.dvt.mobilelogin.AuthException { *; }

# Keep interfaces for DI
-keep interface com.dvt.mobilelogin.TokenStore { *; }
-keep interface com.dvt.mobilelogin.NetworkMonitor { *; }
-keep interface com.dvt.mobilelogin.AuthRepository { *; }

# ==================== DataStore ====================
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ==================== Security Crypto / Tink ====================
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Missing annotations from error-prone (used by Tink but not runtime required)
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn com.google.errorprone.annotations.concurrent.LazyInit
-dontwarn com.google.errorprone.annotations.InlineMe
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy

# Google API Client (optional Tink dependency)
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant

# ==================== Compose ====================
# Keep Compose compiler metadata
-keep class androidx.compose.runtime.** { *; }

# ==================== General Android ====================
# Keep R8 from removing annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep source file names for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
