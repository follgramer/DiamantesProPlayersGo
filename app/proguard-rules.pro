# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Google Mobile Ads - CRÍTICO PARA ANUNCIOS
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }
-keep class com.google.android.ump.** { *; }

# WebView para anuncios
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.firebase.**

# Firebase Database
-keepattributes Signature
-keepclassmembers class com.follgramer.diamantesproplayersgo.** {
    @com.google.firebase.database.PropertyName <methods>;
}

# Tu aplicación - MANTENER CLASES DE ANUNCIOS
-keep class com.follgramer.diamantesproplayersgo.ads.** { *; }
-keep class com.follgramer.diamantesproplayersgo.DiamantesApplication { *; }
-keep class com.follgramer.diamantesproplayersgo.MainActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.SplashActivity { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Encrypted SharedPreferences
-keep class androidx.security.crypto.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Material Dialogs
-keep class com.afollestad.materialdialogs.** { *; }
-dontwarn com.afollestad.materialdialogs.**

# ShortcutBadger
-keep class me.leolin.shortcutbadger.** { *; }

# Mantener anotaciones
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Para View Binding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Retrofit si lo usas en el futuro
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Exceptions

# OkHttp si lo usas
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson si lo usas
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }

# Para producción (habilitar ofuscación y optimización)
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Logs en release (eliminar logs en producción)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Mantener nombres de recursos
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Evitar warnings comunes
-dontwarn android.support.**
-dontwarn androidx.**