package com.surrus.galwaybus.ui


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.*
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.annotation.ColorRes
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.orhanobut.logger.Logger

import com.surrus.galwaybus.R
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Location
//import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.ui.data.Resource
import com.surrus.galwaybus.ui.data.ResourceState
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import com.surrus.galwaybus.util.ext.observe
import kotlinx.android.synthetic.main.fragment_nearby.*
import org.koin.android.viewmodel.ext.android.sharedViewModel


class NearbyFragment : Fragment(R.layout.fragment_nearby), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var map: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null

    val nearestBusStopsViewModel: NearestBusStopsViewModel by sharedViewModel()

    private lateinit var busStopsAdapter: BusStopsRecyclerViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("onCreate")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d("onViewCreated")

        activity?.title = getString(R.string.app_name)

        setHasOptionsMenu(true)

        // initialize recycler view
        with (busStopsList) {
            layoutManager = LinearLayoutManager(context)
            layoutManager!!.setAutoMeasureEnabled(false)

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
        super.onResume()
        Logger.d("onResume")
        nearestBusStopsViewModel.pollForNearestBusStopTimes()
    }


     override fun onPause() {
        super.onPause()
        Logger.d("onPause")
        nearestBusStopsViewModel.stopPolling()
    }


    override fun onDestroy() {
        super.onDestroy()
        Logger.d("onDestroy")
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main, menu);
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



    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        Logger.d("onMapReady")
        map = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true


        if (nearestBusStopsViewModel.getLocation() == null) {

            val loc = Location(53.2743394, -9.0514163) // default if we can't get location
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
            val cameraPosition = googleMap.cameraPosition

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

        if (map != null && busStopList.isNotEmpty()) {

            val builder = LatLngBounds.Builder()
            for (busStop in busStopList) {
                val busStopLocation = LatLng(busStop.latitude, busStop.longitude)


                val icon = bitmapDescriptorFromVector(activity!!, R.drawable.ic_stop, R.color.mapMarkerGreen)

                val markerOptions = MarkerOptions()
                        .title(busStop.longName)
                        .position(busStopLocation).icon(icon)

                val marker = map?.addMarker(markerOptions)
                marker?.tag = busStop
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


    // TODO move this in to common code
    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int, @ColorRes tintColor: Int? = null): BitmapDescriptor? {

        // retrieve the actual drawable
        val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bm = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)

        // add the tint if it exists
        tintColor?.let {
            DrawableCompat.setTint(drawable, ContextCompat.getColor(context, it))
        }
        // draw it onto the bitmap
        val canvas = Canvas(bm)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bm)
    }
}

