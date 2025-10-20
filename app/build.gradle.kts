import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
}

android {
    namespace = "com.follgramer.diamantesproplayersgo"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties().apply {
                    load(FileInputStream(keystorePropertiesFile))
                }
                storeFile = file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
            } else {
                storeFile = file("diamantes-keystore.jks")
                storePassword = "77095132"
                keyAlias = "diamantes-key"
                keyPassword = "77095132"
            }
        }
    }

    defaultConfig {
        applicationId = "com.follgramer.diamantesproplayersgo"
        minSdk = 24
        targetSdk = 35
        versionCode = 10
        versionName = "1.0.9"

        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        renderscriptTargetApi = 24
        renderscriptSupportModeEnabled = false

        manifestPlaceholders["deepLinkScheme"] = "diamantespro"
        manifestPlaceholders["deepLinkHost"] = "refer"

        buildConfigField("String", "REFERRAL_SCHEME", "\"diamantespro\"")
        buildConfigField("String", "REFERRAL_HOST", "\"refer\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            buildConfigField("boolean", "USE_TEST_ADS", "true")
            buildConfigField("boolean", "ENABLE_REFERRAL_TESTING", "true")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            versionNameSuffix = "-DEBUG"
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("boolean", "USE_TEST_ADS", "false")
            buildConfigField("boolean", "ENABLE_REFERRAL_TESTING", "false")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-2024712392092488~7992650364"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        dataBinding = true
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module"
            )
            pickFirsts += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    // ==================== CORE ANDROID ====================
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.webkit:webkit:1.12.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // ==================== GOOGLE ADS Y REFERIDOS ====================
    implementation("com.google.android.gms:play-services-ads:24.5.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")

    // Facebook Audience Network
    implementation("com.facebook.android:audience-network-sdk:6.17.0")

    // ==================== FIREBASE ====================
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity:18.0.0")

    // ==================== LIFECYCLE Y COROUTINES ====================
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // ==================== SECURITY ====================
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ==================== MATERIAL DIALOGS ====================
    implementation("com.afollestad.material-dialogs:core:3.3.0")
    implementation("com.afollestad.material-dialogs:input:3.3.0")

    // ==================== IN-APP UPDATES & REVIEW ====================
    implementation("com.google.android.play:review-ktx:2.0.2")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.google.android.play:integrity:1.4.0")

    // ==================== INSTALL REFERRER ====================
    implementation("com.android.installreferrer:installreferrer:2.2")

    // ==================== UTILIDADES ====================
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")
    implementation("androidx.multidex:multidex:2.0.1")

    // ==================== WORKMANAGER ====================
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // ==================== STARTUP ====================
    implementation("androidx.startup:startup-runtime:1.2.0")

    // ==================== NETWORKING ====================
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ==================== IMAGE LOADING ====================
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // ==================== CORE LIBRARY DESUGARING ====================
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // ==================== TESTING ====================
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
}

tasks.register("printVersionInfo") {
    doLast {
        println("=====================================")
        println("Build Information")
        println("=====================================")
        println("App: ${android.defaultConfig.applicationId}")
        println("Version: ${android.defaultConfig.versionName} (${android.defaultConfig.versionCode})")
        println("Min SDK: ${android.defaultConfig.minSdk}")
        println("Target SDK: ${android.defaultConfig.targetSdk}")
        println("=====================================")
    }
}

// FIX COMPLETO PARA ERRORES DE WINDOWS
afterEvaluate {
    tasks.configureEach {
        if (name == "produceReleaseBundleIdeListingFile" ||
            name == "createReleaseBundleListingFileRedirect") {
            enabled = false
        }
    }
}