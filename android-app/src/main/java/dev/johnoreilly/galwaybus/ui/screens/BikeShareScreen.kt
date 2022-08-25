package dev.johnoreilly.galwaybus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.surrus.galwaybus.common.remote.Station
import com.surrus.galwaybus.common.remote.freeBikes
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.highAvailabilityColor
import dev.johnoreilly.galwaybus.ui.lowAvailabilityColor
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi


@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun BikeShareScreen(bottomBar: @Composable () -> Unit, viewModel: GalwayBusViewModel) {
    val stationsState by viewModel.stations.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(true) {
        viewModel.fetchStations()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("BikeShare - Galway") })
        }, bottomBar = bottomBar) { paddingValues ->

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { viewModel.fetchStations() },
        ) {
            LazyColumn(Modifier.padding(paddingValues)) {
                items(stationsState) { station ->
                    StationView(station)
                }
            }
        }
    }
}


@Composable
fun StationView(station: Station) {
    Card(elevation = 12.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {

        Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {

            Image(
                painterResource(R.drawable.ic_bike),
                colorFilter = ColorFilter.tint(if (station.freeBikes() < 5) lowAvailabilityColor else highAvailabilityColor),
                modifier = Modifier.size(32.dp), contentDescription = station.freeBikes().toString())

            Spacer(modifier = Modifier.size(16.dp))

            Column {
                Text(text = station.name, style = MaterialTheme.typography.h6)

                ProvideTextStyle(MaterialTheme.typography.body2) {
                    Row {
                        Text("Bikes:", modifier = Modifier.width(128.dp))
                        Text(
                            text = station.freeBikes().toString(),
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    Row {
                        Text("Stands:", modifier = Modifier.width(128.dp))
                        Text(
                            text = station.empty_slots.toString(),
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

