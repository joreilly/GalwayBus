package com.surrus.galwaybus.backend

import io.ktor.http.ContentType
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.routing.Route
import io.ktor.routing.accept

fun Route.index() {
    static("frontend") {
        resource("web.bundle.js")
    }

    accept(ContentType.Text.Html) {
        defaultResource("index.html")
    }
}
