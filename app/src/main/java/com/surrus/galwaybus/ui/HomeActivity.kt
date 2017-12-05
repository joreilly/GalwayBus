package com.surrus.galwaybus.ui

import android.os.Bundle


import com.surrus.galwaybus.R
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModel
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModelFactory
import com.surrus.galwaybus.util.ext.observe
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject


class HomeActivity : AppCompatActivity() {

    @Inject lateinit var busRoutesViewModelFactory: BusRoutesViewModelFactory

    private lateinit var busRoutesViewModel : BusRoutesViewModel
    private lateinit var busRoutesAdapter: BusRoutesRecyclerViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        AndroidInjection.inject(this)

        busRoutesViewModel = ViewModelProviders.of(this, busRoutesViewModelFactory).get(BusRoutesViewModel::class.java)

        with (busRoutesList) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@HomeActivity)

            busRoutesAdapter = BusRoutesRecyclerViewAdapter()
            adapter = busRoutesAdapter
        }


        busRoutesViewModel.getBusRoutes().observe(this) {
            busRoutesAdapter.busRouteList = ArrayList(it)
            busRoutesAdapter.notifyDataSetChanged()
        }
    }


}
