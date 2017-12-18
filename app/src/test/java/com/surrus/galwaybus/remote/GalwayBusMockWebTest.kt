package com.surrus.galwaybus.remote

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.surrus.galwaybus.model.RouteSchedule
import junit.framework.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.mockwebserver.MockResponse
import java.net.HttpURLConnection
import io.reactivex.subscribers.TestSubscriber




@RunWith(JUnit4::class)
class GalwayBusMockWebTest {

    private lateinit var testSubscriber: TestSubscriber<LinkedHashMap<String, List<Map<String, String>>>>

    private lateinit var testSubscriberGetSchedules: TestSubscriber<List<RouteSchedule>>

    private lateinit var mockWebServer: MockWebServer
    private lateinit var retrofit: Retrofit
    private lateinit var galwayBusService: GalwayBusService



    val schedulesResponseJson: String = "{\n" +
            "\"401\": [\n" +
            "{\n" +
            "\"Salthill - Eyre Square\": \"http://www.buseireann.ie/timetables/1425472464-401.pdf\"\n" +
            "}\n" +
            "],\n" +
            "\"402\": [\n" +
            "{\n" +
            "\"Merlin Park - Eyre Square - Seacrest\": \"http://www.buseireann.ie/timetables/1464192900-402.pdf\"\n" +
            "}\n" +
            "],\n" +
            "\"403\": [\n" +
            "{\n" +
            "\"Eyre Square - Castlepark\": \"http://www.buseireann.ie/timetables/1464193090-403.pdf\"\n" +
            "}\n" +
            "],\n" +
            "\"404\": [\n" +
            "{\n" +
            "\"Newcastle - Eyre Square - Oranmore\": \"http://www.buseireann.ie/timetables/1475580187-404.pdf\"\n" +
            "}\n" +
            "],\n" +
            "\"405\": [\n" +
            "{\n" +
            "\"Rahoon - Eyre Square - Ballybane\": \"http://www.buseireann.ie/timetables/1475580263-405.pdf\"\n" +
            "}\n" +
            "],\n" +
            "\"407\": [\n" +
            "{\n" +
            "\"Eyre Square - Bóthar an Chóiste and return\": \"http://www.buseireann.ie/timetables/1425472732-407.pdf\"\n" +
            "}\n" +
            "],\n" +
            "\"409\": [\n" +
            "{\n" +
            "\"Eyre Square - GMIT - Parkmore\": \"http://www.buseireann.ie/timetables/1475580323-409.pdf\"\n" +
            "}\n" +
            "]\n" +
            "}"

    @Before
    fun setup() {

        mockWebServer = MockWebServer()

        val response = MockResponse()
        response.setResponseCode(HttpURLConnection.HTTP_OK)
        response.setBody(schedulesResponseJson)
        mockWebServer.enqueue(response)

        mockWebServer.start()

        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
                .create()

        retrofit = Retrofit.Builder()
                .baseUrl(mockWebServer.url("").toString())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        galwayBusService =  retrofit.create(GalwayBusService::class.java)

        testSubscriber = TestSubscriber()

        testSubscriberGetSchedules = TestSubscriber()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetSchedules() {

        val galwayBusRemoteImpl = GalwayBusRemoteImpl(galwayBusService)

        galwayBusRemoteImpl.getSchedules().subscribe(testSubscriberGetSchedules)
        testSubscriberGetSchedules.assertNoErrors()
        testSubscriberGetSchedules.assertValueCount(1)

        val scheduleList = testSubscriberGetSchedules.values()[0]
        for (schedule in scheduleList) {
            assertNotNull(schedule.timetableId)
            assertNotNull(schedule.routeName)
            assertNotNull(schedule.pdfUrl)
        }
    }
}