package dev.johnoreilly.galwaybus.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.LatLngBounds
import com.google.android.libraries.maps.model.MarkerOptions
import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.BusStop
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.utils.rememberMapViewWithLifecycle
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel

@Composable
fun BusInfoScreen(modifier: Modifier, viewModel: GalwayBusViewModel) {
    val busInfoList by viewModel.busInfoList.observeAsState(emptyList())
    val mapView = rememberMapViewWithLifecycle()

    Column(modifier) {
        val stopRef = viewModel.stopRef.value
        stopRef?.let {
            viewModel.getBusStop(stopRef)?.let { stop ->
                if (busInfoList.isNotEmpty()) {
                    BusInfoMapViewContainer(stop, busInfoList, mapView)
                }
            }
        }
    }
}


@SuppressLint("MissingPermission")
@Composable
fun BusInfoMapViewContainer(stop: BusStop, busInfoList: List<Bus>, map: MapView) {
    AndroidView({ map }) { mapView ->
        mapView.getMapAsync { map ->
            map.isMyLocationEnabled = true
            map.uiSettings.isZoomControlsEnabled = true

            map.clear()
            val builder = LatLngBounds.Builder()

            for (bus in busInfoList) {
                val busStopLocation = LatLng(bus.latitude, bus.longitude)

                val tintColor = if (bus.direction == 1) {
                    R.color.direction1
                } else {
                    R.color.direction2
                }

                val icon = bitmapDescriptorFromVector(mapView.context, R.drawable.bus_side, tintColor)
                val markerOptions = MarkerOptions()
                    .title(bus.vehicle_id)
                    .position(busStopLocation)
                    .icon(icon)

                val marker = map.addMarker(markerOptions)
                marker.tag = bus
                builder.include(busStopLocation)
            }

            map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))
        }
    }
}
