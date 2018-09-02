
object Versions {
    const val kotlin = "1.2.60"
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