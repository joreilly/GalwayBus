package dev.johnoreilly.galwaybus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text
import androidx.compose.material.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.BitmapDescriptor
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Location
import dev.johnoreilly.galwaybus.ui.*
import dev.johnoreilly.galwaybus.ui.utils.*
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import dev.johnoreilly.galwaybus.ui.viewmodel.UiState
import kotlinx.coroutines.flow.collect
import org.koin.android.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private val galwayBusViewModel by viewModel<GalwayBusViewModel>()

    @ExperimentalComposeApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GalwayBusTheme {
                val fusedLocationWrapper = fusedLocationWrapper()

                val fineLocation = checkSelfPermissionState(this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                MainLayout(fineLocation, fusedLocationWrapper, galwayBusViewModel)
            }
        }
    }
}


@SuppressLint("MissingPermission")
@Composable
fun MainLayout(fineLocation: PermissionState,
               fusedLocationWrapper: FusedLocationWrapper,
               viewModel: GalwayBusViewModel
) {
    var bottomBarSelectedIndex by remember { mutableStateOf(0) }
    val busStopState = viewModel.uiState.observeAsState(UiState.Loading)

    val hasLocationPermission by fineLocation.hasPermission.collectAsState()

    Scaffold(
            topBar = {
                TopAppBar(title = { Text("Galway Bus") },
                    actions = {
                        IconButton(onClick = { viewModel.centerInEyreSquare() }) {
                            Icon(Icons.Filled.Home)
                        }
                    }
                )
            },
            bodyContent = {
                if (hasLocationPermission) {

                    LaunchedEffect(fusedLocationWrapper) {
                        fusedLocationWrapper.lastLocation().collect {
                            val loc = Location(it.latitude, it.longitude)
                            viewModel.setLocation(loc)
                            viewModel.getNearestStops(loc)
                        }
                    }

                    when (bottomBarSelectedIndex) {
                        0 -> {
                            BusStopListBody(viewModel, busStopState)
                        }
                        1 -> {
                            Text("Starred items")
                        }
                    }
                } else {
                    fineLocation.launchPermissionRequest()
                }
            },
            bottomBar = {
                BottomAppBar {
                    BottomNavigationItem(icon = { Icon(Icons.Default.LocationOn) }, label = { Text("Nearby") },
                            selected = bottomBarSelectedIndex == 0,
                            onClick = { bottomBarSelectedIndex = 0 })

                    BottomNavigationItem(icon = { Icon(Icons.Default.Favorite) }, label = { Text("Favorites") },
                            selected = bottomBarSelectedIndex == 1,
                            onClick = { bottomBarSelectedIndex = 1 })
                }
            }
    )
}


@SuppressLint("MissingPermission")
@Composable
fun BusStopListBody(viewModel: GalwayBusViewModel, busStopState: State<UiState<List<BusStop>>>) {
    val mapView = rememberMapViewWithLifecycle()

    var sheetState by remember { mutableStateOf(BottomSheetState(show = true)) }
    var drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)

    val departureList by viewModel.busDepartureList.observeAsState(emptyList())

    val favorites by viewModel.favorites.collectAsState(setOf())

    BottomDrawerLayout(
        drawerState = drawerState,
        drawerShape = RoundedCornerShape(16.dp),
        drawerContent = {
            Text(text = "Departures", style = typography.h6,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            LazyColumnFor(items = departureList, itemContent = { departure ->
                BusStopDeparture(departure)
            })
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
                        LazyColumnFor(items = uiState.data, itemContent = { stop ->
                            StopViewRow(stop = stop,
                                itemClick = {
                                    viewModel.setLocation(Location(it.latitude, it.longitude))
                                    viewModel.setStopRef(it.stopRef)
                                    sheetState = sheetState.copy(show = true)
                                    drawerState.open()
                                },
                                isFavorite = favorites.contains(stop.stopRef),
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(stop.stopRef)
                                }
                            )
                        })
                    }
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Snackbar(text = { Text("Error retrieving bus stop info") })
                    }
                }
            }
        }
    }
}

@Composable
fun StopViewRow(stop: BusStop, itemClick : (stop : BusStop) -> Unit, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = { itemClick(stop) }).padding(8.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(imageResource(R.drawable.ic_bus), modifier = Modifier.preferredSize(32.dp))

        Spacer(modifier = Modifier.preferredSize(16.dp))

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
fun FavoritesButton(
        isFavorite: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    IconToggleButton(
            checked = isFavorite,
            onCheckedChange = { onClick() },
            modifier = modifier
    ) {
        if (isFavorite) {
            Icon(imageVector = Icons.Filled.Favorite, tint = maroon500)
        } else {
            Icon(imageVector = Icons.Filled.FavoriteBorder, tint = maroon500)
        }
    }
}


@SuppressLint("MissingPermission")
@Composable
private fun MapViewContainer(viewModel: GalwayBusViewModel, stops: List<BusStop>, map: MapView) {
    val currentLocation = viewModel.location.observeAsState()

    AndroidView({ map }) { mapView ->
        mapView.getMapAsync { map ->
            map.isMyLocationEnabled = true
            map.uiSettings.isZoomControlsEnabled = true

            currentLocation.value?.let {
                val position = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15.0f))
            }

            map.setOnCameraIdleListener{
                val cameraPosition = map.cameraPosition
                val location = Location(cameraPosition.target.latitude, cameraPosition.target.longitude)
                viewModel.setLocation(location)
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
    val canvas = android.graphics.Canvas(bm)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bm)
}


data class BottomSheetState(var show: Boolean = false)

