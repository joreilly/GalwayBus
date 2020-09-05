package com.surrus.galwaybus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope.weight
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.ui.tooling.preview.Preview
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.BitmapDescriptor
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.surrus.galwaybus.common.model.Location
import com.surrus.galwaybus.common.remote.Stop
import com.surrus.galwaybus.ui.BusStopScreen
import com.surrus.galwaybus.ui.GalwayBusTheme
import com.surrus.galwaybus.ui.rememberMapViewWithLifecycle
import com.surrus.galwaybus.ui.viewmodel.GalwayBusViewModel
import com.surrus.galwaybus.ui.viewmodel.Screen
import com.surrus.galwaybus.ui.viewmodel.UiState
import org.koin.android.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    private val galwayBusViewModel by viewModel<GalwayBusViewModel>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : BasePermissionListener() {
                    @SuppressLint("MissingPermission")
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        println("Permission granted")
                        fusedLocationClient.lastLocation.addOnSuccessListener { fusedLocation ->
                            if (fusedLocation != null) {
                                //val location = Location(fusedLocation.latitude, fusedLocation.longitude)

                                // Center in Eyre Squre for now
                                val location = Location(53.2743394, -9.0514163)
                                galwayBusViewModel.setLocation(location)
                                galwayBusViewModel.getNearestStops(location)
                            }
                        }
                    }
                }).check()


        setContent {
            AppContent(galwayBusViewModel)
        }
    }


    override fun onBackPressed() {
        if (!galwayBusViewModel.onBack()) {
            super.onBackPressed()
        }
    }
}




@Composable
private fun AppContent(viewModel: GalwayBusViewModel) {
    val busStopState = viewModel.uiState.observeAsState(UiState.Loading)
    val currentScreenState = viewModel.currentScreen.observeAsState()

    GalwayBusTheme {
        Crossfade(currentScreenState.value) { screen ->
            when (screen) {
                is Screen.Home -> {
                    MainLayout(viewModel, busStopState)
                }
                is Screen.BusStopView -> {
                    BusStopScreen(viewModel, screen.stopId, screen.stopName)
                }
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: GalwayBusViewModel, busStopState: State<UiState<List<Stop>>>) {
    var bottomBarSelectedIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Galway Bus") })
        },
        bodyContent = {
            when (bottomBarSelectedIndex) {
                0 -> {
                    BusStopListBody(viewModel, busStopState)
                }
                1 -> {
                    Text("Starred items")
                }
            }
        },
        bottomBar = {
            BottomAppBar(content = {
                BottomNavigationItem(icon = { Icon(Icons.Default.LocationOn) }, label = { Text("Nearby") },
                        selected = bottomBarSelectedIndex == 0,
                        onSelect = { bottomBarSelectedIndex = 0 })

                BottomNavigationItem(icon = { Icon(Icons.Default.Star) }, label = { Text("Starred") },
                        selected = bottomBarSelectedIndex == 1,
                        onSelect = { bottomBarSelectedIndex = 1 })
            })
        }
    )
}


@SuppressLint("MissingPermission")
@Composable
fun BusStopListBody(viewModel: GalwayBusViewModel, busStopState: State<UiState<List<Stop>>>) {
    val mapView = rememberMapViewWithLifecycle()

    Column {
        val uiState = busStopState.value
        if (uiState is UiState.Success) {
            MapViewContainer(viewModel, uiState.data, mapView, modifier = Modifier.weight(0.4f) )
        }


        Box(modifier = Modifier.weight(0.6f)) {
            when (val uiState = busStopState.value) {
                is UiState.Success -> {
                    LazyColumnFor(items = uiState.data, itemContent = { stop ->
                        StopViewRow(stop) {
                            viewModel.navigateTo(Screen.BusStopView(stop.stopid, stop.shortname))
                        }
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

@Composable
fun StopViewRow(stop: Stop, itemClick : (stop : Stop) -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = { itemClick(stop) }).padding(8.dp).fillMaxWidth(),
        verticalGravity = Alignment.CenterVertically
    ) {

        Image(imageResource(R.drawable.ic_aiga_bus), modifier = Modifier.preferredSize(32.dp))

        Spacer(modifier = Modifier.preferredSize(12.dp))

        Column {
            Text(text = stop.fullname, style = TextStyle(fontSize = 18.sp))
            Text(text = stop.stopid, style = TextStyle(color = Color.DarkGray, fontSize = 12.sp))
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun MapViewContainer(viewModel: GalwayBusViewModel, stops: List<Stop>, map: MapView, modifier: Modifier) {
    val currentLocation = viewModel.location.observeAsState()

    AndroidView({ map }, modifier = Modifier.weight(0.4f)) { mapView ->
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
                //nearestBusStopsViewModel.setZoomLevel(cameraPosition.zoom)
                viewModel.setLocation(location)

                viewModel.getNearestStops(location)
            }


            for (busStop in stops) {
                val busStopLocation = LatLng(busStop.latitude.toDouble(), busStop.longitude.toDouble())

                val icon = bitmapDescriptorFromVector(mapView.context, R.drawable.ic_stop, R.color.mapMarkerGreen)
                val markerOptions = MarkerOptions()
                        .title(busStop.shortname)
                        .position(busStopLocation)
                        .icon(icon)

                val marker = map.addMarker(markerOptions)
                marker.tag = busStop
            }
        }
    }
}

@Composable
private fun ZoomControls(
        zoom: Float,
        onZoomChanged: (Float) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        ZoomButton("-", onClick = { onZoomChanged(zoom * 0.8f) })
        ZoomButton("+", onClick = { onZoomChanged(zoom * 1.2f) })
    }
}

@Composable
private fun ZoomButton(text: String, onClick: () -> Unit) {
    Button(
            modifier = Modifier.padding(8.dp),
            backgroundColor = MaterialTheme.colors.onPrimary,
            contentColor = MaterialTheme.colors.primary,
            onClick = onClick
    ) {
        Text(text = text, style = MaterialTheme.typography.h5)
    }
}



private const val InitialZoom = 5f
const val MinZoom = 2f
const val MaxZoom = 20f




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



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GalwayBusTheme {
        val stop = Stop("1234", "Some Stop", "Stop full name","0.0", "0.0")
        StopViewRow(stop) {}
    }
}