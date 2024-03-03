package dev.johnoreilly.galwaybus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.surrus.galwaybus.common.remote.Station
import com.surrus.galwaybus.common.remote.freeBikes
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.theme.highAvailabilityColor
import dev.johnoreilly.galwaybus.ui.theme.lowAvailabilityColor
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeShareScreen(viewModel: GalwayBusViewModel) {
    val stationsState by viewModel.stations.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(true) {
        viewModel.fetchStations()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Galway Bike Share") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->

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

    Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically) {

        Column(modifier = Modifier.weight(1.0f)) {
            Text(text = station.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Column(modifier = Modifier.width(100.dp)) {
                        Text("Bikes")
                        Text(station.freeBikes().toString(),
                            color = if (station.freeBikes() < 5) lowAvailabilityColor else highAvailabilityColor)
                    }

                    Column {
                        Text("Stands")
                        Text(station.empty_slots.toString(),
                            color = if (station.freeBikes() < 5) lowAvailabilityColor else highAvailabilityColor)
                    }
                }
            }
        }

        Column {
            Image(
                painterResource(R.drawable.ic_bike),
                colorFilter = ColorFilter.tint(if (station.freeBikes() < 5) lowAvailabilityColor else highAvailabilityColor),
                modifier = Modifier.size(32.dp), contentDescription = station.freeBikes().toString())
        }

    }
}

