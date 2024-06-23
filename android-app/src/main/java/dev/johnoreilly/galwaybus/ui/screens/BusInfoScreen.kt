@file:OptIn(ExperimentalMaterial3Api::class)

package dev.johnoreilly.galwaybus.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.BusStop
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusInfoScreen(viewModel: GalwayBusViewModel,
                  popBack: () -> Unit, onBusSelected: (String) -> Unit) {
    val busInfoList by viewModel.busInfoList.collectAsState(emptyList())
    val routeId by viewModel.routeId.collectAsState()
    val currentBusStop = viewModel.currentBusStop.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(routeId ?: "") },
            navigationIcon = {
                IconButton(onClick = { popBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )},
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->

        Column(Modifier.padding(paddingValues)) {
            currentBusStop.value?.let { stop ->
                if (busInfoList.isNotEmpty()) {
                    BusInfoMapViewContainer(stop, busInfoList) { busId ->
                        coroutineScope.launch {
                            onBusSelected(busId)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                        CircularProgressIndicator()
                    }
                } }
        }
    }
}


@SuppressLint("MissingPermission")
@Composable
fun BusInfoMapViewContainer(stop: BusStop, busInfoList: List<Bus>, onBusSelected: (String) -> Unit) {
    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(stop) {
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

        cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))
    }


    LaunchedEffect(busInfoList) {
        println(busInfoList.map { it.vehicle_id })
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
                Marker(state = MarkerState(position = busStopLocation), title = stop.shortName, icon = icon, tag = stop)
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
                }, (${bus.vehicle_id})"
            } else {
                ""
            }

            val icon = bitmapDescriptorFromVector(context, R.drawable.bus_side, tintColor)
            MarkerInfoWindowContent(
                state = MarkerState(position = busLocation),
                title = title,
                snippet = snippet, icon = icon, tag = bus,
                onInfoWindowClick = {
                    onBusSelected(bus.vehicle_id)
                }
            ) { marker ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(marker.title!!)
                    Text(marker.snippet!!)
                }
            }
        }
    }
}
