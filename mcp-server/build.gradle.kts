plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadowPlugin)
    application
}

dependencies {
    implementation(libs.mcp.server)
    implementation(libs.ktor.server.cio)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.serializationJson)
    implementation(projects.shared)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "MainKt"
}

tasks.shadowJar {
    archiveFileName.set("serverAll.jar")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}
