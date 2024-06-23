import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.*

plugins {
    alias(libs.plugins.android.application)
    kotlin("android")
    alias(libs.plugins.googleServices)
    alias(libs.plugins.compose.compiler)
}


val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
try {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
catch(e: Exception) {
}

val versionMajor = 1
val versionMinor = 1

val versionNum: String? by project

fun versionCode(): Int {
    versionNum?.let {
        val code: Int = (versionMajor * 1000000) + (versionMinor * 1000) + it.toInt()
        println("versionCode is set to $code")
        return code
    } ?: return 1
}

fun versionName(): String {
    versionNum?.let {
        val name = "${versionMajor}.${versionMinor}.${versionNum}"
        println("versionName is set to $name")
        return name
    } ?: return "1.0"
}


android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    signingConfigs {

        getByName("debug") {
            keyAlias = "debug"
            keyPassword = "android"
            storeFile= file("../debug.jks")
            storePassword = "android"
        }

        create("release") {
            storeFile = file("/Users/joreilly/dev/keystore/galwaybus_android.jks")
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storePassword = keystoreProperties["storePassword"] as String?
            enableV2Signing = true
        }
    }

    defaultConfig {
        applicationId = "dev.johnoreilly.galwaybus"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        this.versionCode = versionCode()
        this.versionName = versionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val googleMapsKey = System.getenv("GOOGLE_API_KEY") ?: "test"
        resValue("string", "google_maps_key", googleMapsKey)
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf("META-INF/*.kotlin_module")
        }
    }

    namespace = "dev.johnoreilly.galwaybus"
}


dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation(platform("com.google.firebase:firebase-bom:26.2.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("androidx.activity:activity-compose:1.7.2")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3.windowSizeClass)

    implementation(libs.googleMapsCompose)
    implementation(libs.googleMapsComposeUtils)

    implementation(libs.accompanist.swiperefresh)



    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation("io.github.pushpalroy:jetlime:2.0.1")

    // TODO: Added this as a temporary fix for a crash in ProgressIndicator, can be removed later.
    // Issue: https://github.com/JetBrains/compose-multiplatform/issues/4157
    implementation("androidx.compose.material3:material3-android:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")

    implementation("com.google.android.gms:play-services-location:16.0.0")
    implementation("com.google.android.gms:play-services-maps:18.0.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(project(":SharedCode"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
