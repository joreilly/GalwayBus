package com.surrus.galwaybus.ui

import android.Manifest
import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.View

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.single.BasePermissionListener
import com.surrus.galwaybus.R
import com.surrus.galwaybus.service.GalwayBusService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var parentLayout: View

    private lateinit var busTimesAdapter: BusTimesRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        parentLayout = findViewById<View>(android.R.id.content)

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : BasePermissionListener() {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                        mapFragment.getMapAsync(this@HomeActivity)
                    }

                }).check()

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap


        map.isMyLocationEnabled = true

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(53.27, -9.05)
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney))




        val galwayBusService = GalwayBusService()

        galwayBusService.routes
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe (
                        { busRoutes ->

                            Snackbar.make(parentLayout, "got data", Snackbar.LENGTH_LONG).show()


                            with (busTimesList) {
                                setHasFixedSize(true)
                                layoutManager = LinearLayoutManager(this@HomeActivity)
                                busTimesAdapter = BusTimesRecyclerViewAdapter()
                                busTimesAdapter.busRouteList = ArrayList(busRoutes.values)
                                adapter = busTimesAdapter
                            }

                        },
                        { e ->
                            Snackbar.make(parentLayout, e.message ?: "", Snackbar.LENGTH_LONG).show()
                        }
                )


    }
}
