package dev.johnoreilly.galwaybus.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import com.surrus.galwaybus.common.GalwayBusDeparture
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Location
import dev.johnoreilly.galwaybus.*
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.BusStopDeparture
import dev.johnoreilly.galwaybus.ui.typography
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import dev.johnoreilly.galwaybus.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("MissingPermission")
@Composable
fun NearestBusStopsScreen(bottomBar: @Composable () -> Unit, viewModel: GalwayBusViewModel, navController: NavHostController) {
    val mapView = rememberMapViewWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    val busStopState = viewModel.busStopListState.collectAsState(UiState.Loading)

    val favorites by viewModel.favorites.collectAsState(setOf())

    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

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

        ModalBottomSheetLayout(modifier = Modifier.padding(it), sheetState = sheetState, sheetContent = {
            DeparturesSheetContent(viewModel) {
                viewModel.setRouteId(it.timetableId)
                navController.navigate(Screens.BusInfoScreen.route)
            }
        }) {
            Column {
                val uiState = busStopState.value
                if (uiState is UiState.Success) {
                    Box(modifier = Modifier.weight(0.4f)) {
                        GoogleMapView(modifier = Modifier.fillMaxSize(), viewModel, uiState.data)
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

                                it.latitude?.let { latitude ->
                                    it.longitude?.let { longitude ->
                                        viewModel.setLocation(Location(latitude, longitude))
                                    }
                                }

                                coroutineScope.launch {
                                    sheetState.show()
                                    viewModel.setCurrentStop(it)
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
fun DeparturesSheetContent(viewModel: GalwayBusViewModel, departureSelected: (departure: GalwayBusDeparture) -> Unit)
{
    val departureList by viewModel.busDepartureList.collectAsState(emptyList())
    val busStop by viewModel.currentBusStop.collectAsState()

    Column(Modifier.defaultMinSize(minHeight = 200.dp)) {

        Text(text = busStop?.longName ?: "", style = typography.h6,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
        )

        LazyColumn {
            items(departureList) { departure ->
                BusStopDeparture(departure) { departure ->

                    val firebaseAnalytics = Firebase.analytics
                    firebaseAnalytics.logEvent("select_route_bus_positions") {
                        param("route", departure.timetableId)
                    }

                    departureSelected(departure)
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
            modifier = Modifier.clickable(onClick = { itemClick(stop) })
                    .padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
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


@Composable
private fun GoogleMapView(modifier: Modifier, viewModel: GalwayBusViewModel, stops: List<BusStop>) {
    val context = LocalContext.current
    val location by viewModel.location.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 15f)
    }

    LaunchedEffect(location) {
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        val position = cameraPositionState.position
        val isMoving = cameraPositionState.isMoving

        if (!isMoving) {
            val cameraLocation = Location(position.target.latitude, position.target.longitude)
            viewModel.setCameraPosition(cameraLocation)
            viewModel.setZoomLevel(cameraPositionState.position.zoom)
        }
    }


    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    val uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = true)) }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings
    ) {
        stops.forEach { stop ->
            val latitude = stop.latitude
            val longitude = stop.longitude
            if (latitude != null && longitude != null) {
                val busStopLocation = LatLng(latitude, longitude)
                val icon = bitmapDescriptorFromVector(context, R.drawable.ic_stop, R.color.mapMarkerGreen)
                Marker(state = MarkerState(position = busStopLocation), title = stop.shortName, icon = icon)
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

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
        }
    }

    // Makes MapView follow the lifecycle of this composable
    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> throw IllegalStateException()
            }
        }
    }


