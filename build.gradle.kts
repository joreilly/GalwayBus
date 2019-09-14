
buildscript {

    repositories {
        google()
        jcenter()
        maven(url = "https://maven.fabric.io/public")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
    }

    dependencies {
        classpath(BuildPlugins.androidGradlePlugin)
        classpath(BuildPlugins.kotlinGradlePlugin)
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
        classpath("com.google.gms:google-services:4.2.0")
        classpath("com.google.firebase:firebase-plugins:1.1.5")
        classpath("io.fabric.tools:gradle:1.31.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.nav}")
        classpath("com.squareup.sqldelight:gradle-plugin:${Versions.sqlDelight}")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "http://kotlin.bintray.com/kotlin-dev")
        maven(url = "https://dl.bintray.com/kotlin/ktor")
    }
}

tasks.register("clean").configure {
    delete("build")
}