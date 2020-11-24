package com.surrus.galwaybus.ui


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.surrus.galwaybus.Constants

import com.surrus.galwaybus.R
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModel
import com.surrus.galwaybus.util.ext.observe
import kotlinx.android.synthetic.main.fragment_stops.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import com.surrus.galwaybus.common.model.Result
import kotlin.time.ExperimentalTime

@ExperimentalTime
class StopsFragment : Fragment() {

    private var routeId: String? = null
    private var direction: Int? = null

    private val busStopsViewModel: BusStopsViewModel by sharedViewModel()

    private lateinit var busStopsAdapter: BusStopsRecyclerViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            routeId = it.getString(Constants.ROUTE_ID)
            direction = it.getInt(Constants.DIRECTION)
        }
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
            if (it is Result.Success) {
                busStopsAdapter.busStopList = it.data
                busStopsAdapter.notifyDataSetChanged()
            }
        }

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
