package com.surrus.galwaybus.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager

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
import com.surrus.galwaybus.ui.viewmodel.HomeViewModel
import com.surrus.galwaybus.ui.viewmodel.HomeViewModelFactory
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject


class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    @Inject lateinit var viewModelFactory: HomeViewModelFactory
    private lateinit var viewModel : HomeViewModel

    private lateinit var map: GoogleMap

    private lateinit var busTimesAdapter: BusTimesRecyclerViewAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        AndroidInjection.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HomeViewModel::class.java)


        with (busTimesList) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@HomeActivity)
            busTimesAdapter = BusTimesRecyclerViewAdapter()
            adapter = busTimesAdapter
        }

        viewModel.getBusRoutes().observe(this) {
            busTimesAdapter.busRouteList = ArrayList(it)
            busTimesAdapter.notifyDataSetChanged()
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


        // TEMP location for now
        val sydney = LatLng(53.27, -9.05)
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

}
