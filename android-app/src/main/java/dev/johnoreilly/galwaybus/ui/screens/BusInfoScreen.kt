package dev.johnoreilly.galwaybus.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
    val busInfoList by viewModel.busInfoList.observeAsState(emptyList())
    val mapView = rememberMapViewWithLifecycle()
    val routeId = viewModel.routeId.value
    val stopRef = viewModel.stopRef.value

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(routeId ?: "") },
            navigationIcon = {
                IconButton(onClick = { popBack() }) {
                    Icon(Icons.Filled.ArrowBack)
                }
            }
        )
    }) { paddingValues ->

        Column(Modifier.padding(paddingValues)) {
            stopRef?.let {
                viewModel.getBusStop(stopRef)?.let { stop ->
                    if (busInfoList.isNotEmpty()) {
                        BusInfoMapViewContainer(stop, busInfoList, mapView)
                    } else {
                        Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                            CircularProgressIndicator()
                        }
                    }
                }
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

            if (firstTimeShowingMap) {
                firstTimeShowingMap = false
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))
            }
        }
    }
}
