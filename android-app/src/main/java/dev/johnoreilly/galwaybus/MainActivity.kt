@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package dev.johnoreilly.galwaybus

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.surrus.galwaybus.common.model.Location
import dev.johnoreilly.galwaybus.ui.*
import dev.johnoreilly.galwaybus.ui.screens.*
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.surrus.galwaybus.common.model.BusStop
import dev.johnoreilly.galwaybus.ui.camerascan.DeparturesCanvasOverlayView
import dev.johnoreilly.galwaybus.ui.camerascan.SimpleCameraPreview
import dev.johnoreilly.galwaybus.ui.camerascan.TextAnalyzer
import dev.johnoreilly.galwaybus.ui.screens.BusInfoScreen
import dev.johnoreilly.galwaybus.ui.screens.FavoritesScreen
import dev.johnoreilly.galwaybus.ui.screens.LandingScreen
import dev.johnoreilly.galwaybus.ui.screens.NearestBusStopsScreen
import dev.johnoreilly.galwaybus.ui.utils.*
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val galwayBusViewModel by viewModel<GalwayBusViewModel>()

    @OptIn(ExperimentalComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GalwayBusTheme {
                val fusedLocationWrapper = fusedLocationWrapper()
                val fineLocation = checkSelfPermissionState(this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                val cameraPermission = checkSelfPermissionState(this,
                    Manifest.permission.CAMERA
                )


                var showLandingScreen by remember { mutableStateOf(true) }


                if (showLandingScreen) {
                    LandingScreen(galwayBusViewModel, onTimeout = { showLandingScreen = false })
                } else {
                    MainLayout(cameraPermission, fineLocation, fusedLocationWrapper, galwayBusViewModel)
                }
            }
        }
    }
}


@OptIn(ExperimentalComposeApi::class)
@Composable
fun ScanStopScreen(cameraPermission: PermissionState, galwayBusViewModel: GalwayBusViewModel) {

    var detectedBusStop by remember { mutableStateOf<BusStop?>(null) }
    var detectedText by remember { mutableStateOf<String>("") }
    val busStops = galwayBusViewModel.getBusStops()

    val hasCameraPermission by cameraPermission.hasPermission.collectAsState()
    if (hasCameraPermission) {
        val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

        SimpleCameraPreview( TextAnalyzer(textRecognizer) {
            if (it.isNotEmpty()) {
                val possibleStopId = it.replace("-", "")
                println("JFOR, possibleStopId = $possibleStopId, busStops = $busStops")

                val matchedBusStop = busStops.find { possibleStopId.contains(it.stop_id) }
                if (matchedBusStop != null) {
                    println("matchedBusStop = ${matchedBusStop}")
                    detectedText = it
                    detectedBusStop = matchedBusStop
                }
            }
        })

        detectedBusStop?.let { busStop ->
            galwayBusViewModel.setCurrentStop(busStop)
            DeparturesCanvasOverlayView(galwayBusViewModel, busStop, detectedText)
        }

    } else {
        cameraPermission.launchPermissionRequest()
    }
}

sealed class Screens(val route: String, val label: String, val icon: ImageVector? = null) {
    object NearbyScreen : Screens("Nearby", "Nearby", Icons.Default.LocationOn)
    object FavoritesScreen : Screens("Favorites", "Favorites", Icons.Default.Favorite)
    object BikeShareScreen : Screens("BikeShare", "Bikes", Icons.Default.DirectionsBike)
    object ScanStopTextScreen : Screens("Scan stop", "Scan stop", Icons.Default.Search)
    object BusInfoScreen : Screens("BusInfo", "BusInfo")
    object BusRouteScreen : Screens("BusRoute", "BusRoute")
}

@SuppressLint("MissingPermission", "UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MainLayout(cameraPermission: PermissionState,
               fineLocation: PermissionState,
               fusedLocationWrapper: FusedLocationWrapper,
               viewModel: GalwayBusViewModel
) {
    val navController = rememberNavController()
    val bottomNavigationItems = listOf(Screens.NearbyScreen, Screens.BikeShareScreen, Screens.FavoritesScreen, Screens.ScanStopTextScreen)
    val hasLocationPermission by fineLocation.hasPermission.collectAsState()
    val bottomBar: @Composable () -> Unit = { GalwayBusBottomNavigation(navController, bottomNavigationItems) }

    if (hasLocationPermission) {
        LaunchedEffect(fusedLocationWrapper) {
            val location = fusedLocationWrapper.awaitLastLocation()
            viewModel.setLocation(Location(location.latitude, location.longitude))
        }

        NavHost(navController, startDestination = Screens.NearbyScreen.route) {
            composable(Screens.NearbyScreen.route) {
                NearestBusStopsScreen(bottomBar, viewModel, navController)
            }
            composable(Screens.FavoritesScreen.route) {
                FavoritesScreen(bottomBar, viewModel, navController)
            }
            composable(Screens.BusInfoScreen.route) {
                BusInfoScreen(viewModel, popBack = { navController.popBackStack() }) { busId ->
                    navController.navigate(Screens.BusRouteScreen.route + "/$busId")
                }
            }
            composable(Screens.BusRouteScreen.route+ "/{busId}") { backStackEntry ->
                BusRouteScreen(viewModel,
                    backStackEntry.arguments?.get("busId") as String,
                    popBack = { navController.popBackStack() })
            }
            composable(Screens.BikeShareScreen.route) {
                BikeShareScreen(bottomBar, viewModel)
            }
            composable(Screens.ScanStopTextScreen.route) {
                ScanStopScreen(cameraPermission, viewModel) //, popBack = { navController.popBackStack() })
            }
        }
    } else {
        fineLocation.launchPermissionRequest()
    }
}


@Composable
private fun GalwayBusBottomNavigation(navController: NavHostController, items: List<Screens>) {

    BottomNavigation {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route


        items.forEach { screen ->
            BottomNavigationItem(
                icon = { screen.icon?.let { Icon(it, contentDescription = screen.label) } },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                alwaysShowLabel = false, // This hides the title for the unselected items
                onClick = {
                    // This if check gives us a "singleTop" behavior where we do not create a
                    // second instance of the composable if we are already on that destination
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route)
                    }
                }
            )
        }
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
            Icon(imageVector = Icons.Filled.Favorite, tint = maroon500, contentDescription = "Favorited")
        } else {
            Icon(imageVector = Icons.Filled.FavoriteBorder, contentDescription = "Unfavorited")
        }
    }
}






