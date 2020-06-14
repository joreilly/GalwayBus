package com.surrus.galwaybus.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics

import com.surrus.galwaybus.R
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModel
import com.surrus.galwaybus.util.ext.observe
import kotlinx.android.synthetic.main.fragment_route_list.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class RouteListFragment : Fragment() {
    private val firebaseAnaltyics by inject<FirebaseAnalytics>()
    private val busRoutesViewModel: BusRoutesViewModel by viewModel()
    private lateinit var busRoutesAdapter: BusRoutesRecyclerViewAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (busRoutesList) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)

            busRoutesAdapter = BusRoutesRecyclerViewAdapter {
                val action = RouteListFragmentDirections.actionRouteListFragmentToRouteFragment(it.timetableId, it.longName)
                findNavController().navigate(action)
            }
            adapter = busRoutesAdapter
        }


        busRoutesViewModel.busRoutes.observe(this) {
            busRoutesAdapter.busRouteList = it!!
            busRoutesAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_route_list, container, false)
    }

}
