package com.surrus.galwaybus.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surrus.galwaybus.R
import com.surrus.galwaybus.model.BusStop
import kotlinx.android.synthetic.main.bus_stops_list_item.view.*
import org.joda.time.DateTime
import org.joda.time.Period


class BusStopsRecyclerViewAdapter : RecyclerView.Adapter<BusStopsRecyclerViewAdapter.ViewHolder>() {

    var busStopList: List<BusStop> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.bus_stops_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val busStop = busStopList[position]

        holder.title.text = busStop.longName
        holder.subtitle.text = busStop.irishShortName


        val now = DateTime()

        var timeInfo = "";
        if (busStop.times != null) {
            for (time in busStop.times) {

                val dep = DateTime(time.departTimestamp)
                val timeTillDeparture = Period(now, dep)
                val mins = timeTillDeparture.minutes

                timeInfo += "\n" + time.timetableId + "\t" + time.displayName + "`\t" + mins + " mins";
            }
        }
        holder.times.text = timeInfo

    }

    override fun getItemCount(): Int {
        return busStopList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.title
        val subtitle = view.subtitle
        val times = view.times
    }
}