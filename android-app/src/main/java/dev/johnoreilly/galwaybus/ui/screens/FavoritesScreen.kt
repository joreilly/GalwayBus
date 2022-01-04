package dev.johnoreilly.galwaybus.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dev.johnoreilly.galwaybus.Screens
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FavoritesScreen(bottomBar: @Composable () -> Unit, viewModel: GalwayBusViewModel, navController: NavHostController) {
    val favorites by viewModel.favorites.collectAsState(setOf())

    val busStopList by viewModel.favoriteBusStopList.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    Scaffold(
            topBar = { TopAppBar(title = { Text( "Galway Bus - Favourites") }) },
            bottomBar = bottomBar
    ) {


        ModalBottomSheetLayout(modifier = Modifier.padding(it), sheetState = sheetState, sheetContent = {
            DeparturesSheetContent(viewModel) {
                viewModel.setRouteId(it.timetableId)
                navController.navigate(Screens.BusInfoScreen.route)
            }
        }) {
            BusStopListView(viewModel, busStopList, favorites) {

                val firebaseAnalytics = Firebase.analytics
                firebaseAnalytics.logEvent("select_stop") {
                    param("stop_name", it.longName)
                    param("stop_ref", it.stopRef)
                }

                coroutineScope.launch {
                    sheetState.show()
                    viewModel.setCurrentStop(it)
                }
            }
        }
    }
}