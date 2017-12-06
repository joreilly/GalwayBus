package com.surrus.galwaybus.ui

import android.os.Bundle

import com.surrus.galwaybus.R
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import dagger.android.AndroidInjector


class HomeActivity : AppCompatActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    private val SELECTED_ITEM = "arg_selected_item"
    private var selectedItem = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
            ft.replace(R.id.container, frag).commit()
        }

        // update selected item
        selectedItem = item.getItemId()
    }


    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentDispatchingAndroidInjector
    }
}
