# ============================================
# GOOGLE MOBILE ADS - CRÍTICO PARA ANUNCIOS
# ============================================
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }
-keep class com.google.android.ump.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }

# Mantener todos los adaptadores de mediación
-keep class * extends com.google.android.gms.ads.mediation.MediationAdapter { *; }
-keep class * extends com.google.android.gms.ads.mediation.Adapter { *; }
-keep class * extends com.google.android.gms.ads.mediation.rtb.RtbAdapter { *; }

-keepclassmembers class * extends com.google.android.gms.ads.mediation.Adapter {
    public <init>();
}
-keepclassmembers class * extends com.google.android.gms.ads.mediation.rtb.RtbAdapter {
    public <init>();
}

# ============================================
# FACEBOOK AUDIENCE NETWORK
# ============================================
-keep class com.facebook.** { *; }
-keep interface com.facebook.** { *; }
-dontwarn com.facebook.**

# Facebook Infer annotations
-keep class com.facebook.infer.annotation.** { *; }
-keep @interface com.facebook.infer.annotation.** { *; }
-dontwarn com.facebook.infer.annotation.**

# Facebook Audience Network SDK
-keep class com.facebook.ads.** { *; }
-keep interface com.facebook.ads.** { *; }
-keepclassmembers class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# Facebook native methods
-keepclasseswithmembernames class com.facebook.** {
    native <methods>;
}

# ============================================
# FIREBASE
# ============================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keepattributes Signature
-keepattributes *Annotation*

# Firebase Database
-keepclassmembers class com.follgramer.diamantesproplayersgo.** {
    @com.google.firebase.database.PropertyName <methods>;
}
-keepclassmembers class com.follgramer.diamantesproplayersgo.** {
    @com.google.firebase.database.PropertyName <fields>;
}

# ============================================
# TU APLICACIÓN
# ============================================
-keep class com.follgramer.diamantesproplayersgo.ads.** { *; }
-keep class com.follgramer.diamantesproplayersgo.DiamantesApplication { *; }
-keep class com.follgramer.diamantesproplayersgo.MainActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.SplashActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.BanActivity { *; }

-keep class com.follgramer.diamantesproplayersgo.models.** { *; }
-keep class com.follgramer.diamantesproplayersgo.data.** { *; }
-keep class com.follgramer.diamantesproplayersgo.notifications.** { *; }
-keep class com.follgramer.diamantesproplayersgo.ui.** { *; }
-keep class com.follgramer.diamantesproplayersgo.util.** { *; }

-keep class com.follgramer.diamantesproplayersgo.**.*Adapter { *; }
-keep class com.follgramer.diamantesproplayersgo.**.*ViewHolder { *; }

# ============================================
# ENCRYPTED SHAREDPREFERENCES
# ============================================
-keep class androidx.security.crypto.** { *; }

# ============================================
# VIEW BINDING
# ============================================
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}
-keep class com.follgramer.diamantesproplayersgo.databinding.** { *; }

# ============================================
# KOTLIN
# ============================================
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**

-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ============================================
# ANDROIDX
# ============================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ============================================
# MATERIAL COMPONENTS
# ============================================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ============================================
# WORKMANAGER
# ============================================
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ============================================
# COROUTINES
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ============================================
# MANTENER ATRIBUTOS
# ============================================
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable

# ============================================
# ELIMINAR LOGS EN RELEASE
# ============================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ============================================
# MANTENER RECURSOS R
# ============================================
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ============================================
# WARNINGS A SILENCIAR
# ============================================
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
-dontwarn com.google.android.gms.internal.**
-dontwarn com.google.android.gms.ads.internal.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**