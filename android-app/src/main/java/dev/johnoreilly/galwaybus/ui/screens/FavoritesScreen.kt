package dev.johnoreilly.galwaybus.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dev.johnoreilly.galwaybus.Screens
import dev.johnoreilly.galwaybus.ui.BusStopDeparture
import dev.johnoreilly.galwaybus.ui.typography
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun FavoritesScreen(bottomBar: @Composable () -> Unit, viewModel: GalwayBusViewModel, navController: NavHostController) {
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)

    val departureList by viewModel.busDepartureList.observeAsState(emptyList())
    val favorites by viewModel.favorites.collectAsState(setOf())
    val busStopList = favorites.mapNotNull { viewModel.getBusStop(it) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text( "Galway Bus - Favourites") }) },
        bottomBar = bottomBar)
    {

        BottomDrawer(
            drawerState = drawerState,
            drawerShape = RoundedCornerShape(16.dp),
            drawerContent = {
                Text(text = "Departures", style = typography.h6,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                LazyColumn {
                    items(departureList) { departure ->
                        BusStopDeparture(departure) {
                            viewModel.setRouteId(departure.timetableId)
                            navController.navigate(Screens.BusInfoScreen.route)
                        }
                    }
                }
            }
        ) {

            BusStopListView(viewModel, busStopList, favorites) {

                val firebaseAnalytics = Firebase.analytics
                firebaseAnalytics.logEvent("select_stop") {
                    param("stop_name", it.longName)
                    param("stop_ref", it.stopRef)
                }

                viewModel.setStopRef(it.stopRef)
                coroutineScope.launch {
                    drawerState.open()
                }
            }
        }

    }
}