package dev.johnoreilly.galwaybus

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    // Read straight from the installed package so it always reflects the actual build.
    override val appVersion: String = run {
        val context = GalwayBusPrefs.appContext ?: return@run "unknown"
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
                       else info.versionCode.toLong()
            "${info.versionName} ($code)"
        } catch (_: Exception) {
            "unknown"
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
