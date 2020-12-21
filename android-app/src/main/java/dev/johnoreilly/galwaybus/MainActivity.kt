package dev.johnoreilly.galwaybus

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.setContent
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.surrus.galwaybus.common.model.Location
import dev.johnoreilly.galwaybus.ui.*
import dev.johnoreilly.galwaybus.ui.screens.FavoritesScreen
import dev.johnoreilly.galwaybus.ui.screens.NearestBusStopsScreen
import dev.johnoreilly.galwaybus.ui.utils.*
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
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


sealed class BottomNavigationScreens(val route: String, val label: String, val icon: ImageVector) {
    object NearbyScreen : BottomNavigationScreens("Nearby", "Nearby", Icons.Default.LocationOn)
    object FavoritesScreen : BottomNavigationScreens("Favorites", "Favorites", Icons.Default.Favorite)
}

@SuppressLint("MissingPermission")
@Composable
fun MainLayout(fineLocation: PermissionState,
               fusedLocationWrapper: FusedLocationWrapper,
               viewModel: GalwayBusViewModel
) {
    val navController = rememberNavController()

    val bottomNavigationItems = listOf(
            BottomNavigationScreens.NearbyScreen,
            BottomNavigationScreens.FavoritesScreen
    )

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
                        if (it != null) {
                            val loc = Location(it.latitude, it.longitude)
                            viewModel.setLocation(loc)
                        }
                    }
                }

                NavHost(navController, startDestination = BottomNavigationScreens.NearbyScreen.route) {
                    composable(BottomNavigationScreens.NearbyScreen.route) {
                        NearestBusStopsScreen(viewModel)
                    }
                    composable(BottomNavigationScreens.FavoritesScreen.route) {
                        FavoritesScreen(viewModel)
                    }
                }

            } else {
                fineLocation.launchPermissionRequest()
            }
        },
        bottomBar = {
            BottomNavigation {
                val currentRoute = currentRoute(navController)
                bottomNavigationItems.forEach { screen ->
                    BottomNavigationItem(
                            icon = { Icon(screen.icon) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            alwaysShowLabels = false, // This hides the title for the unselected items
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
    )
}


@Composable
private fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.arguments?.getString(KEY_ROUTE)
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






