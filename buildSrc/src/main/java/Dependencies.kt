
object Versions {
    const val kotlin = "1.2.60"
    const val koin = "0.9.3"
    val support = "27.0.2"
}


object Deps {
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
}

object SupportLibraries {
    val appcompat = "com.android.support:appcompat-v7:${Versions.support}}"
    val design = "com.android.support:design:${Versions.support}"
    val supportV4 = "com.android.support:support-v4:${Versions.support}"
    val vectorDrawable = "com.android.support:support-vector-drawable:${Versions.support}"
}


object Koin {
    val core = "org.koin:koin-core:${Versions.koin}"
    val android = "org.koin:koin-android:${Versions.koin}"
    val androidArchitecture = "org.koin:koin-android-architecture:${Versions.koin}"
}