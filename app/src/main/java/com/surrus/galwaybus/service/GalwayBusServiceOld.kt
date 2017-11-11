package com.surrus.galwaybus.service

import com.surrus.galwaybus.remote.GalwayBusService
import javax.inject.Inject


open class GalwayBusServiceOld @Inject constructor(val galwayBusRestInterface: GalwayBusService) {

    //fun getRoutes()  = galwayBusRestInterface.getRoutes()

    //open fun getRoutes2() : RetrofitLiveData<Map<String, BusRoute>> = RetrofitLiveData(galwayBusRestInterface.getRoutes2())

    fun getStops(routeId: Int) {

/*
        galwayBusResetInterface.getStops(routeId,
                object : Callback<GetStopsResponse>() {
                    fun success(getStopsResponse: GetStopsResponse, response: Response) {
                        Log.d("GalwayBusService", "got stops")
                        bus!!.post(StopsLoadedEvent(getStopsResponse.stops))
                    }

                    fun failure(error: RetrofitError) {
                        Log.e("GalwayBusService", error.getMessage())
                    }
                }
        )
*/
    }


    fun getDepartures(stopRef: String) {
/*
        galwayBusResetInterface.getDepartures(stopRef,
                object : Callback<GetDeparturesResponse>() {
                    fun success(getDeparturesResponse: GetDeparturesResponse, response: Response) {
                        Log.d("GalwayBusService", "got departures")
                        bus!!.post(DeparturesLoadedEvent(getDeparturesResponse.departureTimes))
                    }

                    fun failure(error: RetrofitError) {
                        Log.e("GalwayBusService", error.getMessage())
                    }
                }
        )
*/

    }
}
