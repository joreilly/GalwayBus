package com.surrus.galwaybus.ui


import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surrus.galwaybus.Constants

import com.surrus.galwaybus.base.R
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModelFactory
import com.surrus.galwaybus.util.ext.observe
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_stops.*
import javax.inject.Inject


class StopsFragment : Fragment() {

    private var routeId: String? = null
    private var direction: Int? = null

    @Inject lateinit var busStopsViewModelFactory: BusStopsViewModelFactory

    private lateinit var busStopsViewModel : BusStopsViewModel
    private lateinit var busStopsAdapter: BusStopsRecyclerViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            routeId = it.getString(Constants.ROUTE_ID)
            direction = it.getInt(Constants.DIRECTION)
        }

        busStopsViewModel = ViewModelProviders.of(activity!!, busStopsViewModelFactory).get(BusStopsViewModel::class.java)
    }


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialize recycler view
        with (busStopsList) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            busStopsAdapter = BusStopsRecyclerViewAdapter {}
            adapter = busStopsAdapter
        }


        busStopsViewModel.busStops.observe(this) {
            busStopsAdapter.busStopList = it!!
            busStopsAdapter.notifyDataSetChanged()
        }

        busStopsViewModel.fetchBusStops(routeId!!)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_stops, container, false)
    }
    companion object {

        fun newInstance(routeId: String, direction: Int): StopsFragment {
            val fragment = StopsFragment()
            val args = Bundle()
            args.putString(Constants.ROUTE_ID, routeId)
            args.putInt(Constants.DIRECTION, direction)
            fragment.arguments = args
            return fragment
        }
    }


}
