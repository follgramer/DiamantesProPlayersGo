plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false // ✅ CRASHLYTICS
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
    doLast {
        println("✅ Limpieza completada")
    }
}