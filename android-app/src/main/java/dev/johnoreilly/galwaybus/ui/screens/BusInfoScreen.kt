package dev.johnoreilly.galwaybus.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.BusStop
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel

@Composable
fun BusInfoScreen(viewModel: GalwayBusViewModel, popBack: () -> Unit) {
    val busInfoList by viewModel.busInfoList.collectAsState(emptyList())
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
                        BusInfoMapViewContainer(stop, busInfoList) //, mapView)
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
fun BusInfoMapViewContainer(stop: BusStop, busInfoList: List<Bus>) { //, map: MapView) {
    val context = LocalContext.current

    val builder = LatLngBounds.Builder()

    stop.latitude?.let { latitude ->
        stop.longitude?.let { longitude ->
            val busStopLocation = LatLng(latitude, longitude)
            builder.include(busStopLocation)
        }
    }

    busInfoList.forEach { bus ->
        val busLocation = LatLng(bus.latitude, bus.longitude)
        builder.include(busLocation)
    }

    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), 64)

    val cameraPositionState = rememberCameraPositionState {
        move(cameraUpdate)
    }

    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    val uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = true)) }
    GoogleMap(
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings
    ) {
        stop.latitude?.let { latitude ->
            stop.longitude?.let { longitude ->
                val busStopLocation = LatLng(latitude, longitude)
                val icon =
                    bitmapDescriptorFromVector(context, R.drawable.ic_stop, R.color.mapMarkerGreen)
                Marker(position = busStopLocation, title = stop.shortName, icon = icon, tag = stop)
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
                "Delay: $delayMins ${
                    context.resources.getQuantityString(
                        R.plurals.mins,
                        delayMins
                    )
                }"
            } else {
                ""
            }


            val icon = bitmapDescriptorFromVector(context, R.drawable.bus_side, tintColor)
            Marker(
                position = busLocation, title = title,
                snippet = snippet, icon = icon, tag = bus
            )
        }
    }
}
