
object Versions {
    const val kotlin = "1.2.70"
    const val koin = "1.0.0"
    const val retrofit = "2.3.0"
    const val okHttp = "3.9.0"

}


object Deps {
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
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
    val rxjava2 = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
    val converterScalars = "com.squareup.retrofit2:converter-scalars:${Versions.retrofit}"
}