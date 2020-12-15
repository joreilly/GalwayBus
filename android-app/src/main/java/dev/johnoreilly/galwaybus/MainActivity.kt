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
import androidx.compose.ui.platform.setContent
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


@SuppressLint("MissingPermission")
@Composable
fun MainLayout(fineLocation: PermissionState,
               fusedLocationWrapper: FusedLocationWrapper,
               viewModel: GalwayBusViewModel
) {
    var bottomBarSelectedIndex by remember { mutableStateOf(0) }
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
                            println("JFOR, got location update")
                            val loc = Location(it.latitude, it.longitude)
                            viewModel.setLocation(loc)
                        }
                    }
                }

                when (bottomBarSelectedIndex) {
                    0 -> NearestBusStopsScreen(viewModel)
                    1 -> FavoritesScreen(viewModel)
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






