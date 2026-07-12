package dev.johnoreilly.galwaybus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.johnoreilly.galwaybus.map.BusMapView
import androidx.compose.ui.graphics.Color
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.DepartureTime
import dev.johnoreilly.galwaybus.model.Route
import dev.johnoreilly.galwaybus.model.Stop
import kotlinx.coroutines.delay
import kotlin.time.Instant

@Composable
fun App() {
    val repository = remember { GalwayBusRepository() }
    val viewModel = viewModel { GalwayBusViewModel(repository) }
    val colorScheme = lightColorScheme(
        primary = Color(0xFFA80050),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF3E001D),
        secondary = Color(0xFF77565C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFD9E2),
        onSecondaryContainer = Color(0xFF2C151A),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        surface = Color(0xFFFFFBFB),
        onSurface = Color(0xFF201A1B),
        surfaceVariant = Color(0xFFF4DDDD),
        onSurfaceVariant = Color(0xFF524343),
        outline = Color(0xFF857373),
        outlineVariant = Color(0xFFD8C2C2)
    )
    MaterialTheme(colorScheme = colorScheme) {
        GalwayBusApp(viewModel)
    }
}

private enum class ViewMode(val label: String) {
    LIST("Buses"), STOPS("Stops"), MAP("Map")
}

private enum class TopTab(val label: String, val glyph: String) {
    FAVOURITES("My stops", "★"), BUSES("Buses", "🚌"), ROUTES("Routes", "☰")
}

private enum class Screen {
    MAIN, TRACKING
}

