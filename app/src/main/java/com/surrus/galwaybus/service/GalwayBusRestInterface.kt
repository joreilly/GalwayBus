package com.surrus.galwaybus.service

import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.GetDeparturesResponse
import com.surrus.galwaybus.model.GetStopsResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface GalwayBusRestInterface {

    @GET("/routes.json")
    fun getRoutes(): Observable<Map<String, BusRoute>>


    @GET("/routes/{route_id}.json")
    fun getStops(@Path("route_id") routeId: Int, cb: Observable<GetStopsResponse>)

    @GET("/stops/{stop_ref}.json")
    fun getDepartures(@Path("stop_ref") stopRef: String, cb: Observable<GetDeparturesResponse>)
}