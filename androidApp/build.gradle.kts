import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
try {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} catch (e: Exception) {
}

// Google Maps SDK key. Kept out of source control. Resolution order:
//   1. MAPS_API_KEY in local.properties (per-machine override)
//   2. GOOGLE_API_KEY env var — the convention shared with the legacy GalwayBus app / CI
//   3. MAPS_API_KEY as a Gradle or env property
// Falls back to an empty key, which renders a blank map.
val localProperties = Properties()
try {
    localProperties.load(FileInputStream(rootProject.file("local.properties")))
} catch (e: Exception) {
}
// Read env/Gradle properties via providers so the configuration cache tracks them (and so the
// value comes from the actual invocation environment, not a stale daemon's).
val mapsApiKey: String = (localProperties.getProperty("MAPS_API_KEY")?.takeIf { it.isNotBlank() }
    ?: providers.environmentVariable("GOOGLE_API_KEY").orNull
    ?: providers.gradleProperty("MAPS_API_KEY").orNull
    ?: providers.environmentVariable("MAPS_API_KEY").orNull
    ?: "")

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
        val name = "$versionMajor.$versionMinor.$versionNum"
        println("versionName is set to $name")
        return name
    } ?: return "1.0"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
ksp {
    arg("appfunctions:aggregateAppFunctions", "true")
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    // Ask GalwayBus: AppFunctions (also exposed to the system's assistants) and the on-device
    // ML Kit agent that uses them as its tools.
    implementation(libs.androidx.appfunctions)
    ksp(libs.androidx.appfunctions.compiler)
    implementation(libs.mlkit.genai.agent)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    testImplementation(libs.junit)
}

android {
    namespace = "dev.johnoreilly.galwaybus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        // Shared debug keystore (committed, password "android") matching the legacy GalwayBus app,
        // so debug builds carry the SHA-1 whitelisted on the Google Maps API key.
        getByName("debug") {
            storeFile = rootProject.file("debug.jks")
            keyAlias = "debug"
            keyPassword = "android"
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
        // 26 rather than the shared minSdk: ML Kit genai-agent requires it
        minSdk = 26
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = versionCode()
        versionName = versionName()
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}