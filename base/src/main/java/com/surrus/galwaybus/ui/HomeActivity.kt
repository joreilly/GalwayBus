package com.surrus.galwaybus.ui

import android.app.SearchManager
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle


import com.surrus.galwaybus.base.R
import android.support.v4.app.Fragment
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.firebase.analytics.FirebaseAnalytics
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModelFactory
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import dagger.android.AndroidInjector
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class HomeActivity : AppCompatActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var firebaseAnaltyics: FirebaseAnalytics

    @Inject
    lateinit var nearestBusStopsViewModelFactory: NearestBusStopsViewModelFactory

    @Inject lateinit var galwayRepository: GalwayBusRepository


    private lateinit var nearestBusStopsViewModel : NearestBusStopsViewModel

    //private var arCoreSession: Session? = null

    private val SELECTED_ITEM = "arg_selected_item"
    private var selectedItem = 0


    private lateinit var searchResultsStopsAdapter: BusStopsRecyclerViewAdapter
    private var searchMenuItem: MenuItem? = null
    private var activeFragment: Fragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Logger.d("onCreate")

        nearestBusStopsViewModel = ViewModelProviders.of(this, nearestBusStopsViewModelFactory).get(NearestBusStopsViewModel::class.java)



        searchResultsStopsAdapter = BusStopsRecyclerViewAdapter {
            nearestBusStopsViewModel.setLocation(Location(it.latitude, it.longitude))
            searchMenuItem?.collapseActionView()
            selectFragment(bottomNavigation.getMenu().getItem(0))
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
            selectedMenuItem = bottomNavigation.getMenu().findItem(selectedItem)
        } else {
            selectedMenuItem = bottomNavigation.getMenu().getItem(0)
        }
        selectFragment(selectedMenuItem)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            selectFragment(item)
            true
        }
    }


    public override fun onResume() {
        super.onResume();
        Logger.d("onResume")
    }


    public override fun onPause() {
        super.onPause();
        Logger.d("onPause")
    }

    public override fun onDestroy() {
        super.onDestroy();
        Logger.d("onDestroy")
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
            ft.replace(R.id.fragmentContainer, activeFragment).commit()
        }


        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, item.toString())
        firebaseAnaltyics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        // update selected item
        selectedItem = item.getItemId()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem?.getActionView() as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setSubmitButtonEnabled(false)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                searchMenuItem?.collapseActionView()
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (query.length >= 2) {
                    galwayRepository.getBusStopsByName("%$query%")
                            .debounce(300, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                searchResultsStopsAdapter.busStopList = it
                                searchResultsStopsAdapter.notifyDataSetChanged()
                            }
                }
                return false
            }
        })


        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                bottomNavigation.visibility = View.GONE

                if (activeFragment != null) {
                    supportFragmentManager.beginTransaction().hide(activeFragment).commit()
                }

                searchResultsList.visibility = View.VISIBLE
                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                searchResultsList.visibility = View.GONE
                bottomNavigation.visibility = View.VISIBLE

                if (activeFragment != null) {
                    supportFragmentManager.beginTransaction().show(activeFragment).commit()
                }
                return true
            }
        })



        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.action_centre) {
            nearestBusStopsViewModel.setZoomLevel(15.0f)
            nearestBusStopsViewModel.setCameraPosition(Location(53.2743394, -9.0514163))

            val bundle = Bundle()
            bundle.putString("menu_option", "action_centre")
            firebaseAnaltyics.logEvent("menu_selected", bundle)

            return true
        }

//        else if (id == R.id.action_view_ar) {
//            val intent = Intent(this, ArActivity::class.java)
//            startActivity(intent)
//            return true
//        }

        return super.onOptionsItemSelected(item)
    }


    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentDispatchingAndroidInjector
    }


}
