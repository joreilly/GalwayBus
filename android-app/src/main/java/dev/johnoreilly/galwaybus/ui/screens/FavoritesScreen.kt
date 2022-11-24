@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)

package dev.johnoreilly.galwaybus.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.Screens
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(viewModel: GalwayBusViewModel, navController: NavHostController) {
    val favorites by viewModel.favorites.collectAsState()
    val busStopList by viewModel.favoriteBusStopList.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { androidx.compose.material3.Text("Galway Bus - Favourites") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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