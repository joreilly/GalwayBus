package com.surrus.galwaybus.ui


import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surrus.galwaybus.Constants

import com.surrus.galwaybus.R
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModel
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModelFactory
import com.surrus.galwaybus.util.ext.observe
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_routes.*
import javax.inject.Inject


class RoutesFragment : Fragment() {

    @Inject
    lateinit var busRoutesViewModelFactory: BusRoutesViewModelFactory

    private lateinit var busRoutesViewModel : BusRoutesViewModel
    private lateinit var busRoutesAdapter: BusRoutesRecyclerViewAdapter


    companion object {
        fun newInstance(): RoutesFragment {
            return RoutesFragment()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        busRoutesViewModel = ViewModelProviders.of(activity!!, busRoutesViewModelFactory).get(BusRoutesViewModel::class.java)
    }


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (busRoutesList) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)

            busRoutesAdapter = BusRoutesRecyclerViewAdapter {
                val intent = Intent(context, BusStopListActivity::class.java)
                intent.putExtra(Constants.ROUTE_ID, it.timetableId)
                intent.putExtra(Constants.ROUTE_NAME, it.longName)
                intent.putExtra(Constants.SCHEDULE_PDF, it.schedulePdf)
                context.startActivity(intent)
            }
            adapter = busRoutesAdapter
        }


        busRoutesViewModel.getBusRoutes().observe(this) {
            busRoutesAdapter.busRouteList = ArrayList(it)
            busRoutesAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_routes, container, false)
    }


}
