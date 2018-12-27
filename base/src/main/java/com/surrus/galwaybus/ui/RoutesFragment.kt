package com.surrus.galwaybus.ui


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.surrus.galwaybus.Constants

import com.surrus.galwaybus.base.R
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModel
import com.surrus.galwaybus.util.ext.observe
import kotlinx.android.synthetic.main.fragment_routes.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class RoutesFragment : Fragment() {
    private val firebaseAnaltyics by inject<FirebaseAnalytics>()

    private val busRoutesViewModel: BusRoutesViewModel by viewModel()

    private lateinit var busRoutesAdapter: BusRoutesRecyclerViewAdapter


    companion object {
        fun newInstance(): RoutesFragment {
            return RoutesFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (busRoutesList) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)

            busRoutesAdapter = BusRoutesRecyclerViewAdapter {

                val bundle = Bundle()
                bundle.putString("route", it.timetableId)
                firebaseAnaltyics.logEvent("route_selected", bundle)

                val intent = Intent(context, BusStopListActivity::class.java)
                intent.putExtra(Constants.ROUTE_ID, it.timetableId)
                intent.putExtra(Constants.ROUTE_NAME, it.longName)
                intent.putExtra(Constants.SCHEDULE_PDF, it.schedulePdf)
                context.startActivity(intent)
            }
            adapter = busRoutesAdapter
        }


        busRoutesViewModel.getBusRoutes().observe(this) {
            busRoutesAdapter.busRouteList = it!!
            busRoutesAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_routes, container, false)
    }

}
