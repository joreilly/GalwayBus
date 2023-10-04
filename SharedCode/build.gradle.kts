
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
    compileSdk = AndroidSdk.compile
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = AndroidSdk.min
        targetSdk = AndroidSdk.target

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "com.surrus.galwaybus.lib"
}


kotlin {
    targetHierarchy.default()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "SharedCode"
        }
    }

    macosX64("macOS")
    androidTarget()
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                with(Deps.Kotlinx) {
                    implementation(Deps.Kotlinx.coroutinesCore)
                    implementation(serializationCore)
                    implementation(dateTime)
                }

                with(Deps.Ktor) {
                    implementation(clientCore)
                    implementation(clientJson)
                    implementation(clientLogging)
                    implementation(contentNegotiation)
                    implementation(json)
                }

                with(Deps.SqlDelight) {
                    implementation(runtime)
                    implementation(coroutineExtensions)
                }

                with(Deps.Koin) {
                    api(core)
                    api(test)
                }

                with(Deps.Log) {
                    api(kermit)
                }

                api(Deps.multiplatformSettings)
                api(Deps.multiplatformSettingsCoroutines)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-android:${Versions.ktor}")
                implementation("app.cash.sqldelight:android-driver:${Versions.sqlDelight}")
            }
        }

        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:${Versions.ktor}")
                implementation("app.cash.sqldelight:native-driver:${Versions.sqlDelight}")
            }
        }

        val macosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:${Versions.ktor}")
                implementation("app.cash.sqldelight:native-driver-macosx64:${Versions.sqlDelight}")
                }
        }


        val jvmMain by getting {
            dependencies {
                implementation(Deps.Ktor.clientJava)
                //implementation(Ktor.slf4j)
                implementation("app.cash.sqldelight:sqlite-driver:${Versions.sqlDelight}")
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
            //sourceFolders = listOf("sqldelight")
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

