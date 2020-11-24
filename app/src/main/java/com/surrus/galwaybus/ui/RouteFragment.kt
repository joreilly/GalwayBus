package com.surrus.galwaybus.ui


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import androidx.fragment.app.Fragment
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
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

import com.surrus.galwaybus.R
import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Result
import com.surrus.galwaybus.ui.data.ResourceState
import com.surrus.galwaybus.ui.viewmodel.BusInfoViewModel
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModel
import com.surrus.galwaybus.util.ext.observe
import kotlinx.android.synthetic.main.fragment_route.mapView
import kotlinx.android.synthetic.main.fragment_route.progressBar
import kotlinx.android.synthetic.main.fragment_route.rootLayout
import kotlinx.android.synthetic.main.fragment_route.pager
import kotlinx.android.synthetic.main.fragment_route.showStopsCheckBox
import kotlinx.android.synthetic.main.fragment_route.tabLayout
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel


class RouteFragment : Fragment(R.layout.fragment_route), OnMapReadyCallback {

    private val busStopsViewModel: BusStopsViewModel by sharedViewModel()
    private val busInfoViewModel: BusInfoViewModel by viewModel()

    private lateinit var routeId: String
    private lateinit var routeName: String

    private var direction: Int = 0

    private var firstTimeShowingMap = true

    private var map: GoogleMap? = null
    private lateinit var pagerAdapter: SectionsPagerAdapter
    private var progressCountdownTimer: CountDownTimer? = null

    private val params by navArgs<RouteFragmentArgs>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        routeId = params.routId
        routeName = params.routeName

        activity?.title = "$routeId - $routeName"

        mapView.onCreate(savedInstanceState)
        mapView.onResume()

        pagerAdapter = SectionsPagerAdapter(childFragmentManager)
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
            updateUI()
        }

        busStopsViewModel.setRouteId(routeId)

        Dexter.withActivity(activity)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : BasePermissionListener() {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        mapView.getMapAsync(this@RouteFragment)
                    }
                }).check()

    }


    override fun onResume() {
        super.onResume()
        busInfoViewModel.pollForBusLocations(routeId)
    }


    override fun onPause() {
        super.onPause()
        Logger.d("onPause")
        progressCountdownTimer?.cancel()
        busInfoViewModel.stopPolling()
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


        val busInfoWindowAdapter = BusInfoWindowAdapter()
        map?.setInfoWindowAdapter(busInfoWindowAdapter)

        // default position until we have more data
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(53.2743394, -9.0514163), 12.0f))

        updateUI()
    }



    private fun updateUI() {
        busStopsViewModel.busStops.removeObservers(this)

        busStopsViewModel.busStops.observe(this) { busStopsListResult ->

            when (busStopsListResult) {
                is Result.Success -> { updateRouteBusMap(busStopsListResult.data) }
                is Result.Error -> {
                    Snackbar.make(this.rootLayout, busStopsListResult.exception.message as CharSequence, Snackbar.LENGTH_LONG).show()
                }
            }

        }
    }


    private fun updateRouteBusMap(busStopList: List<BusStop>) {
        busInfoViewModel.busListForRoute.removeObservers(this)
        busInfoViewModel.busListForRoute.observe(this) { resource ->

            progressBar.max = BusInfoViewModel.POLL_INTERVAL.toInt()
            progressCountdownTimer?.cancel()
            progressCountdownTimer = object: CountDownTimer(BusInfoViewModel.POLL_INTERVAL, 500){
                override fun onTick(millisUntilFinished: Long){
                    progressBar.progress = BusInfoViewModel.POLL_INTERVAL.toInt() - millisUntilFinished.toInt()
                }
                override fun onFinish() {
                }
            }
            progressCountdownTimer?.start()

            when (resource?.status) {
                ResourceState.SUCCESS -> updateMap(busStopList, resource.data!!)
                ResourceState.ERROR -> {
                    Snackbar.make(this.rootLayout, resource.message as CharSequence, Snackbar.LENGTH_LONG).show()
                }
                ResourceState.LOADING -> TODO()
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

                if (showStopsCheckBox.isChecked) {
                    val icon = bitmapDescriptorFromVector(context!!, R.drawable.ic_stop, R.color.mapMarkerGreen)
                    val markerOptions = MarkerOptions()
                            .title(busStop.longName)
                            //.snippet(snippet)
                            .position(busStopLocation).icon(icon)

                    val marker = it.addMarker(markerOptions)
                    marker.tag = busStop
                }
            }


            Logger.d("updateMap, number of buses=${busListForRoute.size}")
            for (bus in busListForRoute) {
                Logger.d("updateMap, vid = ${bus.vehicle_id}, update time=${bus.modified_timestamp}, direction=${bus.direction}")

                var busStopLocation = LatLng(bus.latitude, bus.longitude)
                if (busAlreadyAtThislocation(bus, busListForRoute)) {
                    busStopLocation = LatLng(bus.latitude + COORDINATE_OFFSET, bus.longitude + COORDINATE_OFFSET)
                }

                val tintColor = if (bus.direction == 1) {
                    R.color.direction1
                } else {
                    R.color.direction2
                }

                val icon = bitmapDescriptorFromVector(context!!, R.drawable.bus_side, tintColor)
                var title: String
                var snippet: String
                if (bus.departure_metadata != null) {
                    title = "To ${bus.departure_metadata?.destination} ($routeId)"
                    val delayMins = bus.departure_metadata?.delay?.div(60) ?: 0
                    val minsString = resources.getQuantityString(R.plurals.mins, delayMins)
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

            if (firstTimeShowingMap && busStopList.isNotEmpty()) {
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
            val delayTextView = view.findViewById<TextView>(R.id.delayTextView)
            val departureTimeTextView = view.findViewById<TextView>(R.id.departureTimeTextView)
            val updatedWhenTextView = view.findViewById<TextView>(R.id.updatedWhenTextView)

            if (marker.tag is Bus) {
                val bus = marker.tag as Bus
                val title: String
                val subTitle: String
                if (bus.departure_metadata != null) {
                    title = "To ${bus.departure_metadata?.destination} ($routeId)"
                    val delayMins = bus.departure_metadata?.delay?.div(60) ?: 0
                    val minsString = resources.getQuantityString(R.plurals.mins, delayMins)

                    delayTextView.visibility = VISIBLE
                    // TODO use string resource
                    delayTextView.text = "Delay: $delayMins $minsString"
                } else {
                    title = "($routeId)"
                    delayTextView.visibility = GONE
                }
                titleTextView.text = title


                if (!bus.route.isNullOrEmpty()) {
                    val timestampList = bus.route?.toList()?.sortedBy { it.first }
                    if (!timestampList.isNullOrEmpty()) {
                        val date = DateTime(timestampList[0].first.toLong() * 1000)
                        val departureTimeString = date.toString(DateTimeFormat.shortTime())

                        departureTimeTextView.visibility = VISIBLE
                        // TODO use string resource
                        departureTimeTextView.text = "Departure Time: $departureTimeString"
                    }
                } else {
                    departureTimeTextView.visibility = GONE
                }


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
                departureTimeTextView.text = busStop.stopRef
                updatedWhenTextView.visibility = GONE
            }


            return view
        }

        override fun getInfoWindow(marker: Marker): View? {
            return null
        }
    }



    companion object {
        const val COORDINATE_OFFSET = 0.00002f

        fun bundleArgs(routeId: String, routeName: String): Bundle {
            return Bundle().apply {
                this.putString(Constants.ROUTE_ID, routeId)
                this.putString(Constants.ROUTE_NAME, routeName)
            }
        }
    }
}