/** Formats departure countdown with live delays. Returns "Due", "3 min", "8 min (late)", etc. */
fun DepartureTime.formatWithLive(nowMs: Long): String {
    val delaySeconds = delaySeconds
    val scheduledMinutes = depart_timestamp?.let { ts ->
        Instant.parse(ts).let { instant ->
            ((instant.toEpochMilliseconds() - nowMs) / 1000 / 60).toInt()
        }
    } ?: return "N/A"

    return when {
        delaySeconds != null -> {
            val liveDisplay = when {
                scheduledMinutes < 0 -> "Due"
                scheduledMinutes < 60 -> "$scheduledMinutes min"
                else -> "${scheduledMinutes / 60}h ${scheduledMinutes % 60}min"
            }
            val lateLabel = if (delaySeconds > 30) " (late)" else if (delaySeconds < -30) " (early)" else ""
            "$liveDisplay$lateLabel"
        }
        scheduledMinutes <= 0 -> "Due"
        scheduledMinutes < 60 -> "$scheduledMinutes min"
        else -> "${scheduledMinutes / 60}h ${scheduledMinutes % 60}min"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalwayBusApp(viewModel: GalwayBusViewModel) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    val selectedRouteNum = viewModel.selectedRouteNum
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }
    // Land on "My stops" when the user already has favourites, otherwise on Routes.
    var topTab by remember {
        mutableStateOf(if (viewModel.favourites.value.isNotEmpty()) TopTab.FAVOURITES else TopTab.ROUTES)
    }

    // Ticker driving the relative "updated Xs ago" / departure countdown labels
    var nowMs by remember { mutableStateOf(nowEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            nowMs = nowEpochMilliseconds()
        }
    }

    val onDepartureClick: (DepartureTime, String) -> Unit = { dep, stopRef ->
        viewModel.setTrackedDeparture(dep, stopRef)
        currentScreen = Screen.TRACKING
    }

    BoxWithConstraints {
        val compact = maxWidth < 600.dp
        
        if (currentScreen == Screen.TRACKING) {
            BusTrackingView(
                viewModel = viewModel,
                onBack = { currentScreen = Screen.MAIN }
            )
        } else {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (topTab) {
                                TopTab.FAVOURITES -> "My stops"
                                TopTab.BUSES -> "All Buses"
                                TopTab.ROUTES ->
                                    if (selectedRouteNum != null) "Route $selectedRouteNum" else "Galway Bus"
                            }
                        )
                    },
                    navigationIcon = {
                        if (topTab == TopTab.ROUTES && compact && selectedRouteNum != null) {
                            IconButton(onClick = { viewModel.clearRoute() }) {
                                Text("←", fontSize = 20.sp)
                            }
                        }
                    },
                    actions = {
                        if (topTab == TopTab.FAVOURITES && favourites.isNotEmpty()) {
                            IconButton(onClick = { viewModel.refreshFavouriteDepartures() }) {
                                Text("↻", fontSize = 22.sp)
                            }
                        }
                        if (topTab == TopTab.BUSES) {
                            IconButton(onClick = { viewModel.refreshAllBusPositions() }) {
                                Text("↻", fontSize = 22.sp)
                            }
                        }
                        if (topTab == TopTab.ROUTES && !compact && selectedRouteNum != null) {
                            ModeSwitcher(
                                viewMode = viewMode,
                                onSelect = { viewMode = it },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    TopTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = topTab == tab,
                            onClick = {
                                topTab = tab
                                if (tab == TopTab.FAVOURITES) viewModel.refreshFavouriteDepartures()
                            },
                            icon = { Text(tab.glyph, fontSize = 18.sp) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (topTab) {
                TopTab.FAVOURITES -> FavouritesPanel(
                    viewModel = viewModel,
                    nowMs = nowMs,
                    onBrowseRoutes = { topTab = TopTab.ROUTES },
                    onDepartureClick = onDepartureClick,
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
                TopTab.BUSES -> AllBusesPanel(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
                TopTab.ROUTES -> if (compact) {
                    if (selectedRouteNum == null) {
                        RouteListPanel(
                            routes = routes,
                            selectedRouteNum = null,
                            onRouteSelected = {
                                viewModel.selectRoute(it.short_name)
                                viewMode = ViewMode.LIST
                            },
                            modifier = Modifier.padding(padding).fillMaxSize()
                        )
                    } else {
                        Column(Modifier.padding(padding).fillMaxSize()) {
                            ModeSwitcher(
                                viewMode = viewMode,
                                onSelect = { viewMode = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            HorizontalDivider()
                            DetailPane(viewModel, viewMode, nowMs, onDepartureClick, Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                } else {
                    Row(Modifier.padding(padding).fillMaxSize()) {
                        RouteListPanel(
                            routes = routes,
                            selectedRouteNum = selectedRouteNum,
                            onRouteSelected = {
                                viewModel.selectRoute(it.short_name)
                                viewMode = ViewMode.LIST
                            },
                            modifier = Modifier.width(200.dp).fillMaxHeight()
                        )
                        Box(Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        DetailPane(viewModel, viewMode, nowMs, onDepartureClick, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}
}

@Composable
private fun AllBusesPanel(
    viewModel: GalwayBusViewModel,
    modifier: Modifier = Modifier
) {
    val busPositions by viewModel.allBusPositions.collectAsStateWithLifecycle()
    val allStops by viewModel.allStops.collectAsStateWithLifecycle()

    Box(modifier) {
        BusMapView(
            positions = busPositions,
            stops = allStops,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BusTrackingView(
    viewModel: GalwayBusViewModel,
    onBack: () -> Unit
) {
    val busPositions by viewModel.busPositions.collectAsStateWithLifecycle()
    val routeStops by viewModel.routeStops.collectAsStateWithLifecycle()
    val trackedTripId = viewModel.trackedTripId
    val trackedStopRef = viewModel.trackedStopRef

    // Filter to show only the tracked bus and the targeted stop
    // If we can't find the specific bus (e.g. ID mismatch or live-only departure),
    // show all buses on this route so the user still sees something.
    val busesForRoute = busPositions
    val trackedBus = busesForRoute.filter { it.trip_duid == trackedTripId }
    val displayPositions = if (trackedBus.isNotEmpty()) trackedBus else busesForRoute

    val trackedStop = routeStops.flatten().filter { it.stop_ref == trackedStopRef }.distinctBy { it.stop_ref }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Track Bus") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            BusMapView(
                positions = displayPositions,
                stops = trackedStop,
                trackedTripId = trackedTripId,
                trackedStopRef = trackedStopRef,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FavouritesPanel(
    viewModel: GalwayBusViewModel,
    nowMs: Long,
    onBrowseRoutes: () -> Unit,
    onDepartureClick: (DepartureTime, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    val departuresByStop by viewModel.favouriteDepartures.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshFavouriteDepartures() }

    if (favourites.isEmpty()) {
        Column(
            modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "No favourite stops yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Open a route, tap a stop, then tap ☆\nto pin it here for quick departures",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBrowseRoutes) { Text("Browse routes") }
        }
        return
    }

    LazyColumn(modifier) {
        items(favourites, key = { it.stopRef }) { fav ->
            FavouriteStopCard(
                name = fav.name,
                stopId = fav.stopId,
                departures = departuresByStop[fav.stopRef],
                nowMs = nowMs,
                onRemove = { viewModel.removeFavourite(fav.stopRef) },
                onDepartureClick = { onDepartureClick(it, fav.stopRef) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FavouriteStopCard(
    name: String,
    stopId: String,
    departures: List<DepartureTime>?,
    nowMs: Long,
    onRemove: () -> Unit,
    onDepartureClick: (DepartureTime) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(
                    stopId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Text("★", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (departures == null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 44.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Loading departures…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            StopDeparturesSection(departures, isLoading = false, nowMs = nowMs, onDepartureClick = onDepartureClick)
        }
    }
}

@Composable
private fun ModeSwitcher(
    viewMode: ViewMode,
    onSelect: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        ViewMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = viewMode == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, ViewMode.entries.size)
            ) { Text(mode.label) }
        }
    }
}

@Composable
private fun DetailPane(
    viewModel: GalwayBusViewModel,
    viewMode: ViewMode,
    nowMs: Long,
    onDepartureClick: (DepartureTime, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val busPositions by viewModel.busPositions.collectAsStateWithLifecycle()
    val routeStops by viewModel.routeStops.collectAsStateWithLifecycle()
    val directionHeadsigns by viewModel.directionHeadsigns.collectAsStateWithLifecycle()
    val stopDepartures by viewModel.stopDepartures.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    val selectedRouteNum = viewModel.selectedRouteNum

    if (selectedRouteNum == null) {
        NoRoutePlaceholder(modifier)
        return
    }
    when (viewMode) {
        ViewMode.STOPS -> RouteStopsPanel(
            stops = routeStops.getOrElse(viewModel.selectedDirection) { emptyList() },
            directionCount = routeStops.size,
            selectedDirection = viewModel.selectedDirection,
            directionHeadsigns = directionHeadsigns,
            onSelectDirection = { viewModel.selectDirection(it) },
            selectedStopRef = viewModel.selectedStopRef,
            departures = stopDepartures,
            isLoadingDepartures = viewModel.isLoadingDepartures,
            onStopClick = { viewModel.selectStop(it) },
            favouriteRefs = favourites.map { it.stopRef }.toSet(),
            onToggleFavourite = { viewModel.toggleFavourite(it) },
            nowMs = nowMs,
            onDepartureClick = { onDepartureClick(it, viewModel.selectedStopRef ?: "") },
            modifier = modifier
        )
        ViewMode.MAP -> Box(modifier) {
            key(selectedRouteNum) {
                BusMapView(
                    positions = busPositions,
                    stops = routeStops.flatten().distinctBy { it.stop_ref },
                    trackedTripId = viewModel.trackedTripId,
                    trackedStopRef = viewModel.trackedStopRef,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SmallFloatingActionButton(
                onClick = { viewModel.refreshPositions() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Text("↻") }
        }
        ViewMode.LIST -> BusPositionPanel(
            routeNum = selectedRouteNum,
            positions = busPositions,
            isLoading = viewModel.isLoadingPositions,
            isRefreshing = viewModel.isRefreshing,
            lastUpdatedMs = viewModel.lastUpdatedEpochMs,
            nowMs = nowMs,
            errorMessage = viewModel.errorMessage,
            onRefresh = { viewModel.refreshPositions() },
            modifier = modifier
        )
    }
}

@Composable
private fun NoRoutePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Galway Bus",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Select a route from the list to\nview live buses, stops, and map",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RouteListPanel(
    routes: List<Route>,
    selectedRouteNum: String?,
    onRouteSelected: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        if (routes.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
        }
        items(routes, key = { it.short_name }) { route ->
            RouteListItem(
                route = route,
                selected = route.short_name == selectedRouteNum,
                onClick = { onRouteSelected(route) }
            )
        }
    }
}

@Composable
private fun RouteListItem(route: Route, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .defaultMinSize(minWidth = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = route.short_name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = route.long_name,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun BusPositionPanel(
    routeNum: String,
    positions: List<BusLocation>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    lastUpdatedMs: Long?,
    nowMs: Long,
    errorMessage: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.TopStart) {
        when {
            isLoading && positions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null && positions.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Could not load buses",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRefresh) { Text("Retry") }
                }
            }
            positions.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No buses on route $routeNum",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Service may not be running right now",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    lastUpdatedMs?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Checked ${formatTimeAgo(it, nowMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onRefresh) { Text("Refresh") }
                }
            }
            else -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${positions.size} bus${if (positions.size == 1) "" else "es"} on route $routeNum",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                lastUpdatedMs?.let {
                                    Text(
                                        "Updated ${formatTimeAgo(it, nowMs)} · auto-refreshes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp).padding(2.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                TextButton(onClick = onRefresh) { Text("Refresh") }
                            }
                        }
                        HorizontalDivider()
                    }
                    items(positions, key = { it.vehicle_id ?: it.trip_duid }) { bus ->
                        BusCard(bus, nowMs)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStopsPanel(
    stops: List<Stop>,
    directionCount: Int,
    selectedDirection: Int,
    directionHeadsigns: List<String>,
    onSelectDirection: (Int) -> Unit,
    selectedStopRef: String?,
    departures: List<DepartureTime>,
    isLoadingDepartures: Boolean,
    onStopClick: (String) -> Unit,
    favouriteRefs: Set<String>,
    onToggleFavourite: (Stop) -> Unit,
    nowMs: Long,
    onDepartureClick: (DepartureTime) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        if (directionCount > 1) {
            val labels = if (directionHeadsigns.size >= directionCount) {
                directionHeadsigns.take(directionCount)
            } else {
                (0 until directionCount).map { "Direction ${it + 1}" }
            }
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                labels.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedDirection == index,
                        onClick = { onSelectDirection(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, labels.size)
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            HorizontalDivider()
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "${stops.size} stop${if (stops.size == 1) "" else "s"} · tap a stop for next departures",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            itemsIndexed(stops, key = { _, s -> s.stop_ref }) { index, stop ->
                val expanded = stop.stop_ref == selectedStopRef
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (expanded) MaterialTheme.colorScheme.surfaceContainerHighest
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onStopClick(stop.stop_ref) }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(stop.long_name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stop.stop_id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val isFavourite = stop.stop_ref in favouriteRefs
                        IconButton(onClick = { onToggleFavourite(stop) }) {
                            Text(
                                if (isFavourite) "★" else "☆",
                                fontSize = 18.sp,
                                color = if (isFavourite) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (expanded) "⌃" else "⌄",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expanded) {
                        StopDeparturesSection(
                            departures = departures,
                            isLoading = isLoadingDepartures,
                            nowMs = nowMs,
                            onDepartureClick = onDepartureClick
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StopDeparturesSection(
    departures: List<DepartureTime>,
    isLoading: Boolean,
    nowMs: Long,
    onDepartureClick: ((DepartureTime) -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().padding(start = 44.dp, end = 16.dp, bottom = 12.dp)) {
        when {
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Loading departures…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            departures.isEmpty() -> {
                Text(
                    "No more departures today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                departures.forEach { dep ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onDepartureClick != null && dep.tripId != null) {
                                if (dep.tripId != null) onDepartureClick?.invoke(dep)
                            }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                .defaultMinSize(minWidth = 26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dep.timetable_id,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                dep.display_name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (dep.vehicleId != null) {
                                Text(
                                    "🚌 #${dep.vehicleId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                dep.formatWithLive(nowMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                             )
                            if (dep.delaySeconds != null) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (dep.delaySeconds > 0) "🟥" else "🟢",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                         }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusCard(bus: BusLocation, nowMs: Long) {
    val timeAgo = formatTimeAgoIso(bus.modified_timestamp, nowMs)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = bus.headsign ?: bus.timetable_id ?: "Bus",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (bus.vehicle_id != null) {
                Text(
                    text = bus.vehicle_id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (timeAgo.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Updated $timeAgo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimeAgoIso(isoTimestamp: String, nowMs: Long): String {
    return try {
        formatTimeAgo(Instant.parse(isoTimestamp).toEpochMilliseconds(), nowMs)
    } catch (_: Exception) { "" }
}

private fun formatTimeAgo(thenMs: Long, nowMs: Long): String {
    val diffSecs = (nowMs - thenMs) / 1000
    return when {
        // Covers slightly-future timestamps from the 10s UI ticker lagging a fresh update
        diffSecs < 10 -> "just now"
        diffSecs < 60 -> "${diffSecs}s ago"
        diffSecs < 3600 -> "${diffSecs / 60}m ago"
        else -> "${diffSecs / 3600}h ago"
    }
}

