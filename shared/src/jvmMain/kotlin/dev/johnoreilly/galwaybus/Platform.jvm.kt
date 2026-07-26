package dev.johnoreilly.galwaybus

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    // Desktop has no packaged version; the launcher may set -Dapp.version, else it's a dev run.
    override val appVersion: String = System.getProperty("app.version") ?: "dev build"
}

actual fun getPlatform(): Platform = JVMPlatform()
