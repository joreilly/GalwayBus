package dev.johnoreilly.galwaybus

interface Platform {
    /** Human-readable OS/runtime, e.g. "Android 34". */
    val name: String

    /** App version as "name (code)", e.g. "1.1.91 (1001091)". */
    val appVersion: String
}

expect fun getPlatform(): Platform