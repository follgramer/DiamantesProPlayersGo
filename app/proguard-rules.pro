# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep AdMob classes
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep MediaCodec related classes
-keep class android.media.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn android.media.**

# Keep WebView classes
-keep class android.webkit.** { *; }
-keep class androidx.webkit.** { *; }
-dontwarn android.webkit.**

# Keep AttributionTag related classes
-keep class com.google.android.gms.ads.identifier.** { *; }
-keep class com.google.android.gms.appset.** { *; }

# Keep Bluetooth classes (for AdMob)
-keep class android.bluetooth.** { *; }
-dontwarn android.bluetooth.**

# Keep Audio classes
-keep class android.media.AudioManager { *; }
-keep class android.media.AudioAttributes { *; }
-keep class android.media.AudioFocusRequest { *; }

# Keep WorkManager classes
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep your app classes
-keep class com.follgramer.diamantesproplayersgo.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep serialization classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Suppress warnings for missing classes
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.apache.log4j.**
-dontwarn org.slf4j.**

# R8 compatibility
-adaptclassstrings
-allowaccessmodification