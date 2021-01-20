
plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("com.android.library")
    id("org.jetbrains.kotlin.native.cocoapods")
    id("com.squareup.sqldelight")
}

// CocoaPods requires the podspec to have a version.
version = "1.0"


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xuse-experimental=kotlin.time.ExperimentalTime", "-Xobjc-generics")
    }
}


android {
    compileSdkVersion(AndroidSdk.compile)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(AndroidSdk.min)
        targetSdkVersion(AndroidSdk.target)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}


// workaround for https://youtrack.jetbrains.com/issue/KT-43944
android {
    configurations {
        create("androidTestApi")
        create("androidTestDebugApi")
        create("androidTestReleaseApi")
        create("testApi")
        create("testDebugApi")
        create("testReleaseApi")
    }
}


kotlin {
    targets {
        val sdkName: String? = System.getenv("SDK_NAME")

        val isiOSDevice = sdkName.orEmpty().startsWith("iphoneos")
        if (isiOSDevice) {
            iosArm64("iOS")
        } else {
            iosX64("iOS")
        }
        macosX64("macOS")
        android()
        jvm()
    }

    js {
        browser {
        }
    }


    cocoapods {
        // Configure fields required by CocoaPods.
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}") {
                    isForce = true
                }

                // Ktor
                implementation("io.ktor:ktor-client-core:${Versions.ktor}")
                implementation("io.ktor:ktor-client-json:${Versions.ktor}")
                implementation("io.ktor:ktor-client-logging:${Versions.ktor}")
                implementation("io.ktor:ktor-client-serialization:${Versions.ktor}")

                // Serialize
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.kotlinxSerialization}")

                // SQL Delight
                implementation("com.squareup.sqldelight:runtime:${Versions.sqlDelight}")
                implementation("com.squareup.sqldelight:coroutines-extensions:${Versions.sqlDelight}")

                // koin
                api("org.koin:koin-core:${Versions.koin}")

                api(Deps.multiplatformSettings)
                api(Deps.kermit)
                api(Deps.kotlinxDateTime)
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
                implementation("io.ktor:ktor-client-apache:${Versions.ktor}")
                //implementation(Ktor.slf4j)
                //implementation("org.xerial:sqlite-jdbc:${Versions.sqliteJdbcDriver}")
                implementation("com.squareup.sqldelight:sqlite-driver:${Versions.sqlDelight}")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:${Versions.ktor}")
            }
        }
    }
}

sqldelight {
    database("MyDatabase") {
        packageName = "com.surrus.galwaybus.db"
        sourceFolders = listOf("sqldelight")
    }
}

