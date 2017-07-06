package com.surrus.galwaybus.model

class GetStopsResponse {

    var route: BusRoute? = null
        internal set
    var stops: List<List<BusStop>>? = null
        internal set
}
