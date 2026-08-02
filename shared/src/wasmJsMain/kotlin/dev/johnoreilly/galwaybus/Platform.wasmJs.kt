package dev.johnoreilly.galwaybus

class WasmPlatform : Platform {
    override val name: String = "Web (Wasm)"
    override val appVersion: String = "web build"
}

actual fun getPlatform(): Platform = WasmPlatform()
