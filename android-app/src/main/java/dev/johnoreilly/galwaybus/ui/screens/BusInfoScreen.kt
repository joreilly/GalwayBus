package dev.johnoreilly.galwaybus.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun BusInfoScreen(viewModel: GalwayBusViewModel, popBack: () -> Unit) {
    val busInfoList by viewModel.busInfoList.collectAsState(emptyList())
    val mapView = rememberMapViewWithLifecycle()
    val routeId = viewModel.routeId.value
    val currentBusStop = viewModel.currentBusStop.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(routeId ?: "") },
            navigationIcon = {
                IconButton(onClick = { popBack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { paddingValues ->

        Column(Modifier.padding(paddingValues)) {
            currentBusStop.value?.let { stop ->
                //viewModel.getBusStop(stopRef)?.let { stop ->
                    if (busInfoList.isNotEmpty()) {
                        BusInfoMapViewContainer(stop, busInfoList, mapView)
                    } else {
                        Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                            CircularProgressIndicator()
                        }
                    }
                //}
            }
        }

    }
}


@SuppressLint("MissingPermission")
@Composable
fun BusInfoMapViewContainer(stop: BusStop, busInfoList: List<Bus>, map: MapView) {
    var firstTimeShowingMap by remember { mutableStateOf(true)}

    AndroidView({ map }) { mapView ->
        mapView.getMapAsync { map ->
            map.isMyLocationEnabled = true
            map.uiSettings.isZoomControlsEnabled = true

            map.clear()
            val builder = LatLngBounds.Builder()

            stop.latitude?.let { latitude ->
                stop.longitude?.let { longitude ->
                    val busStopLocation = LatLng(latitude, longitude)

                    // bus stop marker
                    val icon = bitmapDescriptorFromVector(mapView.context, R.drawable.ic_stop, R.color.mapMarkerGreen)
                    val markerOptions = MarkerOptions()
                            .title(stop.shortName)
                            .position(busStopLocation)
                            .icon(icon)

                    val marker = map.addMarker(markerOptions)
                    marker.tag = stop
                    builder.include(busStopLocation)
                }
            }

            // bus markers
            for (bus in busInfoList) {
                val busLocation = LatLng(bus.latitude, bus.longitude)

                val tintColor = if (bus.direction == 1) {
                    R.color.direction1
                } else {
                    R.color.direction2
                }

                val title = if (bus.departure_metadata != null) {
                    bus.departure_metadata?.destination
                } else {
                    bus.vehicle_id
                }

                val snippet = if (bus.departure_metadata != null) {
                    val delayMins = bus.departure_metadata?.delay?.div(60) ?: 0
                    "Delay: $delayMins ${mapView.context.resources.getQuantityString(R.plurals.mins, delayMins)}"
                } else {
                    ""
                }


                val icon = bitmapDescriptorFromVector(mapView.context, R.drawable.bus_side, tintColor)
                val markerOptions = MarkerOptions()
                    .title(title)
                    .snippet(snippet)
                    .position(busLocation)
                    .icon(icon)

                val marker = map.addMarker(markerOptions)
                marker.tag = bus
                builder.include(busLocation)
            }

            if (firstTimeShowingMap) {
                firstTimeShowingMap = false
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))
            }
        }
    }
}
