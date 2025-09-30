# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ============================================
# SOLUCIÓN CRÍTICA PARA ERRORES DE COMPILACIÓN
# ============================================
# Estas reglas resuelven los errores de clases faltantes de Android Media
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener

# Silenciar warnings de clases internas no resueltas
-dontwarn com.google.android.gms.internal.**
-dontwarn com.google.android.gms.ads.internal.**
-dontwarn com.google.android.gms.common.internal.**
-dontnote com.google.android.gms.internal.**
-dontnote com.google.android.gms.common.**
-dontnote com.google.android.gms.ads.**

# ============================================
# GOOGLE MOBILE ADS - CRÍTICO PARA ANUNCIOS
# ============================================
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }
-keep class com.google.android.ump.** { *; }

# Mantener las clases internas de ads
-keep class com.google.android.gms.internal.ads.** { *; }

# WebView para anuncios
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# ============================================
# FIREBASE
# ============================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.internal.**

# Firebase Database
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.follgramer.diamantesproplayersgo.** {
    @com.google.firebase.database.PropertyName <methods>;
}
-keepclassmembers class com.follgramer.diamantesproplayersgo.** {
    @com.google.firebase.database.PropertyName <fields>;
}

# Firebase Analytics
-keep class com.google.android.gms.measurement.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# ============================================
# TU APLICACIÓN - MANTENER CLASES CRÍTICAS
# ============================================
-keep class com.follgramer.diamantesproplayersgo.ads.** { *; }
-keep class com.follgramer.diamantesproplayersgo.DiamantesApplication { *; }
-keep class com.follgramer.diamantesproplayersgo.MainActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.SplashActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.BanActivity { *; }

# Mantener todos los modelos de datos
-keep class com.follgramer.diamantesproplayersgo.models.** { *; }
-keep class com.follgramer.diamantesproplayersgo.data.** { *; }
-keep class com.follgramer.diamantesproplayersgo.notifications.** { *; }
-keep class com.follgramer.diamantesproplayersgo.ui.** { *; }
-keep class com.follgramer.diamantesproplayersgo.util.** { *; }

# Mantener adaptadores de RecyclerView
-keep class com.follgramer.diamantesproplayersgo.**.*Adapter { *; }
-keep class com.follgramer.diamantesproplayersgo.**.*ViewHolder { *; }

# ============================================
# ROOM DATABASE
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ============================================
# ENCRYPTED SHAREDPREFERENCES
# ============================================
-keep class androidx.security.crypto.** { *; }

# ============================================
# COROUTINES
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================
# MATERIAL DIALOGS
# ============================================
-keep class com.afollestad.materialdialogs.** { *; }
-dontwarn com.afollestad.materialdialogs.**

# ============================================
# SHORTCUTBADGER
# ============================================
-keep class me.leolin.shortcutbadger.** { *; }

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
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ============================================
# ANDROIDX
# ============================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# AppCompat
-keep public class androidx.appcompat.widget.** { *; }
-keep public class androidx.appcompat.view.menu.** { *; }

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ============================================
# MANTENER ANOTACIONES Y ATRIBUTOS
# ============================================
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable

# ============================================
# PARA PRODUCCIÓN (OPTIMIZACIÓN)
# ============================================
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ============================================
# LOGS EN RELEASE (ELIMINAR LOGS)
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
# MANTENER NATIVOS
# ============================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================
# MANTENER CONSTRUCTORES DE VISTAS
# ============================================
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ============================================
# PARCELABLE
# ============================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ============================================
# SERIALIZABLE
# ============================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# ENUMS
# ============================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================
# RETROFIT (para futuro uso)
# ============================================
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Exceptions

# ============================================
# OKHTTP (para futuro uso)
# ============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============================================
# GSON (para futuro uso)
# ============================================
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# ============================================
# WARNINGS GENERALES A IGNORAR
# ============================================
-dontwarn android.support.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.**

# ============================================
# JAVASCRIPT INTERFACE
# ============================================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ============================================
# MULTIDEX
# ============================================
-keep class androidx.multidex.** { *; }

# ============================================
# PLAY CORE (para actualizaciones in-app)
# ============================================
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# ============================================
# R8 FULL MODE
# ============================================
-allowaccessmodification
-repackageclasses ''

# ============================================
# SILENCIAR WARNINGS RESTANTES (si persisten)
# ============================================
# Descomenta la siguiente línea solo si los warnings no te permiten compilar
# -ignorewarnings