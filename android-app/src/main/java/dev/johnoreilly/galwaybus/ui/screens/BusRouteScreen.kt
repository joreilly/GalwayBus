@file:OptIn(ExperimentalMaterial3Api::class)

package dev.johnoreilly.galwaybus.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pushpal.jetlime.data.JetLimeItemsModel
import com.pushpal.jetlime.data.config.*
import com.pushpal.jetlime.ui.JetLimeView
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.launch


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


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RouteTimelineView(viewModel: GalwayBusViewModel, busId: String) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val busInfoList by viewModel.busInfoList.collectAsState(emptyList())
    val busStopList by viewModel.allBusStops.collectAsState(emptyList())

//    LaunchedEffect(viewModel) {
//        viewModel.setRouteId("401")
//    }

    val jetLimeItemsModel by remember {

        derivedStateOf {
            val jetItemList: MutableList<JetLimeItemsModel.JetLimeItem> = mutableStateListOf()

            if (busInfoList.isNotEmpty()) {
                val busInfo = busInfoList.find { it.vehicle_id == busId }

                val nextStopRef = busInfo?.next_stop_ref
                println("extStopRef = $nextStopRef")

                val busList = busInfo?.route ?: emptyList()
                var scrollToIndex = 0
                busList.forEachIndexed { index, busStopRef ->
                    val busStop = busStopList.find { it.stopRef == busStopRef }

                    val busStopIndex = busList.indexOf(busStopRef)
                    val nextBusStopIndex = busList.indexOf(nextStopRef)

                    val jetLimeItemConfig = if (busStopRef == nextStopRef) {
                        scrollToIndex = index
                        JetLimeItemConfig(iconAnimation = IconAnimation())
                    }
                    else {
                        if (busStopIndex < nextBusStopIndex) {
                            JetLimeItemConfig(itemHeight = 80.dp, iconType = IconType.Checked)
                        } else {
                            JetLimeItemConfig(itemHeight = 80.dp, iconType = IconType.Empty)
                        }

                    }

                    jetItemList.add(
                        JetLimeItemsModel.JetLimeItem(
                            title = busStop?.shortName ?: "",
                            description = busStopRef,
                            jetLimeItemConfig = jetLimeItemConfig
                        )
                    )
                }
                coroutineScope.launch {
                    listState.scrollToItem(scrollToIndex)
                }
            }

            JetLimeItemsModel(list = jetItemList)
        }
    }

    JetLimeView(
        jetLimeItemsModel = jetLimeItemsModel,
        jetLimeViewConfig = JetLimeViewConfig(lineType = LineType.Solid),
        listState = listState
    )

}