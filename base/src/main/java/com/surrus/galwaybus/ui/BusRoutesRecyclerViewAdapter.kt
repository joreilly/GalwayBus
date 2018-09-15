package com.surrus.galwaybus.ui

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.surrus.galwaybus.base.R
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import com.surrus.galwaybus.util.ext.inflate
import kotlinx.android.synthetic.main.busroutes_list_item.view.*


class BusRoutesRecyclerViewAdapter(val listener: (BusRouteSchedule) -> Unit) : RecyclerView.Adapter<BusRoutesRecyclerViewAdapter.ViewHolder>() {
    var busRouteList: List<BusRouteSchedule> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.inflate(R.layout.busroutes_list_item))

    override fun getItemCount() = busRouteList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(busRouteList[position], listener)


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(busRoute: BusRouteSchedule, listener: (BusRouteSchedule) -> Unit) = with(itemView) {
            title.text = busRoute.longName
            subtitle.text = busRoute.timetableId
            setOnClickListener { listener(busRoute) }
        }
    }
}