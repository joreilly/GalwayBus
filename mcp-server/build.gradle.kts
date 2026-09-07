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

// The shared module pulls the same lifecycle-runtime-compose jar in under two coordinates, which
// the `application` plugin's distTar/distZip refuse to package without being told what to do.
// Only the archive tasks need this; shadowJar does its own merging.
tasks.withType<AbstractArchiveTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.shadowJar {
    archiveFileName.set("serverAll.jar")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}
