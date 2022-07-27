
object Versions {
    const val androidMinSdk = 21
    const val androidCompileSdk = 32
    const val androidTargetSdk = androidCompileSdk

    const val kotlin = "1.7.10"
    const val kotlinCoroutines = "1.6.4"
    const val kotlinxSerialization = "1.3.3"
    const val kotlinxDateTime = "0.4.0"
    const val koin = "3.2.0"
    const val sqlDelight = "1.5.3"
    const val multiplatformSettings = "0.9"
    const val kmpNativeCoroutines = "0.12.5-new-mm"

    const val compose = "1.2.0"
    const val composeCompiler = "1.3.0-rc01"
    const val navCompose = "2.4.2"
    const val accompanist = "0.24.13-rc"
    const val mapsCompose = "2.5.0"

    const val kermit = "1.0.0"
    const val ktor = "2.0.0"
    const val slf4j = "1.7.30"

    const val junit = "4.12"
    const val mockito = "2.27.0"
    const val robolectric = "3.6.1"
}


object Koin {
    val core = "io.insert-koin:koin-core:${Versions.koin}"
    val test = "io.insert-koin:koin-test:${Versions.koin}"
    val android = "io.insert-koin:koin-android:${Versions.koin}"
    val compose = "io.insert-koin:koin-androidx-compose:${Versions.koin}"
}

object Firebase {
    val core = "com.google.firebase:firebase-core:16.0.9"
    val performance = "com.google.firebase:firebase-perf:16.2.3"
}



object Deps {

    object Kotlinx {
        const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
        const val serializationCore = "org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.kotlinxSerialization}"
        const val dateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDateTime}"
    }

    object Test {
        const val junit = "junit:junit:${Versions.junit}"
        const val mockito = "org.mockito:mockito-inline:${Versions.mockito}"
        const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    }

    object Compose {
        const val compiler = "androidx.compose.compiler:compiler:${Versions.composeCompiler}"
        const val ui = "androidx.compose.ui:ui:${Versions.compose}"
        const val uiGraphics = "androidx.compose.ui:ui-graphics:${Versions.compose}"
        const val uiTooling = "androidx.compose.ui:ui-tooling:${Versions.compose}"
        const val foundationLayout = "androidx.compose.foundation:foundation-layout:${Versions.compose}"
        const val material = "androidx.compose.material:material:${Versions.compose}"
        const val navigation = "androidx.navigation:navigation-compose:${Versions.navCompose}"
        const val accompanistPlaceholder = "com.google.accompanist:accompanist-placeholder:${Versions.accompanist}"
        const val mapsCompose = "com.google.maps.android:maps-compose:${Versions.mapsCompose}"
    }

    object PlayServices {
        val location = "com.google.android.gms:play-services-location:16.0.0"
        val maps = "com.google.android.gms:play-services-maps:18.0.2"
    }

    object Koin {
        const val core = "io.insert-koin:koin-core:${Versions.koin}"
        const val test = "io.insert-koin:koin-test:${Versions.koin}"
        const val testJUnit4 = "io.insert-koin:koin-test-junit4:${Versions.koin}"
        const val android = "io.insert-koin:koin-android:${Versions.koin}"
        const val compose = "io.insert-koin:koin-androidx-compose:${Versions.koin}"
    }

    object Ktor {
        const val serverCore = "io.ktor:ktor-server-core:${Versions.ktor}"
        const val contentNegotiation = "io.ktor:ktor-client-content-negotiation:${Versions.ktor}"
        const val json = "io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}"

        const val clientCore = "io.ktor:ktor-client-core:${Versions.ktor}"
        const val clientJson = "io.ktor:ktor-client-json:${Versions.ktor}"
        const val clientLogging = "io.ktor:ktor-client-logging:${Versions.ktor}"
        const val clientIos = "io.ktor:ktor-client-ios:${Versions.ktor}"
        const val clientJava = "io.ktor:ktor-client-java:${Versions.ktor}"
    }

    object SqlDelight {
        const val runtime = "com.squareup.sqldelight:runtime:${Versions.sqlDelight}"
        const val coroutineExtensions = "com.squareup.sqldelight:coroutines-extensions:${Versions.sqlDelight}"
        const val androidDriver = "com.squareup.sqldelight:android-driver:${Versions.sqlDelight}"
        const val nativeDriver = "com.squareup.sqldelight:native-driver:${Versions.sqlDelight}"
        const val nativeDriverMacos = "com.squareup.sqldelight:native-driver-macosx64:${Versions.sqlDelight}"
        const val sqliteDriver = "com.squareup.sqldelight:sqlite-driver:${Versions.sqlDelight}"
    }

    object Log {
        const val slf4j = "org.slf4j:slf4j-simple:${Versions.slf4j}"
        const val kermit = "co.touchlab:kermit:${Versions.kermit}"
    }

    const val multiplatformSettings = "com.russhwolf:multiplatform-settings:${Versions.multiplatformSettings}"
    const val multiplatformSettingsCoroutines = "com.russhwolf:multiplatform-settings-coroutines:${Versions.multiplatformSettings}"

}
