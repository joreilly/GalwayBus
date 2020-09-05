package com.surrus.galwaybus.ui

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            LazyColumnFor(items = busStopState.value, itemContent = { departure ->
                BusStopDeparture(departure)
            })
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