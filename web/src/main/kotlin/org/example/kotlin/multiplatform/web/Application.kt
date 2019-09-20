package org.example.kotlin.multiplatform.web

import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.remote.GalwayBusApi
import kotlinext.js.getOwnPropertyNames
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RComponent
import react.RHandler
import react.RProps
import react.RState
import react.child
import react.dom.button
import react.dom.div
import react.dom.h3
import react.dom.p
import react.functionalComponent
import react.useState



class Application : RComponent<ApplicationProps, RState>() {

    override fun RBuilder.render() {
        div(classes = "container") {
            div(classes = "header clearfix") {
                h3 { +"Galway Bus Stops" }
            }
            child(busStopList(), props = props)
        }
    }
}



fun busStopList() = functionalComponent<ApplicationProps> { props ->
    val (busStops, setBusStops) = useState(null as List<BusStop>?)

    div {
        button(classes = "btn btn-primary") {
            +"Fetch"
            attrs {
                onClickFunction = {
                    val mainScope = MainScope()
                    mainScope.launch {
                        val api = GalwayBusApi("http://localhost:8080")
                        val stops = api.fetchBusStops()
                        setBusStops(stops)
                    }
                }
            }
        }

        busStops?.forEach {
            p {
                +"${it.long_name}: ${it.irish_long_name}"
            }
        }
    }
}



external interface ApplicationProps : RProps {
}

fun RBuilder.app(handler: RHandler<ApplicationProps>) = child(Application::class, handler)
