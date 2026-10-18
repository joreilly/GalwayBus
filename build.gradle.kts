plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
// Ktor's JS/wasm client pins ws 8.20.1 (still on Ktor main), which is in the range of a ws security
// advisory; Dependabot can't bump it because kotlin-js-store/wasm has a lockfile but no package.json.
// The browser app never loads ws (it uses the browser's own WebSocket), but override it to a fixed
// release so the lock stops carrying the vulnerable version. Drop this once Ktor bumps ws itself.
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension>().resolution("ws", "8.22.0")
}
