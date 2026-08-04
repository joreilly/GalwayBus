package dev.johnoreilly.galwaybus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pushpal.jetlime.EventPointType
import com.pushpal.jetlime.ItemsList
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeDefaults
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.JetLimeExtendedEvent
import dev.johnoreilly.galwaybus.location.NearbyStop
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.map.BusMapView
import androidx.compose.ui.graphics.Color
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.DepartureTime
import dev.johnoreilly.galwaybus.model.Route
import dev.johnoreilly.galwaybus.model.Stop
import dev.johnoreilly.galwaybus.scan.CameraTextScanner
import dev.johnoreilly.galwaybus.scan.isStopScanSupported
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import kotlin.time.Instant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import galwaybus.shared.generated.resources.*

private val GalwayLightColors = lightColorScheme(
    primary = Color(0xFF4f0000),
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

private val GalwayDarkColors = darkColorScheme(
    primary = Color(0xFFFFB3B4),
    onPrimary = Color(0xFF561D1E),
    primaryContainer = Color(0xFF733333),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE6BDC3),
    onSecondary = Color(0xFF44292E),
    secondaryContainer = Color(0xFF5D3F44),
    onSecondaryContainer = Color(0xFFFFD9E2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF181213),
    onSurface = Color(0xFFECDFE0),
    surfaceVariant = Color(0xFF524343),
    onSurfaceVariant = Color(0xFFD8C2C2),
    outline = Color(0xFFA08C8C),
    outlineVariant = Color(0xFF524343)
)

@Composable
fun App() {
    val repository = remember { GalwayBusRepository() }
    val viewModel = viewModel { GalwayBusViewModel(repository) }
    val colorScheme = if (isSystemInDarkTheme()) GalwayDarkColors else GalwayLightColors
    ProvideAppLocale(viewModel.appLanguage) {
        MaterialTheme(colorScheme = colorScheme) {
            GalwayBusApp(viewModel)
        }
    }
}

// Delay-status accents (colorScheme has no green slot). Late reuses colorScheme.error.
@Composable
private fun onTimeGreen(): Color =
    if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)

/**
 * In light mode the app bar carries the brand maroon; in dark mode a maroon-light-pink
 * bar would glare, so it falls back to a tonal surface like most dark UIs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun galwayAppBarColors(): TopAppBarColors =
    if (isSystemInDarkTheme()) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    } else {
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

private enum class ViewMode(val labelRes: StringResource) {
    STOPS(Res.string.view_stops), MAP(Res.string.view_map)
}

private enum class TopTab(val labelRes: StringResource, val icon: ImageVector) {
    FAVOURITES(Res.string.tab_favourites, Icons.Filled.Star),
    NEARBY(Res.string.tab_nearby, Icons.Filled.NearMe),
    SCAN(Res.string.tab_scan, Icons.Filled.QrCodeScanner),
    BUSES(Res.string.tab_buses, Icons.Filled.DirectionsBus),
    ROUTES(Res.string.tab_routes, Icons.AutoMirrored.Filled.List)
}

/** Tabs shown in the bottom bar — the camera "Scan" tab only where the platform supports it. */
private val visibleTopTabs: List<TopTab> =
    TopTab.entries.filter { it != TopTab.SCAN || isStopScanSupported }

private enum class Screen {
    MAIN, TRACKING
}

/** Bare countdown to the (live-adjusted) departure. Returns "Due", "3 min", "1h 5min", "N/A". */
@Composable
fun DepartureTime.countdownLabel(nowMs: Long): String {
    val scheduledMinutes = depart_timestamp?.let { ts ->
        ((Instant.parse(ts).toEpochMilliseconds() - nowMs) / 1000 / 60).toInt()
    } ?: return stringResource(Res.string.not_available)
    return when {
        scheduledMinutes <= 0 -> stringResource(Res.string.due)
        scheduledMinutes < 60 -> stringResource(Res.string.countdown_min, scheduledMinutes)
        else -> stringResource(Res.string.countdown_hours_min, scheduledMinutes / 60, scheduledMinutes % 60)
    }
}

/**
 * Countdown with a "(late)"/"(early)" suffix baked in, for the departures list where there's no
 * separate delay status shown. Where the delay is displayed on its own (e.g. the tracking card),
 * use [countdownLabel] instead so the lateness isn't stated twice.
 */
