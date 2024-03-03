
@file:Suppress("OPT_IN_USAGE")

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("com.android.library")
    id("app.cash.sqldelight")
    id("com.google.devtools.ksp")
    id("com.rickclephas.kmp.nativecoroutines")
}

// CocoaPods requires the podspec to have a version.
version = "1.0"


android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "com.surrus.galwaybus.lib"
}


kotlin {
    applyDefaultHierarchyTemplate()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "SharedCode"
        }
    }

    macosX64("macos")
    androidTarget()
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.bundles.ktor.common)
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.serialization)
                implementation(libs.kotlinx.datetime)

                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)

                api(libs.koin.core)
                implementation(libs.koin.test)

                api(libs.kermit)
                api(libs.bundles.multiplatformSettings)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.sqldelight.android.driver)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native.driver)
            }
        }

        macosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native.driver)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.slf4j)
            }
        }
    }
}


kotlin {
    sourceSets.all {
        languageSettings {
            optIn("kotlin.RequiresOptIn")
        }
    }
}

sqldelight {
    databases {
        create("MyDatabase") {
            packageName.set("com.surrus.galwaybus.db")
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

