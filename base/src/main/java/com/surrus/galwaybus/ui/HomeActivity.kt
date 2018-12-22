package com.surrus.galwaybus.ui

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle


import com.surrus.galwaybus.base.R
import androidx.fragment.app.Fragment
import androidx.core.view.MenuItemCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.instantapps.InstantApps
//import com.google.ar.core.Config
//import com.google.ar.core.Session
import com.google.firebase.analytics.FirebaseAnalytics
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class HomeActivity : AppCompatActivity() {

    private val firebaseAnaltyics by inject<FirebaseAnalytics>()
    private val galwayRepository by inject<GalwayBusRepository>()

    private val nearestBusStopsViewModel by viewModel<NearestBusStopsViewModel>()

    //private var arCoreSession: Session? = null

    private val SELECTED_ITEM = "arg_selected_item"
    private var selectedItem = 0


    private lateinit var searchResultsStopsAdapter: BusStopsRecyclerViewAdapter
    private var searchMenuItem: MenuItem? = null
    private var activeFragment: Fragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Logger.d("onCreate")

        searchResultsStopsAdapter = BusStopsRecyclerViewAdapter {
            nearestBusStopsViewModel.setLocation(Location(it.latitude, it.longitude))
            searchMenuItem?.collapseActionView()
            selectFragment(bottomNavigation.menu.getItem(0))
        }
        searchResultsList.adapter = searchResultsStopsAdapter


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


        val selectedMenuItem: MenuItem
        if (savedInstanceState != null) {
            selectedItem = savedInstanceState.getInt(SELECTED_ITEM, 0)
            selectedMenuItem = bottomNavigation.menu.findItem(selectedItem)
        } else {
            selectedMenuItem = bottomNavigation.menu.getItem(0)
        }
        selectFragment(selectedMenuItem)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            selectFragment(item)
            true
        }
    }


    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(SELECTED_ITEM, selectedItem)
        super.onSaveInstanceState(outState)
    }

    private fun selectFragment(item: MenuItem) {
        when (item.itemId) {
            R.id.navigation_nearby -> {
                activeFragment = NearbyFragment.newInstance()
            }
            R.id.navigation_routes -> {
                activeFragment = RoutesFragment.newInstance()
            }
        }

        if (activeFragment != null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.fragmentContainer, activeFragment!!).commit()
        }


        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, item.toString())
        firebaseAnaltyics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        // update selected item
        selectedItem = item.itemId
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        // check if instant app
        val installMenuItem = menu.findItem(R.id.action_install)
        if (InstantApps.isInstantApp(this)) {
            installMenuItem.isVisible = true
        }

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.isSubmitButtonEnabled = false

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                searchMenuItem?.collapseActionView()
                return false
            }

            @SuppressLint("CheckResult")
            override fun onQueryTextChange(query: String): Boolean {
                if (query.length >= 2) {

                    GlobalScope.launch  {
                        val busStops = galwayRepository.getBusStopsByName("%$query%")

                        withContext(Dispatchers.Main) {
                            searchResultsStopsAdapter.busStopList = busStops
                            searchResultsStopsAdapter.notifyDataSetChanged()
                        }
                    }
                }
                return false
            }
        })


        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                bottomNavigation.visibility = View.GONE

                if (activeFragment != null) {
                    supportFragmentManager.beginTransaction().hide(activeFragment!!).commit()
                }

                searchResultsList.visibility = View.VISIBLE
                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                searchResultsList.visibility = View.GONE
                bottomNavigation.visibility = View.VISIBLE

                if (activeFragment != null) {
                    supportFragmentManager.beginTransaction().show(activeFragment!!).commit()
                }
                return true
            }
        })



        return true
    }


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
            R.id.action_install -> {
                val postInstallIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.surrus.com/galwaybus")).addCategory(Intent.CATEGORY_BROWSABLE)

                InstantApps.showInstallPrompt(this, postInstallIntent, 1, "")
            }

        }


        return super.onOptionsItemSelected(item)
    }

}
