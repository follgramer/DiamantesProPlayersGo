# ============================================
# PROYECTO: Diamantes Pro Players Go
# ÚLTIMA ACTUALIZACIÓN: Enero 2025
# INCLUYE: Sistema de referidos completo
# ============================================

# ============================================
# CONFIGURACIÓN BÁSICA
# ============================================
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Optimizaciones específicas
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ============================================
# MANTENER ATRIBUTOS CRÍTICOS
# ============================================
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# ============================================
# GOOGLE MOBILE ADS - CRÍTICO PARA ANUNCIOS
# ============================================
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }
-keep class com.google.android.ump.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }
-keep class com.google.android.gms.ads.mediation.** { *; }

# Adaptadores de mediación
-keep class * extends com.google.android.gms.ads.mediation.MediationAdapter { *; }
-keep class * extends com.google.android.gms.ads.mediation.Adapter { *; }
-keep class * extends com.google.android.gms.ads.mediation.rtb.RtbAdapter { *; }

-keepclassmembers class * extends com.google.android.gms.ads.mediation.Adapter {
    public <init>();
}
-keepclassmembers class * extends com.google.android.gms.ads.mediation.rtb.RtbAdapter {
    public <init>();
}

# AdMob específico
-keep class com.google.android.gms.ads.AdView { *; }
-keep class com.google.android.gms.ads.InterstitialAd { *; }
-keep class com.google.android.gms.ads.rewarded.** { *; }

# ============================================
# ADVERTISING ID - ESENCIAL PARA REFERIDOS
# ============================================
-keep class com.google.android.gms.ads.identifier.** { *; }
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient { *; }
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info { *; }
-dontwarn com.google.android.gms.ads.identifier.**

# ============================================
# FACEBOOK AUDIENCE NETWORK
# ============================================
-keep class com.facebook.** { *; }
-keep interface com.facebook.** { *; }
-dontwarn com.facebook.**

# Facebook SDK completo
-keep class com.facebook.ads.** { *; }
-keep interface com.facebook.ads.** { *; }
-keepclassmembers class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# Facebook annotations
-keep class com.facebook.infer.annotation.** { *; }
-keep @interface com.facebook.infer.annotation.** { *; }
-dontwarn com.facebook.infer.annotation.**

# Facebook native methods
-keepclasseswithmembernames class com.facebook.** {
    native <methods>;
}

# ============================================
# FIREBASE COMPLETO
# ============================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Database (crítico para referidos)
-keep class com.google.firebase.database.** { *; }
-keepclassmembers class com.follgramer.diamantesproplayersgo.** {
    @com.google.firebase.database.PropertyName <methods>;
}
-keepclassmembers class com.follgramer.diamantesproplayersgo.** {
    @com.google.firebase.database.PropertyName <fields>;
}

# Firebase Analytics
-keep class com.google.android.gms.measurement.** { *; }
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.android.gms.measurement.**

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**
-keep public class * extends java.lang.Exception

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.firebase.iid.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Remote Config
-keep class com.google.firebase.remoteconfig.** { *; }

# ============================================
# SISTEMA DE REFERIDOS - CRÍTICO
# ============================================
-keep class com.follgramer.diamantesproplayersgo.util.ReferralManager { *; }
-keep class com.follgramer.diamantesproplayersgo.util.ReferralManager$** { *; }
-keep class com.follgramer.diamantesproplayersgo.util.ShareManager { *; }
-keep class com.follgramer.diamantesproplayersgo.util.ShareManager$** { *; }
-keep class com.follgramer.diamantesproplayersgo.util.DeepLinkHandler { *; }

# Clases de datos para referidos
-keep class com.follgramer.diamantesproplayersgo.util.ReferralManager$ReferralStats { *; }
-keep class com.follgramer.diamantesproplayersgo.util.ReferralManager$InviteLinks { *; }
-keep class com.follgramer.diamantesproplayersgo.util.ShareManager$ShareConfig { *; }

