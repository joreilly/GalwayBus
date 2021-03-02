package dev.johnoreilly.galwaybus.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.ColorRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.BitmapDescriptor
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Location
import dev.johnoreilly.galwaybus.*
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.BusStopDeparture
import dev.johnoreilly.galwaybus.ui.typography
import dev.johnoreilly.galwaybus.ui.utils.rememberMapViewWithLifecycle
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import dev.johnoreilly.galwaybus.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@SuppressLint("MissingPermission")
@Composable
fun NearestBusStopsScreen(bottomBar: @Composable () -> Unit, viewModel: GalwayBusViewModel, navController: NavHostController) {
    val mapView = rememberMapViewWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)

    val departureList by viewModel.busDepartureList.observeAsState(emptyList())
    val busStopState = viewModel.busStopListState.observeAsState(UiState.Loading)

    val favorites by viewModel.favorites.collectAsState(setOf())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Galway Bus") },
                actions = {
                    IconButton(onClick = { viewModel.centerInEyreSquare() }) {
                        Icon(Icons.Filled.Home, contentDescription = "Center in Eyre Square")
                    }
                }
            )
        },
        bottomBar = bottomBar)
    {

        BottomDrawer(
                drawerState = drawerState,
                drawerShape = RoundedCornerShape(16.dp),
                drawerContent = {
                    Text(text = "Departures", style = typography.h6,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center
                    )

                    LazyColumn {
                        items(departureList) { departure ->
                            BusStopDeparture(departure) {

                                val firebaseAnalytics = Firebase.analytics
                                firebaseAnalytics.logEvent("select_route_bus_positions") {
                                    param("route", it.timetableId)
                                }

                                viewModel.setRouteId(departure.timetableId)
                                navController.navigate(Screens.BusInfoScreen.route)
                            }
                        }
                    }
                }
        ) {
            Column {
                val uiState = busStopState.value
                if (uiState is UiState.Success) {
                    Box(modifier = Modifier.weight(0.4f)) {
                        MapViewContainer(viewModel, uiState.data, mapView)
                    }
                }


                Box(modifier = Modifier.weight(0.6f)) {
                    when (val uiState = busStopState.value) {
                        is UiState.Success -> {
                            BusStopListView(viewModel, uiState.data, favorites) {

                                val firebaseAnalytics = Firebase.analytics
                                firebaseAnalytics.logEvent("select_stop") {
                                    param("stop_name", it.longName)
                                }

                                viewModel.setLocation(Location(it.latitude, it.longitude))
                                viewModel.setStopRef(it.stopRef)
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            }
                        }
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is UiState.Error -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error retrieving bus stop info"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusStopListView(viewModel: GalwayBusViewModel, busStopList: List<BusStop>,
                    favorites: Set<String>, itemClick : (stop : BusStop) -> Unit) {
    LazyColumn {
        items(busStopList) { stop ->
            BusStopView(stop = stop,
                    itemClick = itemClick,
                    isFavorite = favorites.contains(stop.stopRef),
                    onToggleFavorite = {
                        viewModel.toggleFavorite(stop.stopRef)
                    }
            )
        }
    }
}

@Composable
fun BusStopView(stop: BusStop, itemClick : (stop : BusStop) -> Unit, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    Row(
            modifier = Modifier.clickable(onClick = { itemClick(stop) }).padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
    ) {

        Image(painterResource(R.drawable.ic_bus), modifier = Modifier.size(32.dp), contentDescription = "Bus")

        Spacer(modifier = Modifier.size(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = stop.longName, style = TextStyle(fontSize = 18.sp))
            Text(text = stop.stopRef, style = TextStyle(color = Color.DarkGray, fontSize = 12.sp))
        }
        FavoritesButton(
            isFavorite = isFavorite,
            onClick = onToggleFavorite
        )
    }
}


@SuppressLint("MissingPermission")
@Composable
fun MapViewContainer(viewModel: GalwayBusViewModel, stops: List<BusStop>, map: MapView) {
    val currentLocation = viewModel.location.observeAsState()

    AndroidView({ map }) { mapView ->
        mapView.getMapAsync { map ->
            map.isMyLocationEnabled = true
            map.uiSettings.isZoomControlsEnabled = true

            currentLocation.value?.let {
                val position = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(position,  viewModel.getZoomLevel()))
            }

            map.setOnCameraIdleListener{
                val cameraPosition = map.cameraPosition
                val location = Location(cameraPosition.target.latitude, cameraPosition.target.longitude)
                viewModel.setLocation(location)
                viewModel.setZoomLevel(cameraPosition.zoom)
            }

            for (busStop in stops) {
                val busStopLocation = LatLng(busStop.latitude.toDouble(), busStop.longitude.toDouble())

                val icon = bitmapDescriptorFromVector(mapView.context, R.drawable.ic_stop, R.color.mapMarkerGreen)
                val markerOptions = MarkerOptions()
                        .title(busStop.shortName)
                        .position(busStopLocation)
                        .icon(icon)

                val marker = map.addMarker(markerOptions)
                marker.tag = busStop
            }
        }
    }
}


// TODO move this in to common code
fun bitmapDescriptorFromVector(context: Context, vectorResId: Int, @ColorRes tintColor: Int? = null): BitmapDescriptor? {

    // retrieve the actual drawable
    val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    val bm = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)

    // add the tint if it exists
    tintColor?.let {
        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, it))
    }
    // draw it onto the bitmap
    val canvas = android.graphics.Canvas(bm)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bm)
}



