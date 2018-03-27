package com.surrus.galwaybus.ui


import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.orhanobut.logger.Logger

import com.surrus.galwaybus.base.R
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.data.Resource
import com.surrus.galwaybus.ui.data.ResourceState
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModelFactory
import com.surrus.galwaybus.util.ext.observe
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_nearby.*
import javax.inject.Inject



class NearbyFragment : Fragment(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var map: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private var myLocationEnabled = false

    @Inject lateinit var nearestBusStopsViewModelFactory: NearestBusStopsViewModelFactory
    private lateinit var nearestBusStopsViewModel : NearestBusStopsViewModel

    private lateinit var busStopsAdapter: BusStopsRecyclerViewAdapter


    companion object {
        fun newInstance(): NearbyFragment {
            return NearbyFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("onCreate")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!);

        nearestBusStopsViewModel = ViewModelProviders.of(activity!!, nearestBusStopsViewModelFactory).get(NearestBusStopsViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d("onViewCreated")

        // initialize recycler view
        with (busStopsList) {
            layoutManager = LinearLayoutManager(context)
            layoutManager.setAutoMeasureEnabled(false)

            busStopsAdapter = BusStopsRecyclerViewAdapter {
                val latLng = LatLng(it.latitude, it.longitude)
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, nearestBusStopsViewModel.getZoomLevel()))
            }
            adapter = busStopsAdapter
        }


        if (progressBar != null) {
            progressBar.visibility = View.VISIBLE
        }



        // subscribe to updates
        nearestBusStopsViewModel.busStops.observe(this) {
            if (it != null) handleDataState(it)
        }

        // show map
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(this)
    }



    override fun onResume() {
        super.onResume();
        Logger.d("onResume")
        nearestBusStopsViewModel.pollForNearestBusStopTimes()
    }


     override fun onPause() {
        super.onPause();
        Logger.d("onPause")
        nearestBusStopsViewModel.stopPolling()
    }


    override fun onDestroy() {
        super.onDestroy();
        Logger.d("onDestroy")
    }


    private fun handleDataState(resource: Resource<List<BusStop>>) {

        if (progressBar != null) {
            progressBar.visibility = View.GONE
        }

        when (resource.status) {
            ResourceState.SUCCESS -> {
                busStopsAdapter.busStopList = resource.data!!
                busStopsAdapter.notifyDataSetChanged()
            }
            ResourceState.ERROR -> {
                Snackbar.make(this.view!!, resource.message as CharSequence, Snackbar.LENGTH_LONG).show()
            }
            ResourceState.LOADING -> TODO()
        }
    }


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        Logger.d("onAttach")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Logger.d("onCreateView")
        return inflater.inflate(R.layout.fragment_nearby, container, false)
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        Logger.d("onMapReady")
        map = googleMap
        googleMap.getUiSettings().isZoomControlsEnabled = true


        if (nearestBusStopsViewModel.getLocation() == null) {

            var loc = Location(53.2743394, -9.0514163) // default if we can't get location
            nearestBusStopsViewModel.setLocation(loc)
            nearestBusStopsViewModel.setCameraPosition(loc)

            Dexter.withActivity(activity)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(object : BasePermissionListener() {
                        @SuppressLint("MissingPermission")
                        override fun onPermissionGranted(response: PermissionGrantedResponse) {
                            googleMap.isMyLocationEnabled = true

                            fusedLocationClient.lastLocation.addOnSuccessListener { fusedLocation ->
                                if (fusedLocation != null) {
                                    val location = Location(fusedLocation.latitude, fusedLocation.longitude)
                                    nearestBusStopsViewModel.setLocation(location)
                                    nearestBusStopsViewModel.setCameraPosition(location)
                                }
                            }
                        }
                    }).check()
        } else {
            nearestBusStopsViewModel.setCameraPosition(nearestBusStopsViewModel.getLocation()!!)

            if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.isMyLocationEnabled = true
            }
        }



        nearestBusStopsViewModel.cameraPosition.observe(this) {
            setCamera(it!!, nearestBusStopsViewModel.getZoomLevel())
        }


        nearestBusStopsViewModel.busStops.observe(this) {
            if (it != null && it.status == ResourceState.SUCCESS) {
                updateMap(it.data!!)
            }
        }

        googleMap.setOnCameraIdleListener{
            val cameraPosition = googleMap.getCameraPosition()

            val location = Location(cameraPosition.target.latitude, cameraPosition.target.longitude)
            nearestBusStopsViewModel.setZoomLevel(cameraPosition.zoom)
            nearestBusStopsViewModel.setLocation(location)

            nearestBusStopsViewModel.pollForNearestBusStopTimes()

            // scroll to top of list
            busStopsList?.scrollToPosition(0)
        }

    }


    private fun setCamera(location: Location, zoomLevel: Float) {
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), zoomLevel))
    }


    private fun updateMap(busStopList: List<BusStop>) {

        if (map != null && busStopList.size > 0) {

            val builder = LatLngBounds.Builder()
            for (busStop in busStopList) {
                val busStopLocation = LatLng(busStop.latitude, busStop.longitude);
                val marker = map?.addMarker(MarkerOptions().position(busStopLocation).title(busStop.longName))
                marker?.tag = busStop.stopRef
                builder.include(busStopLocation)
            }
            //map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))


            map?.setOnInfoWindowClickListener {
                //val stopRef = it.tag as String
            }

            map?.setOnMarkerClickListener {
                //val stopRef = it.tag as String
                false
            }


        }
    }
}

