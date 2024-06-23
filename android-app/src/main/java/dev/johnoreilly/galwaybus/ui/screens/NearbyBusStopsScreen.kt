@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package dev.johnoreilly.galwaybus.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.ColorRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.clustering.ClusterItem
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.GalwayBusDeparture
import com.surrus.galwaybus.common.model.Location
import dev.johnoreilly.galwaybus.*
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.BusStopDeparture
import dev.johnoreilly.galwaybus.ui.theme.typography
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import dev.johnoreilly.galwaybus.ui.viewmodel.UiState
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)
@SuppressLint("MissingPermission", "CoroutineCreationDuringComposition")
@Composable
fun NearestBusStopsScreen(viewModel: GalwayBusViewModel, navController: NavHostController) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val busStopState = viewModel.busStopListState.collectAsState(UiState.Loading)
    val favorites by viewModel.favorites.collectAsState(setOf())
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.app_name))
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = { viewModel.centerInEyreSquare() }) {
                        Icon(Icons.Filled.Home, contentDescription = "Center in Eyre Square")
                    }
                }
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) {

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
                    when (uiState) {
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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentSize(Alignment.Center)
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

        Text(text = busStop?.longName ?: "", style = typography.headlineSmall,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
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
                    favorites: Set<String>, stopSelected : (stop : BusStop) -> Unit) {
    LazyColumn {
        items(busStopList) { stop ->
            BusStopView(stop = stop,
                    stopSelected = stopSelected,
                    isFavorite = favorites.contains(stop.stopRef),
                    onToggleFavorite = {
                        viewModel.toggleFavorite(stop.stopRef)
                    }
            )
        }
    }
}

@Composable
fun BusStopView(stop: BusStop, stopSelected : (stop : BusStop) -> Unit, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    val headlineText = "${stop.longName} (${stop.stopRef})"
    val supportingText = stop.routes.joinToString()

    if (supportingText.isNotEmpty()) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = { stopSelected(stop) }),
            headlineContent = { Text(headlineText, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(supportingText) },
            trailingContent = {
                FavoritesButton(isFavorite = isFavorite, onClick = onToggleFavorite)
            }
        )
    } else {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = { stopSelected(stop) }),
            headlineContent = { Text(headlineText) },
            trailingContent = {
                FavoritesButton(isFavorite = isFavorite, onClick = onToggleFavorite)
            }
        )
    }
}


data class BusStopPositionClusterItem(
    val itemPosition: LatLng,
    val itemTitle: String,
    val itemSnippet: String,
) : ClusterItem {
    override fun getPosition(): LatLng = itemPosition
    override fun getTitle(): String = itemTitle
    override fun getSnippet(): String = itemSnippet
    override fun getZIndex(): Float? {
        return null
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

        Clustering(
            items = stops.map { stop ->
                val latitude = stop.latitude
                val longitude = stop.longitude
                val busStopLocation = LatLng(latitude!!, longitude!!)
                BusStopPositionClusterItem(busStopLocation, stop.shortName, "Snippet")
            },
            clusterContent = { cluster ->
                Surface(
                    Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color.Blue,
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "%,d".format(cluster.size),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        )
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
