// ==============================================================================
//       Buildscript a Nivel de App: Aplica plugins y define dependencias
// ==============================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.services)
}

android {
    namespace = "com.follgramer.diamantesproplayersgo"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.follgramer.diamantesproplayersgo"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        /**
         * Configuración de build para la versión de lanzamiento (release).  Se habilita la
         * minificación y compresión de recursos para reducir el tamaño del APK/AAB y mejorar
         * el rendimiento.  R8 se encargará de eliminar código y recursos no utilizados.
         */
        release {
            // Habilita el uso de R8/ProGuard para minificar y ofuscar el código.
            // Esto reduce el tamaño final del binario y mejora la seguridad.
            isMinifyEnabled = true

            // Habilita la compresión de recursos.  Los archivos no usados se eliminarán
            // automáticamente y las imágenes se optimizarán.
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        /**
         * Configuración para la versión de depuración (debug).  Se deshabilita la
         * minificación para facilitar la depuración.  isDebuggable se mantiene en true.
         */
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:2.1.0")
        }
    }
}

dependencies {
    // ✅ DEPENDENCIAS BÁSICAS DE ANDROID
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.drawerlayout)

    // ✅ FIREBASE - PLATAFORMA Y SERVICIOS
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)

    // ✅ GOOGLE ADS Y CONSENTIMIENTO
    implementation(libs.google.play.services.ads)
    implementation(libs.google.user.messaging.platform)

    // ✅ MATERIAL DIALOGS
    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.input)

    // ✅ COROUTINES PARA OPTIMIZACIÓN DEL SPLASH
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ✅ RECYCLERVIEW (para leaderboards y listas)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ✅ LIFECYCLE COMPONENTS (para ViewModels si los usas)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // ✅ TESTING
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.espresso.core)
}
