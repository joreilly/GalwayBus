package com.surrus.galwaybus.ui

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle

import com.surrus.galwaybus.R
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.analytics.FirebaseAnalytics
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModelFactory
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import dagger.android.AndroidInjector


class HomeActivity : AppCompatActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var firebaseAnaltyics: FirebaseAnalytics

    @Inject
    lateinit var nearestBusStopsViewModelFactory: NearestBusStopsViewModelFactory


    private lateinit var nearestBusStopsViewModel : NearestBusStopsViewModel


    private val SELECTED_ITEM = "arg_selected_item"
    private var selectedItem = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Logger.d("onCreate")

        nearestBusStopsViewModel = ViewModelProviders.of(this, nearestBusStopsViewModelFactory).get(NearestBusStopsViewModel::class.java)

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
        var frag: Fragment? = null

        when (item.itemId) {
            R.id.navigation_nearby -> {
                frag = NearbyFragment.newInstance()
            }
            R.id.navigation_routes -> {
                frag = RoutesFragment.newInstance()
            }
        }

        if (frag != null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.fragmentContainer, frag).commit()
        }


        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, item.toString())
        firebaseAnaltyics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        // update selected item
        selectedItem = item.getItemId()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
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
            nearestBusStopsViewModel.setCameraPosition(Location(53.273849, -9.049695))

            val bundle = Bundle()
            bundle.putString("menu_option", "action_centre")
            firebaseAnaltyics.logEvent("menu_selected", bundle)

            return true
        }

        return super.onOptionsItemSelected(item)
    }


    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentDispatchingAndroidInjector
    }
}
