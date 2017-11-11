package com.surrus.galwaybus.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surrus.galwaybus.R
import com.surrus.galwaybus.model.BusRoute
import kotlinx.android.synthetic.main.bus_times_list_item.view.*


class BusTimesRecyclerViewAdapter : RecyclerView.Adapter<BusTimesRecyclerViewAdapter.ViewHolder>() {

    var busRouteList: List<BusRoute> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.bus_times_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val busRoute = busRouteList[position]

        holder.title.text = busRoute.longName + " (" + busRoute.timetableId + ")";
    }

    override fun getItemCount(): Int {
        return busRouteList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.title
    }
}