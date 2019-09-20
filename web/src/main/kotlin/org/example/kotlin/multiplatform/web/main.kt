package org.example.kotlin.multiplatform.web

import com.surrus.galwaybus.common.GalwayBusRepository
import kotlinext.js.require
import react.dom.render
import kotlin.browser.document
import kotlin.browser.window

fun main() {
    val globalState = object {
        val r = GalwayBusRepository()
        //val networkRepository = makeRepository()
    }

    require("main.css")

    window.onload = {
        render(document.getElementById("content")) {
            app {
                attrs {
                    //networkRepository = globalState.networkRepository
                }
            }
        }
    }
}

