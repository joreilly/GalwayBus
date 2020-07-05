package com.surrus.galwaybus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.*
import androidx.compose.State
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentManager
import androidx.ui.animation.Crossfade
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.*
import androidx.ui.foundation.lazy.LazyColumnItems
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.livedata.observeAsState
import androidx.ui.material.*
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.material.icons.filled.LocationOn
import androidx.ui.material.icons.filled.Star
import androidx.ui.res.imageResource
import androidx.ui.text.TextStyle
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import androidx.ui.viewinterop.AndroidView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.surrus.galwaybus.common.model.Location
import com.surrus.galwaybus.common.remote.Stop
import com.surrus.galwaybus.ui.BusStopScreen
import com.surrus.galwaybus.ui.GalwayBusTheme
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
                                val location = Location(fusedLocation.latitude, fusedLocation.longitude)
                                println(location)

                                galwayBusViewModel.setLocation(location)
                                galwayBusViewModel.getNearestStops(location)

//                                nearestBusStopsViewModel.setLocation(location)
//                                nearestBusStopsViewModel.setCameraPosition(location)
                            }
                        }
                    }
                }).check()



        setContent {
            GalwayBusTheme() {
                AppContent(galwayBusViewModel, supportFragmentManager)
            }
        }
    }


    override fun onBackPressed() {
        if (!galwayBusViewModel.onBack()) {
            super.onBackPressed()
        }
    }
}




@Composable
private fun AppContent(viewModel: GalwayBusViewModel, fragmentManager: FragmentManager) {

    val busStopState = viewModel.uiState.observeAsState(UiState.Loading)
    val currentScreenState = viewModel.currentScreen.observeAsState()

    Crossfade(currentScreenState.value) { screen ->
        when (screen) {
            is Screen.Home -> {
                MainLayout(viewModel, busStopState, fragmentManager)
            }
            is Screen.BusStopView -> {
                BusStopScreen(viewModel, screen.stopId, screen.stopName)
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: GalwayBusViewModel, busStopState: State<UiState<List<Stop>>>, fragmentManager: FragmentManager) {
    val scaffoldState = remember { ScaffoldState() }
    var bottomBarSelectedIndex by state { 0 }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(title = { Text("Galway Bus") })
        },
        bodyContent = {
            when (bottomBarSelectedIndex) {
                0 -> {
                    BusStopListBody(viewModel, busStopState, fragmentManager)
                }
                1 -> {
                    Text("Starred items")
                }
            }
        },
        bottomBar = {
            BottomAppBar(content = {
                BottomNavigationItem(icon = { Icon(Icons.Default.LocationOn) }, text = { Text("Nearby") },
                        selected = bottomBarSelectedIndex == 0,
                        onSelected = { bottomBarSelectedIndex = 0 })

                BottomNavigationItem(icon = { Icon(Icons.Default.Star) }, text = { Text("Starred") },
                        selected = bottomBarSelectedIndex == 1,
                        onSelected = { bottomBarSelectedIndex = 1 })
            })
        }
    )
}


@SuppressLint("MissingPermission")
@Composable
fun BusStopListBody(viewModel: GalwayBusViewModel, busStopState: State<UiState<List<Stop>>>, fragmentManager: FragmentManager) {
    val centerLocation = Location(53.2743394, -9.0514163) // default if we can't get location


    val currentLocation = viewModel.location.observeAsState()


    Column {

        val uiState = busStopState.value
        if (uiState is UiState.Success) {
            AndroidView(resId = R.layout.fragment_nearby, modifier = Modifier.weight(0.4f)) {
                val mapFragment = fragmentManager.findFragmentById(R.id.map) as SupportMapFragment

                mapFragment.getMapAsync { map ->

                    map.isMyLocationEnabled = true
                    map.uiSettings.isZoomControlsEnabled = true

                    currentLocation.value?.let {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15.0f))
                    }

                    map.setOnCameraIdleListener{
                        val cameraPosition = map.cameraPosition

                        val location = Location(cameraPosition.target.latitude, cameraPosition.target.longitude)
                        //nearestBusStopsViewModel.setZoomLevel(cameraPosition.zoom)
                        viewModel.setLocation(location)

                        viewModel.getNearestStops(location)
                    }


                    for (busStop in uiState.data) {
                        val busStopLocation = LatLng(busStop.latitude.toDouble(), busStop.longitude.toDouble())

                        val icon = bitmapDescriptorFromVector(it.context, R.drawable.ic_stop, R.color.mapMarkerGreen)
                        val markerOptions = MarkerOptions()
                                .title(busStop.shortname)
                                .position(busStopLocation)
                                .icon(icon)

                        val marker = map?.addMarker(markerOptions)
                        marker?.tag = busStop
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(0.6f)) {
            when (val uiState = busStopState.value) {
                is UiState.Success -> {
                    LazyColumnItems(items = uiState.data) { stop ->
                        StopViewRow(stop) {
                            viewModel.navigateTo(Screen.BusStopView(stop.stopid, stop.shortname))
                        }
                    }
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