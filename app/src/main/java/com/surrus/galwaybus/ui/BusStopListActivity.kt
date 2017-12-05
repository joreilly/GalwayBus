package com.surrus.galwaybus.ui

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.surrus.galwaybus.Constants
import com.surrus.galwaybus.R
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModelFactory
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModelFactory
import com.surrus.galwaybus.util.ext.observe
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_bus_stop_list.*
import javax.inject.Inject


class BusStopListActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    private var routeId: String = ""

    @Inject lateinit var busStopsViewModelFactory: BusStopsViewModelFactory

    private lateinit var busStopsViewModel : BusStopsViewModel
    private lateinit var busStopsAdapter: BusStopsRecyclerViewAdapter

    @Inject lateinit var nearestBusStopsViewModelFactory: NearestBusStopsViewModelFactory
    private lateinit var nearestBusStopsViewModel : NearestBusStopsViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.surrus.galwaybus.R.layout.activity_bus_stop_list)
        AndroidInjection.inject(this)

        busStopsViewModel = ViewModelProviders.of(this, busStopsViewModelFactory).get(BusStopsViewModel::class.java)

        nearestBusStopsViewModel = ViewModelProviders.of(this, nearestBusStopsViewModelFactory).get(NearestBusStopsViewModel::class.java)


        if (savedInstanceState != null) {
            routeId = savedInstanceState.getString(Constants.ROUTE_ID)
        } else {
            routeId = intent.extras[Constants.ROUTE_ID] as String
        }
        setTitle(routeId)



        with (busStopsList) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@BusStopListActivity)

            busStopsAdapter = BusStopsRecyclerViewAdapter()
            adapter = busStopsAdapter
        }


        nearestBusStopsViewModel.busStops.observe(this) {
            busStopsAdapter.busStopList = it!!
            busStopsAdapter.notifyDataSetChanged()
        }

        val location = Location(53.258f, -9.058f)
        nearestBusStopsViewModel.fetchNearestBusStops(location)


/*
        busStopsViewModel.busStops.observe(this) {
            busStopsAdapter.busStopList = it!!.get(1)
            busStopsAdapter.notifyDataSetChanged()
        }

        busStopsViewModel.fetchBusStops(routeId)
*/
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : BasePermissionListener() {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                        mapFragment.getMapAsync(this@BusStopListActivity)
                    }
                }).check()
    }


    protected override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(Constants.ROUTE_ID, routeId)
        super.onSaveInstanceState(outState)
    }


    private fun updateMap(busStopList: List<BusStop>) {

        if (map != null && busStopList.size > 0) {

            val builder = LatLngBounds.Builder()
            for (busStop in busStopList) {
                val busStopLocation = LatLng(busStop.latitude, busStop.longitude);
                map.addMarker(MarkerOptions().position(busStopLocation).title(busStop.shortName))
                builder.include(busStopLocation)
            }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.isMyLocationEnabled = true


        busStopsViewModel.busStops.observe(this) {
            updateMap(it!!.get(0))
        }
    }

}
