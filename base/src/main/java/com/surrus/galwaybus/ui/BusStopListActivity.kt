package com.surrus.galwaybus.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.surrus.galwaybus.Constants
import com.surrus.galwaybus.base.R
import com.surrus.galwaybus.model.Bus
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModel
import com.surrus.galwaybus.util.ext.observe
import kotlinx.android.synthetic.main.activity_bus_stop_list.*
import org.koin.android.viewmodel.ext.android.viewModel


class BusStopListActivity : AppCompatActivity(), OnMapReadyCallback {

    val busStopsViewModel: BusStopsViewModel by viewModel()

    private var routeId: String = ""
    private var routeName: String = ""
    private var schedulePdf: String = ""
    private var direction: Int = 0

    private var firstTimeShowingMap = true

    private var map: GoogleMap? = null
    private lateinit var pagerAdapter: SectionsPagerAdapter


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_stop_list)

        if (savedInstanceState != null) {
            routeId = savedInstanceState.getString(Constants.ROUTE_ID)
            routeName = savedInstanceState.getString(Constants.ROUTE_NAME)
            schedulePdf = savedInstanceState.getString(Constants.SCHEDULE_PDF)
        } else {
            routeId = intent.extras[Constants.ROUTE_ID] as String
            routeName = intent.extras[Constants.ROUTE_NAME] as String
            schedulePdf = intent.extras[Constants.SCHEDULE_PDF] as String
        }
        title = "$routeId - $routeName"

        mapView.onCreate(savedInstanceState)
        mapView.onResume()

        pagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        pager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(pager)

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageSelected(position: Int) {
                direction = position
                busStopsViewModel.setDirection(direction)
                busStopsViewModel.pollForBusLocations(routeId)
            }
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        })



        // TEMP
        busStopsViewModel.pollForBusLocations(routeId)

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : BasePermissionListener() {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        mapView.getMapAsync(this@BusStopListActivity)
                    }
                }).check()

    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(Constants.ROUTE_ID, routeId)
        outState.putString(Constants.ROUTE_NAME, routeName)
        outState.putString(Constants.SCHEDULE_PDF, schedulePdf)
        super.onSaveInstanceState(outState)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bus_stops_activity, menu)

        val scheduleMenuItem = menu.findItem(R.id.action_view_schedule)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            scheduleMenuItem.isVisible = true
        }

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_view_schedule -> {
                val intent = Intent(this, SchedulePdfActivity::class.java)
                intent.putExtra(Constants.ROUTE_ID, routeId)
                intent.putExtra(Constants.ROUTE_NAME, routeName)
                intent.putExtra(Constants.SCHEDULE_PDF, schedulePdf)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return StopsFragment.newInstance(routeId, position)
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return "Direction " + (position+1)
        }

    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.isMyLocationEnabled = true

        busStopsViewModel.busStops.observe(this) {

            val bustStopList = it
            busStopsViewModel.busListForRoute.observe(this) {
                updateMap(bustStopList!!, it!!)
            }
        }
    }



    private fun updateMap(busStopList: List<BusStop>, busListForRoute: List<Bus>) {

        map?.let {
            it.clear()

            val builder = LatLngBounds.Builder()

            for (busStop in busStopList) {
                val busStopLocation = LatLng(busStop.latitude, busStop.longitude)
                //map?.addMarker(MarkerOptions().position(busStopLocation).title(busStop.longName))
                builder.include(busStopLocation)
            }


            for (bus in busListForRoute) {
                var busStopLocation = LatLng(bus.latitude, bus.longitude)
                if (busAlreadyAtThislocation(bus, busListForRoute)) {
                    busStopLocation = LatLng(bus.latitude + COORDINATE_OFFSET, bus.longitude + COORDINATE_OFFSET)
                }

                val tintColor = if (bus.direction == 1) {
                    R.color.direction1
                } else {
                    R.color.direction2
                }

                val icon = bitmapDescriptorFromVector(this, R.drawable.bus_side, tintColor)
                var title = ""
                var snippet = ""
                if (bus.departure_metadata != null) {
                    title = "To ${bus.departure_metadata.destination} ($routeId)"
                    val delayMins = bus.departure_metadata.delay/60
                    val minsString = getResources().getQuantityString(R.plurals.mins, delayMins)
                    snippet = "Delay: $delayMins $minsString. Vehicle id: ${bus.vehicle_id}"
                } else {
                    title = "($routeId)"
                    snippet = "Vehicle id: ${bus.vehicle_id}"
                }

                val marker = MarkerOptions()
                        .title(title)
                        .snippet(snippet)
                        .position(busStopLocation).icon(icon)
                it.addMarker(marker)
                builder.include(busStopLocation)
            }

            if (firstTimeShowingMap) {
                firstTimeShowingMap = false
                it.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))
            }
        }
    }

    private fun busAlreadyAtThislocation(bus: Bus, busList: List<Bus>): Boolean {
        for (b in busList) {
            if (b.vehicle_id != bus.vehicle_id) {
                if (b.latitude == bus.latitude && b.longitude == bus.longitude) {
                    return true
                }
            }
        }
        return false
    }


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

    companion object {
        val COORDINATE_OFFSET = 0.00002f
    }

}