@Composable
fun DepartureTime.departureLabel(nowMs: Long): String {
    val countdown = countdownLabel(nowMs)
    val delaySeconds = delaySeconds ?: return countdown
    val suffix = when {
        delaySeconds > 30 -> stringResource(Res.string.late_suffix)
        delaySeconds < -30 -> stringResource(Res.string.early_suffix)
        else -> ""
    }
    return "$countdown$suffix"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun GalwayBusApp(viewModel: GalwayBusViewModel) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    val selectedRouteNum = viewModel.selectedRouteNum
    var viewMode by remember { mutableStateOf(ViewMode.STOPS) }
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }
    var topTab by remember { mutableStateOf(TopTab.FAVOURITES) }
    var showAbout by remember { mutableStateOf(false) }
    var showRestartForLanguage by remember { mutableStateOf(false) }

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

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
    if (showRestartForLanguage) {
        RestartForLanguageDialog(onDismiss = { showRestartForLanguage = false })
    }

    BoxWithConstraints {
        val compact = maxWidth < 600.dp

        // System back (Android) pops in-app navigation instead of exiting:
        // tracking screen first, then route detail in the compact layout.
        BackHandler(
            enabled = currentScreen == Screen.TRACKING ||
                (compact && topTab == TopTab.ROUTES && selectedRouteNum != null)
        ) {
            if (currentScreen == Screen.TRACKING) {
                currentScreen = Screen.MAIN
            } else {
                viewModel.clearRoute()
            }
        }

        // Departures sheet for a stop tapped on any map.
        viewModel.mapStop?.let { stop ->
            val sheetDepartures by viewModel.mapStopDepartures.collectAsStateWithLifecycle()
            ModalBottomSheet(onDismissRequest = { viewModel.clearMapStop() }) {
                MapStopSheetContent(
                    stop = stop,
                    departures = sheetDepartures,
                    isLoading = viewModel.isLoadingMapStop,
                    isFavourite = favourites.any { it.stopRef == stop.stop_ref },
                    nowMs = nowMs,
                    onToggleFavourite = { viewModel.toggleFavourite(stop) },
                    onDepartureClick = { dep ->
                        viewModel.setTrackedDeparture(dep, stop.stop_ref)
                        viewModel.clearMapStop()
                        currentScreen = Screen.TRACKING
                    }
                )
            }
        }

        if (currentScreen == Screen.TRACKING) {
            BusTrackingView(
                viewModel = viewModel,
                nowMs = nowMs,
                onBack = { currentScreen = Screen.MAIN }
            )
        } else {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (topTab) {
                                TopTab.FAVOURITES -> stringResource(Res.string.tab_favourites)
                                TopTab.NEARBY -> stringResource(Res.string.tab_nearby)
                                TopTab.SCAN -> stringResource(Res.string.title_scan_a_stop)
                                TopTab.BUSES -> stringResource(Res.string.title_all_buses)
                                TopTab.ROUTES ->
                                    if (selectedRouteNum != null) stringResource(Res.string.title_route, selectedRouteNum)
                                    else stringResource(Res.string.app_name)
                            }
                        )
                    },
                    navigationIcon = {
                        if (topTab == TopTab.ROUTES && compact && selectedRouteNum != null) {
                            IconButton(onClick = { viewModel.clearRoute() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back_to_routes))
                            }
                        }
                    },
                    actions = {
                        if (topTab == TopTab.FAVOURITES && favourites.isNotEmpty()) {
                            IconButton(onClick = { viewModel.refreshFavouriteDepartures() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.cd_refresh_departures))
                            }
                        }
                        if (topTab == TopTab.NEARBY) {
                            IconButton(onClick = { viewModel.loadNearby() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.cd_refresh_nearby))
                            }
                        }
                        if (topTab == TopTab.BUSES) {
                            IconButton(onClick = { viewModel.refreshAllBusPositions() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.cd_refresh_bus_positions))
                            }
                        }
                        if (topTab == TopTab.ROUTES && !compact && selectedRouteNum != null) {
                            ModeSwitcher(
                                viewMode = viewMode,
                                onSelect = { viewMode = it },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        AppBarOverflowMenu(
                            currentLanguage = viewModel.appLanguage,
                            onSelectLanguage = { tag ->
                                viewModel.selectAppLanguage(tag)
                                if (localeChangeRequiresRestart) showRestartForLanguage = true
                            },
                            onAbout = { showAbout = true }
                        )
                    },
                    colors = galwayAppBarColors()
                )
            },
            bottomBar = {
                NavigationBar {
                    visibleTopTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = topTab == tab,
                            onClick = {
                                topTab = tab
                                if (tab == TopTab.FAVOURITES) viewModel.refreshFavouriteDepartures()
                                if (tab == TopTab.NEARBY) viewModel.loadNearby()
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) }
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
                TopTab.NEARBY -> NearbyPanel(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
                TopTab.SCAN -> ScanStopPanel(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
                TopTab.BUSES -> AllBusesPanel(
                    viewModel = viewModel,
                    nowMs = nowMs,
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
                TopTab.ROUTES -> if (compact) {
                    if (selectedRouteNum == null) {
                        RouteListPanel(
                            routes = routes,
                            selectedRouteNum = null,
                            onRouteSelected = {
                                viewModel.selectRoute(it.short_name)
                                viewMode = ViewMode.STOPS
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
                                viewMode = ViewMode.STOPS
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
    nowMs: Long,
    modifier: Modifier = Modifier
) {
    val busPositions by viewModel.allBusPositions.collectAsStateWithLifecycle()
    val allStops by viewModel.allStops.collectAsStateWithLifecycle()

    Box(modifier) {
        BusMapView(
            positions = busPositions,
            stops = allStops,
            onStopClick = { viewModel.selectMapStop(it) },
            modifier = Modifier.fillMaxSize()
        )
        viewModel.allBusesUpdatedMs?.let { updatedMs ->
            Card(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    stringResource(Res.string.updated, timeAgoLabel(updatedMs, nowMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        // Late at night the feed is legitimately empty; say so rather than
        // leaving a silently bus-less map.
        if (busPositions.isEmpty()) {
            Card(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!viewModel.hasLoadedAllBuses) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(Res.string.loading_live_buses), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Column {
                            Text(stringResource(Res.string.no_buses_reporting), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(Res.string.service_may_not_be_running),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Scan a stop" tab (mobile only): a live camera preview that reads the 6-digit code printed on a
 * Galway bus-stop plate and opens that stop's departures. Recognised text is matched to a stop via
 * [GalwayBusViewModel.matchScannedStop]; a hit opens the shared map-stop departures sheet — the same
 * sheet used from the maps — so a scan lands in the identical departures/tracking flow.
 */
@Composable
private fun ScanStopPanel(
    viewModel: GalwayBusViewModel,
    modifier: Modifier = Modifier
) {
    // The stop we last opened the sheet for, so a stop that stays in frame doesn't re-trigger it.
    // Cleared once the sheet is dismissed (mapStop == null) so the same stop can be scanned again.
    var handledStopRef by remember { mutableStateOf<String?>(null) }
    val sheetOpen = viewModel.mapStop != null
    LaunchedEffect(sheetOpen) {
        if (!sheetOpen) handledStopRef = null
    }

    Box(modifier) {
        CameraTextScanner(
            onText = { text ->
                // Ignore frames while the departures sheet is up; resume once it's dismissed.
                if (viewModel.mapStop == null) {
                    val stop = viewModel.matchScannedStop(text)
                    if (stop != null && stop.stop_ref != handledStopRef) {
                        handledStopRef = stop.stop_ref
                        viewModel.selectMapStop(stop)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(Res.string.scan_point_camera),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * "Stops near me" tab: a map centred on the user's location with the nearest stops as markers,
 * over a distance-ordered list. Tapping a stop opens the shared map-stop departures sheet.
 * With no device fix (permission refused, or desktop) it degrades to Galway city centre.
 */
@Composable
private fun NearbyPanel(
    viewModel: GalwayBusViewModel,
    modifier: Modifier = Modifier
) {
    val nearby by viewModel.nearbyStops.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    val state = viewModel.nearbyState

    LaunchedEffect(Unit) {
        if (state is NearbyState.Idle) viewModel.loadNearby()
    }

    when {
        state is NearbyState.PermissionDenied -> NearbyMessage(
            icon = Icons.Filled.LocationOff,
            title = stringResource(Res.string.location_permission_needed),
            body = stringResource(Res.string.location_permission_body),
            actionLabel = stringResource(Res.string.enable_location),
            onAction = { viewModel.loadNearby() },
            modifier = modifier
        )
        nearby.isEmpty() && (state is NearbyState.Idle || state is NearbyState.Loading) ->
            Column(
                modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(Res.string.finding_stops),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        else -> {
            val userLocation = (state as? NearbyState.Located)?.location
            val list: @Composable (Modifier) -> Unit = { m ->
                NearbyList(
                    nearby = nearby,
                    favouriteRefs = favourites.map { it.stopRef }.toSet(),
                    isFallback = state is NearbyState.Unavailable,
                    onStopClick = { viewModel.selectMapStop(it) },
                    onToggleFavourite = { viewModel.toggleFavourite(it) },
                    modifier = m
                )
            }
            val map: @Composable (Modifier) -> Unit = { m ->
                BusMapView(
                    positions = emptyList(),
                    stops = nearby.map { it.stop },
                    userLocation = userLocation,
                    onStopClick = { viewModel.selectMapStop(it) },
                    modifier = m
                )
            }

            BoxWithConstraints(modifier) {
                if (maxWidth < 600.dp) {
                    Column(Modifier.fillMaxSize()) {
                        map(Modifier.fillMaxWidth().weight(1f))
                        HorizontalDivider()
                        list(Modifier.fillMaxWidth().weight(1f))
                    }
                } else {
                    Row(Modifier.fillMaxSize()) {
                        map(Modifier.fillMaxHeight().weight(1f))
                        Box(Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        list(Modifier.fillMaxHeight().width(320.dp))
                    }
                }
            }
        }
    }
}

/** Distance-ordered list of nearby stops, each tappable and favouritable. */
@Composable
private fun NearbyList(
    nearby: List<NearbyStop>,
    favouriteRefs: Set<String>,
    isFallback: Boolean,
    onStopClick: (Stop) -> Unit,
    onToggleFavourite: (Stop) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        item {
            Text(
                if (isFallback) stringResource(Res.string.showing_city_centre)
                else pluralStringResource(Res.plurals.stops_near_you, nearby.size, nearby.size),
                style = MaterialTheme.typography.labelMedium,
                color = if (isFallback) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        items(nearby, key = { it.stop.stop_ref }) { entry ->
            val stop = entry.stop
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onStopClick(stop) }
                    .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stop.long_name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    stop.direction?.let { dir ->
                        Text(
                            stringResource(Res.string.towards, dir),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val routesLabel = stop.routes?.takeIf { it.isNotEmpty() }
                        ?.joinToString(" · ")?.let { " · $it" } ?: ""
                    Text(
                        "${distanceLabel(entry.distanceMeters)} · ${stringResource(Res.string.stop_label, stop.stop_id)}$routesLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val isFavourite = stop.stop_ref in favouriteRefs
                IconButton(onClick = { onToggleFavourite(stop) }) {
                    Icon(
                        if (isFavourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (isFavourite) stringResource(Res.string.cd_remove_favourite) else stringResource(Res.string.cd_add_favourite),
                        tint = if (isFavourite) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

/** Centered icon + message + action, used for the nearby permission/empty states. */
@Composable
private fun NearbyMessage(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAction) { Text(actionLabel) }
    }
}

/** Human-readable distance: metres under 1 km, otherwise one decimal of a km. */
@Composable
private fun distanceLabel(meters: Double): String = when {
    meters < 1000 -> stringResource(Res.string.distance_m, meters.roundToInt())
    else -> stringResource(Res.string.distance_km, (meters / 100).roundToInt() / 10.0)
}

/** Contents of the bottom sheet shown when a stop marker is tapped on a map. */
@Composable
private fun MapStopSheetContent(
    stop: Stop,
    departures: List<DepartureTime>,
    isLoading: Boolean,
    isFavourite: Boolean,
    nowMs: Long,
    onToggleFavourite: () -> Unit,
    onDepartureClick: (DepartureTime) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stop.long_name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(Res.string.stop_label, stop.stop_id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavourite) {
                Icon(
                    if (isFavourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (isFavourite) stringResource(Res.string.cd_remove_favourite) else stringResource(Res.string.cd_add_favourite),
                    tint = if (isFavourite) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        StopDeparturesSection(
            departures = departures,
            isLoading = isLoading,
            nowMs = nowMs,
            onDepartureClick = onDepartureClick,
            startPadding = 20.dp
        )
    }
}

@Composable
private fun BusTrackingView(
    viewModel: GalwayBusViewModel,
    nowMs: Long,
    onBack: () -> Unit
) {
    val busPositions by viewModel.busPositions.collectAsStateWithLifecycle()
    val routeStops by viewModel.routeStops.collectAsStateWithLifecycle()
    val trackedTripId = viewModel.trackedTripId
    val trackedStopRef = viewModel.trackedStopRef
    val trackedDeparture = viewModel.trackedDeparture

    // Show only the bus for the departure the user tapped, plus the targeted stop.
    // The departure's tripId isn't always the matched vehicle's trip_duid (the repo may
    // match a vehicle by headsign), so prefer matching on the reliable vehicleId.
    // If we can't match at all, the map shows just the stop with no bus marker.
    val trackedVehicleId = trackedDeparture?.vehicleId
    val displayPositions = busPositions.filter {
        (trackedVehicleId != null && it.vehicle_id == trackedVehicleId) || it.trip_duid == trackedTripId
    }
    // Centering/highlighting keys off trip_duid, so use the matched bus's own trip id.
    val trackedBusTripId = displayPositions.firstOrNull()?.trip_duid ?: trackedTripId

    val trackedStop = routeStops.flatten().filter { it.stop_ref == trackedStopRef }.distinctBy { it.stop_ref }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    // Follows the bar's content colour so it adapts to light/dark bar styles.
                    val barContentColor = LocalContentColor.current
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.track_bus))
                            trackedDeparture?.timetable_id?.let { routeNum ->
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .background(
                                            color = barContentColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .defaultMinSize(minWidth = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = routeNum,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = barContentColor
                                    )
                                }
                            }
                        }
                        trackedDeparture?.display_name?.let { destination ->
                            Text(
                                text = destination,
                                style = MaterialTheme.typography.bodySmall,
                                color = barContentColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
                colors = galwayAppBarColors()
            )
        }
    ) { padding ->
        // Stops for the direction the tracked stop belongs to (fall back to the first direction).
        val timelineStops = routeStops.firstOrNull { dir -> dir.any { it.stop_ref == trackedStopRef } }
            ?: routeStops.firstOrNull().orEmpty()

        val mapArea: @Composable (Modifier) -> Unit = { m ->
            Box(m) {
                BusMapView(
                    positions = displayPositions,
                    stops = trackedStop,
                    trackedTripId = trackedBusTripId,
                    trackedStopRef = trackedStopRef,
                    modifier = Modifier.fillMaxSize()
                )
                TrackingInfoCard(
                    departure = trackedDeparture,
                    trackedBus = displayPositions.firstOrNull(),
                    stopName = trackedStop.firstOrNull()?.long_name,
                    nowMs = nowMs,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
        val timeline: @Composable (Modifier) -> Unit = { m ->
            RouteTimeline(
                stops = timelineStops,
                targetStopRef = trackedStopRef,
                trackedBus = displayPositions.firstOrNull(),
                nowMs = nowMs,
                modifier = m
            )
        }

        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            // Too narrow for a side panel → stack the stop list under the map (bottom third).
            if (maxWidth < 600.dp) {
                Column(Modifier.fillMaxSize()) {
                    mapArea(Modifier.fillMaxWidth().weight(2f))
                    if (timelineStops.isNotEmpty()) {
                        HorizontalDivider()
                        timeline(Modifier.fillMaxWidth().weight(1f))
                    }
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    mapArea(Modifier.fillMaxHeight().weight(1f))
                    if (timelineStops.isNotEmpty()) {
                        Box(Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        timeline(Modifier.fillMaxHeight().width(300.dp))
                    }
                }
            }
        }
    }
}

/**
 * Vertical journey timeline (JetLime) of the route's stops. Stops the bus has already
 * passed are checked, the stop it's currently nearest gets an animated node, and the
 * user's target stop is highlighted.
 */
@OptIn(ExperimentalComposeApi::class)
@Composable
private fun RouteTimeline(
    stops: List<Stop>,
    targetStopRef: String?,
    trackedBus: BusLocation?,
    nowMs: Long,
    modifier: Modifier = Modifier
) {
    // Where the bus is along the route, for the passed/next markers. Prefers the real-time
    // next_stop_ref but ignores it when it contradicts the vehicle's GPS position (see
    // resolveBusIndex).
    val busIndex = remember(stops, trackedBus) { resolveBusIndex(stops, trackedBus) }
    // Predicted departure time per stop (ISO timestamp) from the live next_stops payload.
    val etaByStopRef = remember(trackedBus) {
        trackedBus?.next_stops
            ?.mapNotNull { p -> (p.departure_timestamp ?: p.arrival_timestamp)?.let { p.stop_ref to it } }
            ?.toMap()
            ?: emptyMap()
    }
    val targetIndex = remember(stops, targetStopRef) {
        stops.indexOfFirst { it.stop_ref == targetStopRef }
    }

    val checkPainter = rememberVectorPainter(Icons.Filled.Check)
    val targetPainter = rememberVectorPainter(Icons.Filled.Place)
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val passedColor = MaterialTheme.colorScheme.secondary

    val listState = rememberLazyListState()
    // Keep the bus's current position (or the target) centred in the viewport, so
    // the stops just passed and the stops coming up are both visible around it.
    LaunchedEffect(busIndex, targetIndex, stops.size) {
        val focus = if (busIndex >= 0) busIndex else targetIndex
        if (focus < 0) return@LaunchedEffect
        // Wait for the first layout pass so the viewport height is known.
        snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
        val viewport = listState.layoutInfo.viewportSize.height
        val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
        listState.animateScrollToItem(focus, -(viewport - itemHeight) / 2)
    }

    JetLimeColumn(
        itemsList = ItemsList(stops),
        listState = listState,
        modifier = modifier.padding(start = 4.dp, top = 12.dp, end = 12.dp),
        key = { _, stop -> stop.stop_ref },
        style = JetLimeDefaults.columnStyle(lineBrush = JetLimeDefaults.lineSolidBrush(primary))
    ) { index, stop, position ->
        val hasPassed = busIndex >= 0 && index < busIndex
        val isCurrent = busIndex >= 0 && index == busIndex
        val isTarget = stop.stop_ref == targetStopRef

        // Predicted arrival for this stop ("Due" / "3 min" / "1h 9min"), if the live
        // next_stops payload covers it. Shown in the left gutter for stops still ahead.
        val etaText = etaByStopRef[stop.stop_ref]?.let { ts ->
            val mins = ((Instant.parse(ts).toEpochMilliseconds() - nowMs) / 1000 / 60).toInt()
            when {
                mins <= 0 -> stringResource(Res.string.due)
                mins < 60 -> stringResource(Res.string.gutter_min, mins)
                else -> stringResource(Res.string.gutter_hours_min, mins / 60, mins % 60)
            }
        }

        JetLimeExtendedEvent(
            style = JetLimeEventDefaults.eventStyle(
                position = position,
                pointType = when {
                    isTarget -> EventPointType.custom(icon = targetPainter, tint = MaterialTheme.colorScheme.onPrimary)
                    hasPassed -> EventPointType.custom(icon = checkPainter, tint = MaterialTheme.colorScheme.onPrimary)
                    isCurrent -> EventPointType.filled()
                    else -> EventPointType.EMPTY
                },
                pointFillColor = when {
                    isTarget -> primary
                    isCurrent -> primary
                    hasPassed -> passedColor
                    else -> MaterialTheme.colorScheme.surface
                },
                pointColor = if (isTarget || isCurrent) primary else onSurfaceVariant,
                pointStrokeColor = if (isTarget || isCurrent) primary else onSurfaceVariant,
                pointAnimation = if (isCurrent) JetLimeEventDefaults.pointAnimation() else null
            ),
            additionalContentMaxWidth = 52.dp,
            additionalContent = {
                // Minutes-to-arrival, right-aligned against the line. Only for stops the
                // bus hasn't reached yet; passed stops leave the gutter blank. The box is
                // always the full width so every node lines up regardless of gutter text.
                Box(
                    Modifier.width(52.dp).padding(end = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (!hasPassed && etaText != null) {
                        Text(
                            etaText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTarget || isCurrent) primary else onSurfaceVariant,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        ) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    stop.long_name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasPassed) onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isTarget || isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // "Next stop"/"Your stop" get an emphasised label with the stop id beneath;
                // every other stop just shows its id.
                val label = when {
                    isTarget -> stringResource(Res.string.your_stop)
                    isCurrent -> stringResource(Res.string.next_stop)
                    else -> null
                }
                label?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = primary
                    )
                }
                Text(
                    stop.stop_id,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
            }
        }
    }
}

/** Floating summary over the tracking map: ETA to the stop, live delay status, and position freshness. */
@Composable
private fun TrackingInfoCard(
    departure: DepartureTime?,
    trackedBus: BusLocation?,
    stopName: String?,
    nowMs: Long,
    modifier: Modifier = Modifier
) {
    departure ?: return

    val delaySeconds = departure.delaySeconds
    val statusText = when {
        delaySeconds == null -> null
        delaySeconds > 30 -> stringResource(Res.string.min_late, (delaySeconds + 30) / 60)
        delaySeconds < -30 -> stringResource(Res.string.min_early, (-delaySeconds + 30) / 60)
        else -> stringResource(Res.string.on_time)
    }
    val statusColor = when {
        delaySeconds != null && delaySeconds > 30 -> MaterialTheme.colorScheme.error
        else -> onTimeGreen()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.DirectionsBus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stopName?.let { stringResource(Res.string.arriving_at, it) } ?: stringResource(Res.string.arriving),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        // Plain countdown — the delay is shown separately as statusText below,
                        // so we avoid the ambiguous "15 min (late)" that reads as "15 min late".
                        text = departure.countdownLabel(nowMs),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (statusText != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor
                        )
                    }
                }
                val freshness = trackedBus?.let { timeAgoIsoLabel(it.modified_timestamp, nowMs) }
                Text(
                    text = when {
                        trackedBus == null -> stringResource(Res.string.bus_not_reporting)
                        freshness.isNullOrEmpty() -> stringResource(Res.string.live_position)
                        else -> stringResource(Res.string.position_updated, freshness)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val allStops by viewModel.allStops.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    val searching = query.trim().length >= 2
    // Match by name or stop code, prefix matches first so short queries stay useful.
    val searchResults = remember(allStops, query) {
        val q = query.trim()
        if (q.length < 2) emptyList()
        else allStops
            .filter { it.long_name.contains(q, ignoreCase = true) || it.stop_id.contains(q, ignoreCase = true) }
            .sortedWith(
                compareBy(
                    { !it.long_name.startsWith(q, ignoreCase = true) && !it.stop_id.startsWith(q, ignoreCase = true) },
                    { it.long_name }
                )
            )
            .take(30)
    }

    LaunchedEffect(Unit) { viewModel.refreshFavouriteDepartures() }

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(Res.string.search_all_stops), style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.cd_clear_search))
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(24.dp)
        )

        when {
            searching -> StopSearchResults(
                results = searchResults,
                favouriteRefs = favourites.map { it.stopRef }.toSet(),
                onToggleFavourite = { viewModel.toggleFavourite(it) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            favourites.isEmpty() -> Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(Res.string.no_saved_stops),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.no_saved_stops_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBrowseRoutes) { Text(stringResource(Res.string.browse_routes)) }
            }
            else -> PullToRefreshBox(
                isRefreshing = viewModel.isLoadingFavourites,
                onRefresh = { viewModel.refreshFavouriteDepartures() },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            pluralStringResource(Res.plurals.saved_stops, favourites.size, favourites.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    items(favourites, key = { it.stopRef }) { fav ->
                        FavouriteStopCard(
                            name = fav.name,
                            stopId = fav.stopId,
                            departures = departuresByStop[fav.stopRef],
                            nowMs = nowMs,
                            updatedMs = viewModel.favouritesUpdatedMs,
                            isRefreshing = viewModel.isLoadingFavourites,
                            onRemove = { viewModel.removeFavourite(fav.stopRef) },
                            onDepartureClick = { onDepartureClick(it, fav.stopRef) }
                        )
                    }
                }
            }
        }
    }
}

/** Search hits across every stop in the network, each with a star to favourite it directly. */
@Composable
private fun StopSearchResults(
    results: List<Stop>,
    favouriteRefs: Set<String>,
    onToggleFavourite: (Stop) -> Unit,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) {
        Box(modifier.padding(24.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                stringResource(Res.string.no_stops_match),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(modifier) {
        items(results, key = { it.stop_ref }) { stop ->
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stop.long_name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // The destination distinguishes opposite-direction stops that share a name.
                    stop.direction?.let { dir ->
                        Text(
                            stringResource(Res.string.towards, dir),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val routesLabel = stop.routes?.takeIf { it.isNotEmpty() }
                        ?.joinToString(" · ")?.let { " · $it" } ?: ""
                    Text(
                        "${stringResource(Res.string.stop_label, stop.stop_id)}$routesLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val isFavourite = stop.stop_ref in favouriteRefs
                IconButton(onClick = { onToggleFavourite(stop) }) {
                    Icon(
                        if (isFavourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (isFavourite) stringResource(Res.string.cd_remove_favourite) else stringResource(Res.string.cd_add_favourite),
                        tint = if (isFavourite) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

private val FAVOURITE_DEPARTURES_INDENT = 64.dp

/** Age at which auto-refreshed live data is flagged as stale (refresh cycle is 30s). */
private const val STALE_AFTER_MS = 120_000L

/** Trailing chevron marking a departure row as tappable (opens bus tracking). */
@Composable
private fun TrackChevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = stringResource(Res.string.cd_track_this_bus),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavouriteStopCard(
    name: String,
    stopId: String,
    departures: List<DepartureTime>?,
    nowMs: Long,
    updatedMs: Long?,
    isRefreshing: Boolean,
    onRemove: () -> Unit,
    onDepartureClick: (DepartureTime) -> Unit
) {
    // Routes serving this stop, derived from its upcoming departures.
    val routes = departures?.map { it.timetable_id }?.distinct()?.sorted().orEmpty()

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(Res.string.stop_label, stopId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (routes.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            routes.forEach { RouteChip(it) }
                        }
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(Res.string.cd_remove_favourite),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            when {
                departures == null -> Row(
                    Modifier.fillMaxWidth().padding(start = FAVOURITE_DEPARTURES_INDENT, end = 16.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.loading_departures),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                departures.isEmpty() -> Text(
                    stringResource(Res.string.no_more_departures),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                else -> FavouriteDepartures(departures, nowMs, onDepartureClick)
            }

            // Freshness footer; goes loud once the data is old enough to mislead.
            if (updatedMs != null) {
                val stale = !isRefreshing && nowMs - updatedMs > STALE_AFTER_MS
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(Modifier.size(10.dp), strokeWidth = 1.5.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        when {
                            isRefreshing -> "Updating…"
                            stale -> stringResource(Res.string.last_updated_retrying, timeAgoLabel(updatedMs, nowMs))
                            else -> stringResource(Res.string.updated, timeAgoLabel(updatedMs, nowMs))
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stale) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** A small outlined pill listing a route that serves the stop (metadata, not a live departure). */
@Composable
private fun RouteChip(route: String) {
    Box(
        Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .defaultMinSize(minWidth = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            route,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Route number badge against a departure. The `large` (next-bus) badge is filled with the
 * brand maroon to anchor the card; smaller "later" badges use the light container tint.
 */
@Composable
private fun RouteBadge(route: String, large: Boolean) {
    val bg = if (large) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val fg = if (large) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        Modifier
            .background(bg, RoundedCornerShape(if (large) 6.dp else 4.dp))
            .padding(horizontal = if (large) 8.dp else 5.dp, vertical = if (large) 4.dp else 2.dp)
            .defaultMinSize(minWidth = if (large) 34.dp else 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            route,
            style = if (large) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
            color = fg
        )
    }
}

@Composable
private fun departureCountdownColor(delaySeconds: Int?): Color = when {
    delaySeconds != null && delaySeconds > 30 -> MaterialTheme.colorScheme.error
    delaySeconds != null && delaySeconds < -30 -> onTimeGreen()
    else -> MaterialTheme.colorScheme.primary
}

/** Next departure as a prominent hero, then up to 3 more compact rows. */
@Composable
private fun FavouriteDepartures(
    departures: List<DepartureTime>,
    nowMs: Long,
    onDepartureClick: (DepartureTime) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val next = departures.first()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = next.tripId != null) { onDepartureClick(next) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RouteBadge(next.timetable_id, large = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    next.display_name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (next.vehicleId != null) {
                    Text(
                        "🚌 #${next.vehicleId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                next.departureLabel(nowMs),
                style = MaterialTheme.typography.titleLarge,
                color = departureCountdownColor(next.delaySeconds)
            )
            if (next.tripId != null) {
                Spacer(Modifier.width(4.dp))
                TrackChevron()
            }
        }

        val later = departures.drop(1).take(3)
        if (later.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 2.dp))
            later.forEach { dep ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = dep.tripId != null) { onDepartureClick(dep) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RouteBadge(dep.timetable_id, large = false)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dep.display_name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dep.departureLabel(nowMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = departureCountdownColor(dep.delaySeconds)
                    )
                    if (dep.tripId != null) {
                        Spacer(Modifier.width(4.dp))
                        TrackChevron()
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSwitcher(
    viewMode: ViewMode,
    onSelect: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Rendered both on the maroon app bar and on plain surface; follow the ambient
    // content colour so unselected labels stay readable in both places.
    val contentColor = LocalContentColor.current
    val colors = SegmentedButtonDefaults.colors(
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = contentColor,
        activeBorderColor = contentColor.copy(alpha = 0.6f),
        inactiveBorderColor = contentColor.copy(alpha = 0.6f)
    )
    SingleChoiceSegmentedButtonRow(modifier) {
        ViewMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = viewMode == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, ViewMode.entries.size),
                colors = colors
            ) { Text(stringResource(mode.labelRes)) }
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
                    onStopClick = { viewModel.selectMapStop(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            SmallFloatingActionButton(
                onClick = { viewModel.refreshPositions() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.cd_refresh_positions)) }
        }
    }
}

/** Three-dot app-bar menu: language selection (English / Gaeilge) and the About dialog. */
@Composable
private fun AppBarOverflowMenu(
    currentLanguage: String?,
    onSelectLanguage: (String?) -> Unit,
    onAbout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.cd_more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        Text(
            stringResource(Res.string.language),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LanguageMenuItem(Res.string.language_english, "en", currentLanguage) { onSelectLanguage(it); expanded = false }
        LanguageMenuItem(Res.string.language_irish, "ga", currentLanguage) { onSelectLanguage(it); expanded = false }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.about)) },
            onClick = {
                expanded = false
                onAbout()
            }
        )
    }
}

@Composable
private fun LanguageMenuItem(
    labelRes: StringResource,
    tag: String,
    currentLanguage: String?,
    onSelect: (String?) -> Unit
) {
    // Match on the primary language subtag so "ga-IE" still reads as Irish.
    val selected = currentLanguage?.substringBefore('-') == tag
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        leadingIcon = {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Spacer(Modifier.width(24.dp))
            }
        },
        onClick = { onSelect(tag) }
    )
}

/** Shown on iOS after a language change, where the switch only applies on next launch. */
@Composable
private fun RestartForLanguageDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.restart_to_apply_title)) },
        text = { Text(stringResource(Res.string.restart_to_apply_body)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ok)) } }
    )
}

/** Small about/version dialog, sourcing the version from the platform's installed package. */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val platform = remember { getPlatform() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.DirectionsBus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(Res.string.app_name)) },
        text = {
            Text(
                stringResource(Res.string.version, platform.appVersion.substringBefore(" (")),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } }
    )
}

@Composable
private fun NoRoutePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.select_route),
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
                (0 until directionCount).map { stringResource(Res.string.direction_numbered, it + 1) }
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
                    pluralStringResource(Res.plurals.route_stops_hint, stops.size, stops.size),
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
                            Icon(
                                if (isFavourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = if (isFavourite) stringResource(Res.string.cd_remove_favourite) else stringResource(Res.string.cd_add_favourite),
                                tint = if (isFavourite) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    onDepartureClick: ((DepartureTime) -> Unit)? = null,
    startPadding: Dp = 44.dp
) {
    Column(Modifier.fillMaxWidth().padding(start = startPadding, end = 16.dp, bottom = 12.dp)) {
        when {
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.loading_departures),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            departures.isEmpty() -> {
                Text(
                    stringResource(Res.string.no_more_departures),
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
                        // Colour the countdown by live delay; the text itself already reads "(late)"/"(early)".
                        Text(
                            dep.departureLabel(nowMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = departureCountdownColor(dep.delaySeconds)
                        )
                        if (onDepartureClick != null && dep.tripId != null) {
                            Spacer(Modifier.width(4.dp))
                            TrackChevron()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun timeAgoIsoLabel(isoTimestamp: String, nowMs: Long): String {
    val thenMs = try {
        Instant.parse(isoTimestamp).toEpochMilliseconds()
    } catch (_: Exception) { return "" }
    return timeAgoLabel(thenMs, nowMs)
}

@Composable
private fun timeAgoLabel(thenMs: Long, nowMs: Long): String {
    val diffSecs = (nowMs - thenMs) / 1000
    return when {
        // Covers slightly-future timestamps from the 10s UI ticker lagging a fresh update
        diffSecs < 10 -> stringResource(Res.string.time_just_now)
        diffSecs < 60 -> stringResource(Res.string.time_secs_ago, diffSecs.toInt())
        diffSecs < 3600 -> stringResource(Res.string.time_mins_ago, (diffSecs / 60).toInt())
        else -> stringResource(Res.string.time_hours_ago, (diffSecs / 3600).toInt())
    }
}

