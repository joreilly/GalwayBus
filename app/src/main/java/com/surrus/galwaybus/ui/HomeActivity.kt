package com.surrus.galwaybus.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle


import com.surrus.galwaybus.R
import androidx.core.view.MenuItemCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
//import androidx.navigation.ui.setupWithNavController
//import com.google.ar.core.Config
//import com.google.ar.core.Session
import com.google.firebase.analytics.FirebaseAnalytics
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class HomeActivity : AppCompatActivity() {
    private val firebaseAnaltyics by inject<FirebaseAnalytics>()
    private val nearestBusStopsViewModel by viewModel<NearestBusStopsViewModel>()

    private lateinit var searchResultsStopsAdapter: BusStopsRecyclerViewAdapter
    private var searchMenuItem: MenuItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Logger.d("onCreate")

        searchResultsStopsAdapter = BusStopsRecyclerViewAdapter {
            nearestBusStopsViewModel.setLocation(Location(it.latitude, it.longitude))
            searchMenuItem?.collapseActionView()
            //selectFragment(bottomNavigation.menu.getItem(0))
        }
        searchResultsList.adapter = searchResultsStopsAdapter

        // Setup Nav Controller
        val navController = Navigation.findNavController(this, R.id.nav_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController)
        bottomNavigation.setupWithNavController(navController)
  }


    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(Navigation.findNavController(this, R.id.nav_fragment), null)
    }


//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.main, menu)
//
///*
//        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
//        searchMenuItem = menu.findItem(R.id.action_search)
//        val searchView = searchMenuItem?.actionView as SearchView
//        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
//        searchView.isSubmitButtonEnabled = false
//
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//
//            override fun onQueryTextSubmit(query: String): Boolean {
//                searchMenuItem?.collapseActionView()
//                return false
//            }
//
//            @SuppressLint("CheckResult")
//            override fun onQueryTextChange(query: String): Boolean {
//                if (query.length >= 2) {
//
//                    GlobalScope.launch  {
//                        val busStops = galwayRepository.getBusStopsByName("%$query%")
//
//                        withContext(Dispatchers.Main) {
//                            searchResultsStopsAdapter.busStopList = busStops
//                            searchResultsStopsAdapter.notifyDataSetChanged()
//                        }
//                    }
//                }
//                return false
//            }
//        })
//
//
//        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
//            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
//                bottomNavigation.visibility = View.GONE
//
//                if (activeFragment != null) {
//                    supportFragmentManager.beginTransaction().hide(activeFragment!!).commit()
//                }
//
//                searchResultsList.visibility = View.VISIBLE
//                return true
//            }
//
//            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
//                searchResultsList.visibility = View.GONE
//                bottomNavigation.visibility = View.VISIBLE
//
//                if (activeFragment != null) {
//                    supportFragmentManager.beginTransaction().show(activeFragment!!).commit()
//                }
//                return true
//            }
//        })
//*/
//
//
//        return true
//    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_centre -> {
                nearestBusStopsViewModel.setZoomLevel(15.0f)
                nearestBusStopsViewModel.setCameraPosition(Location(53.2743394, -9.0514163))

                val bundle = Bundle()
                bundle.putString("menu_option", "action_centre")
                firebaseAnaltyics.logEvent("menu_selected", bundle)

                return true
            }
        }


        return super.onOptionsItemSelected(item)
    }


    private fun setupArCore() {
/*
        try {
            arCoreSession = Session(this)

            val config = Config(arCoreSession)
            if (!arCoreSession!!.isSupported(config)) {
                Logger.d("ARCore not installed")
                //showSnackbarMessage("This device does not support AR", true)
            } else {
                arCoreSession?.configure(config)
            }
        } catch (ex: Throwable) {
            Logger.d("ARCore not installed")
        }
*/

    }
}
