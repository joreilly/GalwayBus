package com.surrus.galwaybus.remote

import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.GetDeparturesResponse
import com.surrus.galwaybus.model.GetStopsResponse
import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Path

interface GalwayBusService {

    @GET("/routes.json")
    fun getBusRoutes(): Flowable<LinkedHashMap<String, BusRoute>>

    @GET("/routes/{route_id}.json")
    fun getStops(@Path("route_id") routeId: Int, cb: Flowable<GetStopsResponse>)

    @GET("/stops/{stop_ref}.json")
    fun getDepartures(@Path("stop_ref") stopRef: String, cb: Flowable<GetDeparturesResponse>)
}