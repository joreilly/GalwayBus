@file:OptIn(ExperimentalMaterial3Api::class)

package dev.johnoreilly.galwaybus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushpal.jetlime.EventPointType
import com.pushpal.jetlime.ItemsList
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeDefaults
import com.pushpal.jetlime.JetLimeEvent
import com.pushpal.jetlime.JetLimeEventDefaults
import com.surrus.galwaybus.common.model.BusStop
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.delay


@Composable
fun BusRouteScreen(viewModel: GalwayBusViewModel, busId: String, popBack: () -> Unit) {
    val routeId = viewModel.routeId.value

    Scaffold(topBar = {
            CenterAlignedTopAppBar(
                title = { Text(busId ?: "") },
                navigationIcon = {
                    IconButton(onClick = { popBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->

        Column(Modifier.padding(paddingValues)) {
            RouteTimelineView(viewModel, busId)
        }
    }
}


@Composable
fun RouteTimelineView(viewModel: GalwayBusViewModel, busId: String) {
    val listState = rememberLazyListState()

    val busInfoList by viewModel.busInfoList.collectAsState(emptyList())
    val busStopList by viewModel.allBusStops.collectAsState(emptyList())
    val busRouteItems = remember { mutableStateListOf<BusRouteItem>() }

//    LaunchedEffect(viewModel) {
//        viewModel.setRouteId("401")
//    }

    LaunchedEffect(key1 = busInfoList) {
        if (busInfoList.isNotEmpty()) {
            val busInfo = busInfoList.find { it.vehicle_id == busId }

            val nextStopRef = busInfo?.next_stop_ref
            println("extStopRef = $nextStopRef")

            val busList = busInfo?.route ?: emptyList()
            var scrollToIndex = 0
            busRouteItems.clear()
            busList.forEachIndexed { index, busStopRef ->
                var hasPassed = false
                val busStop = busStopList.find { it.stopRef == busStopRef }

                val busStopIndex = busList.indexOf(busStopRef)
                val nextBusStopIndex = busList.indexOf(nextStopRef)

                if (busStopRef == nextStopRef) {
                    scrollToIndex = index
                } else {
                    hasPassed = busStopIndex < nextBusStopIndex
                }

                busStop?.let { safeBusStop ->
                    busRouteItems.add(
                        BusRouteItem(
                            busStop = safeBusStop,
                            busStopRef = busStopRef,
                            shouldShowAnimation = busStopRef == nextStopRef,
                            hasPassed = hasPassed
                        )
                    )
                }
            }

            // Scroll with a delay
            delay(1000)
            listState.animateScrollToItem(index = scrollToIndex)
        }
    }

    JetLimeColumn(
        modifier = Modifier.padding(start = 32.dp, top = 16.dp).fillMaxSize(),
        itemsList = ItemsList(busRouteItems),
        listState = listState,
        key = { _, item -> item.busStop.stop_id },
        style = JetLimeDefaults.columnStyle()
    ) { _, item, position ->
        JetLimeEvent(
            style = JetLimeEventDefaults.eventStyle(
                position = position,
                pointAnimation = if (item.shouldShowAnimation) JetLimeEventDefaults.pointAnimation() else null,
                pointType = if (item.hasPassed) EventPointType.custom(icon = painterResource(id = R.drawable.icon_check)) else EventPointType.filled()
            )
        ) {
            BusStopContent(item)
        }
    }
}

@Composable
fun BusStopContent(item: BusRouteItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(0.9f),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            text = item.busStop.shortName,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 14.sp,
            text = item.busStopRef,
        )
    }
}

data class BusRouteItem(
    val busStop: BusStop,
    val busStopRef: String = "",
    val shouldShowAnimation: Boolean = false,
    val hasPassed: Boolean = false,
)