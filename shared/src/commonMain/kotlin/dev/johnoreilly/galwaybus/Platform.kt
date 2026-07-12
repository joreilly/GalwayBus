package dev.johnoreilly.galwaybus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform