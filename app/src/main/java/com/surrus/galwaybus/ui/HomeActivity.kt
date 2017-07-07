package com.surrus.galwaybus.ui

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.LifecycleActivity
import android.arch.lifecycle.LifecycleRegistryOwner
import android.os.Bundle
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
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import com.surrus.galwaybus.util.ext.observe
import com.surrus.galwaybus.viewmodel.HomeViewModel
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : AppCompatActivity(), LifecycleRegistryOwner, OnMapReadyCallback {

    private val lifecycleRegistry by lazy { android.arch.lifecycle.LifecycleRegistry(this) }
    private lateinit var viewModel : HomeViewModel

    private lateinit var map: GoogleMap
    private lateinit var parentLayout: View

    private lateinit var busTimesAdapter: BusTimesRecyclerViewAdapter


    override fun getLifecycle() = lifecycleRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        parentLayout = findViewById<View>(android.R.id.content)

        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java!!)

        viewModel.routes.observe(this) {
            with (busTimesList) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(this@HomeActivity)
                busTimesAdapter = BusTimesRecyclerViewAdapter()
                busTimesAdapter.busRouteList = ArrayList(it?.values)
                adapter = busTimesAdapter
            }

        }


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
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.isMyLocationEnabled = true

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(53.27, -9.05)
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

}
