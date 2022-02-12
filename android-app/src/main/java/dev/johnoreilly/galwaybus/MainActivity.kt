package dev.johnoreilly.galwaybus

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.pushpal.jetlime.data.JetLimeItemsModel
import com.pushpal.jetlime.data.config.*
import com.pushpal.jetlime.ui.JetLimeView
import com.surrus.galwaybus.common.model.Location
import dev.johnoreilly.galwaybus.ui.*
import dev.johnoreilly.galwaybus.ui.screens.BusInfoScreen
import dev.johnoreilly.galwaybus.ui.screens.FavoritesScreen
import dev.johnoreilly.galwaybus.ui.screens.LandingScreen
import dev.johnoreilly.galwaybus.ui.screens.NearestBusStopsScreen
import dev.johnoreilly.galwaybus.ui.utils.*
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.launch
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
                var showLandingScreen by remember { mutableStateOf(true) }

                if (showLandingScreen) {
                    LandingScreen(galwayBusViewModel, onTimeout = { showLandingScreen = false })
                } else {
                    MainLayout(fineLocation, fusedLocationWrapper, galwayBusViewModel)
                }
            }
        }
    }
}


sealed class Screens(val route: String, val label: String, val icon: ImageVector? = null) {
    object NearbyScreen : Screens("Nearby", "Nearby", Icons.Default.LocationOn)
    object FavoritesScreen : Screens("Favorites", "Favorites", Icons.Default.Favorite)
    object BusInfoScreen : Screens("BusInfo", "BusInfo")
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun JetLineTest(viewModel: GalwayBusViewModel) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val busInfoList by viewModel.busInfoList.collectAsState(emptyList())
    val busStopList by viewModel.allBusStops.collectAsState(emptyList())

    LaunchedEffect(viewModel) {
        viewModel.setRouteId("401")
    }

    val jetLimeItemsModel by remember {

        derivedStateOf {
            val jetItemList: MutableList<JetLimeItemsModel.JetLimeItem> = mutableStateListOf()

            if (busInfoList.isNotEmpty()) {
                val busInfo = busInfoList.find { it.vehicle_id == "1101" }

                val nextStopRef = busInfo?.next_stop_ref
                println("JFOR, nextStopRef = $nextStopRef")

                val busList = busInfo?.route ?: emptyList()
                var scrollToIndex = 0
                busList.forEachIndexed { index, busStopRef ->
                    val busStop = busStopList.find { it.stopRef == busStopRef }

                    val busStopIndex = busList.indexOf(busStopRef)
                    val nextBusStopIndex = busList.indexOf(nextStopRef)

                    val jetLimeItemConfig = if (busStopRef == nextStopRef) {
                        scrollToIndex = index
                        JetLimeItemConfig(iconAnimation = IconAnimation())
                    }
                    else {
                        if (busStopIndex < nextBusStopIndex) {
                            JetLimeItemConfig(itemHeight = 80.dp, iconType = IconType.Checked)
                        } else {
                            JetLimeItemConfig(itemHeight = 80.dp, iconType = IconType.Empty)
                        }

                    }

                    jetItemList.add(
                        JetLimeItemsModel.JetLimeItem(
                            title = busStop?.shortName ?: "",
                            description = busStopRef,
                            jetLimeItemConfig = jetLimeItemConfig
                        )
                    )
                }
//                coroutineScope.launch {
//                    listState.scrollToItem(scrollToIndex)
//                }
            }

            JetLimeItemsModel(list = jetItemList)
        }
    }

    JetLimeView(
        jetLimeItemsModel = jetLimeItemsModel,
        jetLimeViewConfig = JetLimeViewConfig(lineType = LineType.Solid),
        listState = listState
    )

}

@SuppressLint("MissingPermission")
@Composable
fun MainLayout(fineLocation: PermissionState,
               fusedLocationWrapper: FusedLocationWrapper,
               viewModel: GalwayBusViewModel
) {
    val navController = rememberNavController()
    val bottomNavigationItems = listOf(Screens.NearbyScreen, Screens.FavoritesScreen)
    val hasLocationPermission by fineLocation.hasPermission.collectAsState()
    val bottomBar: @Composable () -> Unit = { GalwayBusBottomNavigation(navController, bottomNavigationItems) }

    if (hasLocationPermission) {
        LaunchedEffect(fusedLocationWrapper) {
            val location = fusedLocationWrapper.awaitLastLocation()
            viewModel.setLocation(Location(location.latitude, location.longitude))
        }

        NavHost(navController, startDestination = Screens.NearbyScreen.route) {
            composable(Screens.NearbyScreen.route) {
                Scaffold(
                    topBar = { TopAppBar(title = { Text( "Galway Bus - Route 401") }) },
                    bottomBar = bottomBar
                ) {

                    JetLineTest(viewModel)
                }
                //NearestBusStopsScreen(bottomBar, viewModel, navController)
            }
            composable(Screens.FavoritesScreen.route) {
                FavoritesScreen(bottomBar, viewModel, navController)
            }
            composable(Screens.BusInfoScreen.route) {
                BusInfoScreen(viewModel, popBack = { navController.popBackStack() })
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






