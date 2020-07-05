package com.surrus.galwaybus.ui

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.foundation.lazy.LazyColumnItems
import androidx.ui.layout.*
import androidx.ui.livedata.observeAsState
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.Surface
import androidx.ui.material.TopAppBar
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.surrus.galwaybus.common.remote.RealtimeBusInformation
import com.surrus.galwaybus.ui.viewmodel.GalwayBusViewModel


@Composable
fun BusStopScreen(viewModel: GalwayBusViewModel, stopId: String, stopName: String) {

    viewModel.getBusStopDepartures(stopId)
    val busStopState = viewModel.busDepartureList.observeAsState(emptyList())


    Scaffold(
        topBar = {
            TopAppBar(title = { Text("$stopName ($stopId)", style = TextStyle(fontSize = 18.sp)) })
        },
        bodyContent = {
            LazyColumnItems(items = busStopState.value) { departure ->
                BusStopDeparture(departure)
            }
        }
    )
}


@Composable
fun BusStopDeparture(departure: RealtimeBusInformation) {

    Row(verticalGravity = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
        Text(departure.route, modifier = Modifier.preferredWidth(48.dp), style = TextStyle(fontSize = 18.sp))

        Text(departure.destination, modifier = Modifier.weight(1f).padding(start = 16.dp), style = TextStyle(fontSize = 18.sp))

        Text("${departure.duetime} mins", modifier = Modifier.padding(start = 16.dp), style = TextStyle(fontSize = 18.sp))
    }

}