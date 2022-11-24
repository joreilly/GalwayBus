@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalMaterial3Api::class
)

package dev.johnoreilly.galwaybus

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Text
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.surrus.galwaybus.common.model.Location
import dev.johnoreilly.galwaybus.ui.screens.*
import dev.johnoreilly.galwaybus.ui.theme.GalwayBusBackground
import dev.johnoreilly.galwaybus.ui.theme.GalwayBusTheme
import dev.johnoreilly.galwaybus.ui.theme.LocalBackgroundTheme
import dev.johnoreilly.galwaybus.ui.theme.maroon500
import dev.johnoreilly.galwaybus.ui.utils.*
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val galwayBusViewModel by viewModel<GalwayBusViewModel>()

    @OptIn(ExperimentalComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            GalwayBusTheme {
                GalwayBusBackground {
                    val fusedLocationWrapper = fusedLocationWrapper()
                    val fineLocation = checkSelfPermissionState(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    var showLandingScreen by remember { mutableStateOf(true) }

                    if (showLandingScreen) {
                        LandingScreen(galwayBusViewModel, onTimeout = { showLandingScreen = false })
                    } else {
                        MainLayout(calculateWindowSizeClass(this),  fineLocation, fusedLocationWrapper, galwayBusViewModel)
                    }

                }
            }
        }
    }
}


sealed class Screens(val route: String, val label: String, val selectedIcon: ImageVector? = null, val unSelectedIcon: ImageVector? = null) {
    object NearbyScreen : Screens("Nearby", "Nearby", Icons.Filled.LocationOn, Icons.Outlined.LocationOn)
    object FavoritesScreen : Screens("Favorites", "Favorites", Icons.Filled.Favorite, Icons.Outlined.Favorite)
    object BikeShareScreen : Screens("BikeShare", "Bikes", Icons.Filled.DirectionsBike, Icons.Outlined.DirectionsBike)
    object BusInfoScreen : Screens("BusInfo", "BusInfo")
    object BusRouteScreen : Screens("BusRoute", "BusRoute")
}

@SuppressLint("MissingPermission", "UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MainLayout(windowSizeClass: WindowSizeClass,
               fineLocation: PermissionState,
               fusedLocationWrapper: FusedLocationWrapper,
               viewModel: GalwayBusViewModel
) {
    val navController = rememberNavController()
    val bottomNavigationItems = listOf(Screens.NearbyScreen, Screens.BikeShareScreen, Screens.FavoritesScreen)
    val hasLocationPermission by fineLocation.hasPermission.collectAsState()

    val shouldShowBottomBar = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
                windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact


    if (hasLocationPermission) {
        LaunchedEffect(fusedLocationWrapper) {
            // default to center of Galway until we get location
            viewModel.centerInEyreSquare()

            val location = fusedLocationWrapper.awaitLastLocation()
            viewModel.setLocation(Location(location.latitude, location.longitude))
        }


        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (shouldShowBottomBar) {
                    GalwayBusBottomNavigation(navController, bottomNavigationItems)
                }
            }
        ) { padding ->

            Row(Modifier.fillMaxSize().padding(padding)) {

                if (!shouldShowBottomBar) {
                    GalwayBusNavigationRail(navController, bottomNavigationItems)
                }


                NavHost(navController, startDestination = Screens.NearbyScreen.route) {
                    composable(Screens.NearbyScreen.route) {
                        NearestBusStopsScreen(viewModel, navController)
                    }
                    composable(Screens.FavoritesScreen.route) {
                        FavoritesScreen(viewModel, navController)
                    }
                    composable(Screens.BusInfoScreen.route) {
                        BusInfoScreen(
                            viewModel,
                            popBack = { navController.popBackStack() }) { busId ->
                            navController.navigate(Screens.BusRouteScreen.route + "/$busId")
                        }
                    }
                    composable(Screens.BusRouteScreen.route + "/{busId}") { backStackEntry ->
                        BusRouteScreen(viewModel,
                            backStackEntry.arguments?.get("busId") as String,
                            popBack = { navController.popBackStack() })
                    }
                    composable(Screens.BikeShareScreen.route) {
                        BikeShareScreen(viewModel)
                    }
                }
            }
        }
    } else {
        fineLocation.launchPermissionRequest()
    }
}


@Composable
private fun GalwayBusBottomNavigation(navController: NavHostController, items: List<Screens>) {

    NavigationBar(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    val icon = if (currentRoute == screen.route) {
                        screen.selectedIcon
                    } else {
                        screen.unSelectedIcon
                    }
                    icon?.let { Icon(icon, contentDescription = screen.label) }
                },
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
private fun GalwayBusNavigationRail(navController: NavHostController, items: List<Screens>) {

    NavigationRail(
        //modifier = modifier,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        //contentColor = PeopleInSpaceNavigationDefaults.navigationContentColor(),
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            val selected = currentRoute == screen.route

            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route)
                    }
                },
                icon = {
                    val icon = if (selected) {
                        screen.selectedIcon
                    } else {
                        screen.unSelectedIcon
                    }
                    icon?.let { Icon(icon, contentDescription = screen.label) }
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






