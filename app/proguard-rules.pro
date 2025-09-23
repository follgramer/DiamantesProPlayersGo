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

# Tu aplicación - MANTENER CLASES DE ANUNCIOS
-keep class com.follgramer.diamantesproplayersgo.ads.** { *; }
-keep class com.follgramer.diamantesproplayersgo.DiamantesApplication { *; }
-keep class com.follgramer.diamantesproplayersgo.MainActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.SplashActivity { *; }

# Retrofit si lo usas
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Mantener anotaciones
-keepattributes *Annotation*

# Mantener nombres de clases internas
-keepattributes InnerClasses

# Para View Binding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# Material Dialogs
-keep class com.afollestad.materialdialogs.** { *; }

# Evitar optimización de código que pueda afectar anuncios
-dontoptimize
-dontobfuscate # Temporal para debug, quitar en producción final

# Logs en release (opcional, para debug)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}