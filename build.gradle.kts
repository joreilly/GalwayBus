
buildscript {

    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("xml-apis:xml-apis:1.4.01")
        classpath("com.android.tools.build:gradle:7.0.0-alpha09")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
        classpath("com.squareup.sqldelight:gradle-plugin:${Versions.sqlDelight}")
        classpath("com.google.gms:google-services:4.3.4")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://dl.bintray.com/ekito/koin")
        maven(url = "https://kotlin.bintray.com/kotlin-js-wrappers/")
        maven(url = "https://jitpack.io")
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }
}

