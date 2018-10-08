package com.surrus.galwaybus.remote

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.surrus.galwaybus.utils.RestServiceTestHelper
import junit.framework.Assert.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.mockwebserver.MockResponse
import java.net.HttpURLConnection




@RunWith(JUnit4::class)
class GalwayBusMockWebTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mockResponse: MockResponse

    private lateinit var retrofit: Retrofit
    private lateinit var galwayBusService: GalwayBusService
    private lateinit var galwayBusRemoteImpl: GalwayBusRemoteImpl


    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
                .create()

        retrofit = Retrofit.Builder()
                .baseUrl(mockWebServer.url("").toString())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()

        galwayBusService =  retrofit.create(GalwayBusService::class.java)
        galwayBusRemoteImpl = GalwayBusRemoteImpl(galwayBusService)

        mockResponse = MockResponse()
        mockResponse.setResponseCode(HttpURLConnection.HTTP_OK)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetSchedules() = runBlocking {

        val json = RestServiceTestHelper.getStringFromFile("schedules.json")

        mockResponse.setBody(json)
        mockWebServer.enqueue(mockResponse)


        val scheduleMap = galwayBusRemoteImpl.getSchedules()
        for (schedule in scheduleMap.values) {
            assertNotNull(schedule.timetableId)
            assertNotNull(schedule.routeName)
            assertNotNull(schedule.pdfUrl)
        }
    }

    @Test
    fun testGetAllStops() = runBlocking {

        val json = RestServiceTestHelper.getStringFromFile("stops.json")

        mockResponse.setBody(json)
        mockWebServer.enqueue(mockResponse)

        val busStops = galwayBusRemoteImpl.getAllStops()
        for (busStop in busStops) {
            assertNotNull(busStop.stopId)
            assertNotNull(busStop.shortName)
            assertNotNull(busStop.stopRef)
            assertNotNull(busStop.latitude)
            assertNotNull(busStop.longitude)
        }

    }
}