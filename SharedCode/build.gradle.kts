
plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("com.android.library")
    id("com.squareup.sqldelight")
    id("com.google.devtools.ksp")
    id("com.rickclephas.kmp.nativecoroutines")
}

// CocoaPods requires the podspec to have a version.
version = "1.0"


//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//    kotlinOptions {
//        jvmTarget = "1.8"
//        freeCompilerArgs = listOf("-Xuse-experimental=kotlin.time.ExperimentalTime", "-Xobjc-generics")
//    }
//}


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
    targets {
        val iosTarget: (String, org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.() -> Unit) -> org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget = when {
            System.getenv("SDK_NAME")?.startsWith("iphoneos") == true -> ::iosArm64
            System.getenv("NATIVE_ARCH")?.startsWith("arm") == true -> ::iosSimulatorArm64 // available to KT 1.5.30
            else -> ::iosX64
        }
        iosTarget("iOS") {
            binaries.framework {
                baseName = "SharedCode"

                // re. https://youtrack.jetbrains.com/issue/KT-60230/Native-unknown-options-iossimulatorversionmin-sdkversion-with-Xcode-15-beta-3
                // due to be fixed in Kotlin 1.9.10
                if (System.getenv("XCODE_VERSION_MAJOR") == "1500") {
                    linkerOpts += "-ld64"
                }
            }
        }

        macosX64("macOS")
        androidTarget()
        jvm()
    }

    sourceSets {
        val commonMain by getting {
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
                implementation("com.squareup.sqldelight:android-driver:${Versions.sqlDelight}")
            }
        }

        val iOSMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:${Versions.ktor}")
                implementation("com.squareup.sqldelight:native-driver:${Versions.sqlDelight}")
            }
        }

        val macOSMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:${Versions.ktor}")
                implementation("com.squareup.sqldelight:native-driver-macosx64:${Versions.sqlDelight}")
                }
        }


        val jvmMain by getting {
            dependencies {
                implementation(Deps.Ktor.clientJava)
                //implementation(Ktor.slf4j)
                implementation("com.squareup.sqldelight:sqlite-driver:${Versions.sqlDelight}")
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
    database("MyDatabase") {
        packageName = "com.surrus.galwaybus.db"
        sourceFolders = listOf("sqldelight")
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

