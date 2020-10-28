
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
        classpath("xml-apis:xml-apis:1.4.01")
        classpath("com.android.tools.build:gradle:4.2.0-alpha15")
        classpath(BuildPlugins.kotlinGradlePlugin)
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
        classpath("com.google.gms:google-services:4.3.3")
        classpath("com.google.firebase:firebase-plugins:2.0.0")
        classpath("io.fabric.tools:gradle:1.31.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.nav}")
        classpath("com.squareup.sqldelight:gradle-plugin:${Versions.sqlDelight}")
        classpath("co.touchlab:kotlinxcodesync:0.1.5")
        classpath("com.google.cloud.tools:appengine-gradle-plugin:${Versions.appengine}")
        classpath("org.jetbrains.kotlin:kotlin-frontend-plugin:${Versions.kotlinFrontendPlugin}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://dl.bintray.com/kotlin/squash")
        maven(url = "https://dl.bintray.com/kotlin/kotlinx.html")
        maven(url = "https://dl.bintray.com/kotlin/ktor")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://kotlin.bintray.com/kotlin-dev")
        maven(url = "https://kotlin.bintray.com/kotlin-js-wrappers")
        maven( url = "https://dl.bintray.com/ekito/koin")
        maven( url = "https://dl.bintray.com/touchlabpublic/kotlin")
        maven( url = "https://jitpack.io" )
    }
}

