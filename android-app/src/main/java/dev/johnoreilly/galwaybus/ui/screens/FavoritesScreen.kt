package dev.johnoreilly.galwaybus.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomDrawerLayout
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import dev.johnoreilly.galwaybus.Screens
import dev.johnoreilly.galwaybus.ui.BusStopDeparture
import dev.johnoreilly.galwaybus.ui.typography
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel

@Composable
fun FavoritesScreen(viewModel: GalwayBusViewModel, navController: NavHostController) {
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)

    val departureList by viewModel.busDepartureList.observeAsState(emptyList())
    val favorites by viewModel.favorites.collectAsState(setOf())
    val busStopList = favorites.mapNotNull { viewModel.getBusStop(it) }

    BottomDrawerLayout(
            drawerState = drawerState,
            drawerShape = RoundedCornerShape(16.dp),
            drawerContent = {
                Text(text = "Departures", style = typography.h6,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                )

                LazyColumn {
                    items(items = departureList, itemContent = { departure ->
                        BusStopDeparture(departure) {
                            viewModel.setRouteId(departure.timetableId)
                            navController.navigate(Screens.BusInfoScreen.route)
                        }
                    })
                }
            }
    ) {

        BusStopListView(viewModel, busStopList, favorites) {
            viewModel.setStopRef(it.stopRef)
            drawerState.open()
        }
    }
}