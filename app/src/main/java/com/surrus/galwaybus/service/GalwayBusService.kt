package com.surrus.galwaybus.service

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.surrus.galwaybus.model.GetDeparturesResponse
import com.surrus.galwaybus.model.GetStopsResponse
import com.surrus.galwaybus.model.BusRoute
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


class GalwayBusService constructor() {

    private val galwayBusResetInterface: GalwayBusRestInterface

    init {
        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()

        val retrofit = Retrofit.Builder()
                .baseUrl("http://galwaybus.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        galwayBusResetInterface = retrofit.create(GalwayBusRestInterface::class.java)
    }


    fun getRoutes()  = galwayBusResetInterface.getRoutes()

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
