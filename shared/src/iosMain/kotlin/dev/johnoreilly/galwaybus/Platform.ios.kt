package dev.johnoreilly.galwaybus

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override val appVersion: String = run {
        val bundle = NSBundle.mainBundle
        val short = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
        when {
            short != null && build != null -> "$short ($build)"
            short != null -> short
            else -> "unknown"
        }
    }
}

actual fun getPlatform(): Platform = IOSPlatform()
