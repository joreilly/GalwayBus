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
import android.view.View
import android.view.View.GONE
import android.widget.CheckBox
import android.widget.TextView
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.Constants
import com.surrus.galwaybus.base.R
import com.surrus.galwaybus.model.Bus
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.ui.data.ResourceState
import com.surrus.galwaybus.ui.viewmodel.BusInfoViewModel
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModel
import com.surrus.galwaybus.util.ext.observe
import kotlinx.android.synthetic.main.activity_bus_stop_list.*
import kotlinx.android.synthetic.main.bus_times_list_item.view.duration
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.koin.android.viewmodel.ext.android.viewModel


class BusStopListActivity : AppCompatActivity(), OnMapReadyCallback {

    val busStopsViewModel: BusStopsViewModel by viewModel()
    val busInfoViewModel: BusInfoViewModel by viewModel()

    private var routeId: String = ""
    private var routeName: String = ""
    private var schedulePdf: String = ""
    private var direction: Int = 0

    private var showStops: Boolean = false

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
            showStops = savedInstanceState.getBoolean(Constants.SHOW_STOPS)
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
            }
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        })

        showStopsCheckBox.setOnClickListener {
            showStops = ((it as CheckBox).isChecked)
            updateUI()
        }


        busStopsViewModel.fetchBusStops(routeId)

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
        outState.putBoolean(Constants.SHOW_STOPS, showStops)
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


        val busInfoWindowAdapter: BusInfoWindowAdapter = BusInfoWindowAdapter()
        map?.setInfoWindowAdapter(busInfoWindowAdapter)

        // default position until we have more data
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(53.2743394, -9.0514163), 12.0f))

        updateUI()

        busInfoViewModel.pollForBusLocations(routeId)
    }


    private fun updateUI() {
        busStopsViewModel.busStops.removeObservers(this)
        busInfoViewModel.busListForRoute.removeObservers(this)

        busStopsViewModel.busStops.observe(this) { busStopsList ->
            busInfoViewModel.busListForRoute.observe(this) { resource ->

                when (resource?.status) {
                    ResourceState.SUCCESS -> updateMap(busStopsList!!, resource.data!!)
                    ResourceState.ERROR -> {
                        Snackbar.make(this.rootLayout, resource.message as CharSequence, Snackbar.LENGTH_LONG).show()
                    }
                    ResourceState.LOADING -> TODO()
                }
            }
        }
    }

    private fun updateMap(busStopList: List<BusStop>, busListForRoute: List<Bus>) {

        map?.let {
            it.clear()
            val builder = LatLngBounds.Builder()

            for (busStop in busStopList) {
                val busStopLocation = LatLng(busStop.latitude, busStop.longitude)
                builder.include(busStopLocation)

                if (showStops) {
                    val icon = bitmapDescriptorFromVector(this, R.drawable.ic_stop, R.color.mapMarkerGreen)
                    val markerOptions = MarkerOptions()
                            .title(busStop.longName)
                            //.snippet(snippet)
                            .position(busStopLocation).icon(icon)

                    val marker = it.addMarker(markerOptions)
                    marker.tag = busStop
                }
            }


            Logger.d("JFOR: updateMap")
            for (bus in busListForRoute) {
                Logger.d("JFOR: updateMap, bus = $bus")

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

                val markerOptions = MarkerOptions()
                        .title(title)
                        .snippet(snippet)
                        .position(busStopLocation).icon(icon)
                val marker = it.addMarker(markerOptions)
                marker.tag = bus
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




    private inner class BusInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        val secondsFormatter: PeriodFormatter = PeriodFormatterBuilder()
                .printZeroAlways()
                .appendSeconds()
                .appendSuffix(" second", " seconds")
                .toFormatter()

        override fun getInfoContents(marker: Marker): View {
            val view = layoutInflater.inflate(R.layout.custom_map_info_contents, null)
            val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
            val subTitleTextView = view.findViewById<TextView>(R.id.subTitleTextView)
            val updatedWhenTextView = view.findViewById<TextView>(R.id.updatedWhenTextView)

            if (marker.tag is Bus) {
                val bus = marker.tag as Bus
                var title = ""
                var subTitle = ""
                if (bus.departure_metadata != null) {
                    title = "To ${bus.departure_metadata.destination} ($routeId)"
                    val delayMins = bus.departure_metadata.delay / 60
                    val minsString = getResources().getQuantityString(R.plurals.mins, delayMins)
                    subTitle = "Delay: $delayMins $minsString. Vehicle id: ${bus.vehicle_id}"
                } else {
                    title = "($routeId)"
                    subTitle = "Vehicle id: ${bus.vehicle_id}"
                }

                titleTextView.text = title
                subTitleTextView.text = subTitle


                val now = DateTime()
                val updateTime = DateTime(bus.modified_timestamp)
                val timeSinceUpdate = Period(updateTime, now)
                val seconds = timeSinceUpdate.seconds
                if (seconds >= 0) {
                    updatedWhenTextView.text = "Updated ${secondsFormatter.print(timeSinceUpdate)} ago"
                }
            } else if (marker.tag is BusStop) {
                val busStop = marker.tag as BusStop
                titleTextView.text = busStop.longName
                subTitleTextView.text = busStop.irishShortName
                updatedWhenTextView.text = busStop.stopRef
            }


            return view
        }

        override fun getInfoWindow(marker: Marker): View? {
            return null
        }
    }



    companion object {
        const val COORDINATE_OFFSET = 0.00002f
    }

}