# Métodos críticos del sistema de referidos
-keepclassmembers class com.follgramer.diamantesproplayersgo.util.ReferralManager {
    public static *** getDeviceId(...);
    public static *** generateReferralCode(...);
    public static *** processReferralCode(...);
    public static *** checkAndGrantReward(...);
    public static *** getReferralStats(...);
    public static *** createReferralCode(...);
    public static *** generateInviteLinks(...);
}

-keepclassmembers class com.follgramer.diamantesproplayersgo.util.ShareManager {
    public static *** shareApp(...);
    public static *** getShareConfig(...);
    public static *** logShare(...);
}

# ============================================
# TU APLICACIÓN PRINCIPAL
# ============================================
-keep class com.follgramer.diamantesproplayersgo.MainActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.SplashActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.BanActivity { *; }
-keep class com.follgramer.diamantesproplayersgo.DiamantesApplication { *; }

# Gestores principales
-keep class com.follgramer.diamantesproplayersgo.SessionManager { *; }
-keep class com.follgramer.diamantesproplayersgo.util.AnalyticsManager { *; }
-keep class com.follgramer.diamantesproplayersgo.util.CommunicationManager { *; }

# Paquetes completos
-keep class com.follgramer.diamantesproplayersgo.ads.** { *; }
-keep class com.follgramer.diamantesproplayersgo.models.** { *; }
-keep class com.follgramer.diamantesproplayersgo.data.** { *; }
-keep class com.follgramer.diamantesproplayersgo.notifications.** { *; }
-keep class com.follgramer.diamantesproplayersgo.ui.** { *; }
-keep class com.follgramer.diamantesproplayersgo.util.** { *; }
-keep class com.follgramer.diamantesproplayersgo.fcm.** { *; }

# Adaptadores y ViewHolders
-keep class com.follgramer.diamantesproplayersgo.**.*Adapter { *; }
-keep class com.follgramer.diamantesproplayersgo.**.*ViewHolder { *; }

# ============================================
# VIEW BINDING Y DATA BINDING
# ============================================
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
-keep class com.follgramer.diamantesproplayersgo.databinding.** { *; }

# Data Binding
-keep class androidx.databinding.** { *; }
-keep class * extends androidx.databinding.DataBindingComponent { *; }

# ============================================
# ANDROIDX Y MATERIAL DESIGN
# ============================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Navigation Component
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ============================================
# KOTLIN Y COROUTINES
# ============================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ============================================
# ENCRYPTED SHAREDPREFERENCES
# ============================================
-keep class androidx.security.crypto.** { *; }
-keep class androidx.security.crypto.MasterKeys { *; }
-keep class androidx.security.crypto.EncryptedSharedPreferences { *; }

# ============================================
# NETWORKING
# ============================================
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================
# MATERIAL DIALOGS
# ============================================
-keep class com.afollestad.materialdialogs.** { *; }
-dontwarn com.afollestad.materialdialogs.**

# ============================================
# GLIDE IMAGE LOADING
# ============================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.** { *; }

# ============================================
# LOTTIE ANIMATIONS
# ============================================
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ============================================
# INSTALL REFERRER (para referidos)
# ============================================
-keep class com.android.installreferrer.** { *; }
-dontwarn com.android.installreferrer.**

# ============================================
# MANTENER RECURSOS ANDROID
# ============================================
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Mantener Activities, Services, etc.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# View constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================
# DEEP LINKS Y INTENT FILTERS
# ============================================
-keep class com.follgramer.diamantesproplayersgo.util.DeepLinkHandler { *; }
-keepclassmembers class com.follgramer.diamantesproplayersgo.util.DeepLinkHandler {
    public static *** handle(...);
    public static *** checkPendingReferralCode(...);
    public static *** generateInviteLinks(...);
}

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
# TESTING (solo en debug)
# ============================================
-keep class com.follgramer.diamantesproplayersgo.util.TestReferrals { *; }

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
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.checkerframework.**
-dontwarn com.google.j2objc.annotations.**

# ============================================
# MANTENER SERIALIZABLES
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
# PARCELABLES
# ============================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================
# CONFIGURACIÓN FINAL
# ============================================
# Mantener información para stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Optimización agresiva para release
-repackageclasses ''
-allowaccessmodification