
object Versions {
    const val kotlin = "1.3.11"
    const val kotlinCoroutines = "1.0.1"
    const val koin = "1.0.2"
    const val retrofit = "2.4.0"
    const val okHttp = "3.11.0"
    const val ktx = "1.0.1"
    const val nav = "1.0.0-alpha06"
    const val work = "1.0.0-alpha10"
    const val room = "2.1.0-alpha01"
}


object Kotlin {
    const val stdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}"
}


object ArchComponents {
    val coreKtx = "androidx.core:core-ktx:${Versions.ktx}"
    val navFragmentKtx = "android.arch.navigation:navigation-fragment-ktx:${Versions.nav}"
    val navUiKtx = "android.arch.navigation:navigation-ui-ktx:${Versions.nav}"
    val work = "android.arch.work:work-runtime-ktx:${Versions.work}"
    val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
    val roomCompiler =  "androidx.room:room-compiler:${Versions.room}"
}


object Koin {
    val core = "org.koin:koin-core:${Versions.koin}"
    val android = "org.koin:koin-android:${Versions.koin}"
    val androidViewModel = "org.koin:koin-android-viewmodel:${Versions.koin}"
}

object Firebase {
    val core = "com.google.firebase:firebase-core:16.0.3"
    val performance = "com.google.firebase:firebase-perf:16.1.0"
}

object PlayServices {
    val location = "com.google.android.gms:play-services-location:15.0.1"
    val maps = "com.google.android.gms:play-services-maps:15.0.1"
}


object Okhttp {
    val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
    val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}"
}

object Retrofit {
    val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    val converterGson = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    val converterScalars = "com.squareup.retrofit2:converter-scalars:${Versions.retrofit}"
    val coroutinesAdapter = "com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2"
}