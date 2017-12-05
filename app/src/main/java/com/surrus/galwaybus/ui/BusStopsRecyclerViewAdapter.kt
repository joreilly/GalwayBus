package com.surrus.galwaybus.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surrus.galwaybus.R
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import kotlinx.android.synthetic.main.busroutes_list_item.view.*


class BusStopsRecyclerViewAdapter : RecyclerView.Adapter<BusStopsRecyclerViewAdapter.ViewHolder>() {

    var busStopList: List<BusStop> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.busroutes_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val busStop = busStopList[position]

        holder.title.text = busStop.longName
        holder.subtitle.text = busStop.irishShortName
    }

    override fun getItemCount(): Int {
        return busStopList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.title
        val subtitle = view.subtitle
    }
}