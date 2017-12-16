package com.surrus.galwaybus.ui

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surrus.galwaybus.R
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Departure
import kotlinx.android.synthetic.main.bus_stops_list_item.view.*
import kotlinx.android.synthetic.main.bus_times_list_item.view.*
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder


class BusStopsRecyclerViewAdapter(val listener: (BusStop) -> Unit) : RecyclerView.Adapter<BusStopsRecyclerViewAdapter.ViewHolder>() {

    var busStopList: List<BusStop> = mutableListOf()
    val minsFormatter: PeriodFormatter

    init {
        minsFormatter = PeriodFormatterBuilder()
                .appendHours()
                .appendSuffix(" hr ", " hrs ")
                .printZeroAlways()
                .appendMinutes()
                .appendSuffix(" min", " mins")
                .toFormatter()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.bus_stops_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val busStop = busStopList[position]

        holder.title.text = busStop.longName

        if (!TextUtils.isEmpty(busStop.irishShortName)) {
            holder.subtitle.visibility = View.VISIBLE
            holder.subtitle.text = "${busStop.stopRef} (${busStop.irishShortName})"
        } else {
            holder.subtitle.visibility = View.GONE
        }


        if (busStop.times != null && busStop.times.size > 0) {
            holder.timesLayout.visibility = View.VISIBLE

            with (holder.busTimesList) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                val busTimesAdapter = BusTimesAdapter()
                busTimesAdapter.busTimes = busStop.times
                adapter = busTimesAdapter
            }
        } else {
            holder.timesLayout.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { listener(busStop) }
    }

    override fun getItemCount(): Int {
        return busStopList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.title
        val subtitle = view.subtitle
        val busTimesList = view.busTimesList
        val timesLayout = view.timesLayout
    }



    inner class BusTimesAdapter : RecyclerView.Adapter<BusTimesAdapter.ViewHolder>() {

        var busTimes: List<Departure> = arrayListOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return ViewHolder(layoutInflater.inflate(R.layout.bus_times_list_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val busTime = busTimes[position]

            holder.timetableId.text = busTime.timetableId
            holder.destination.text = busTime.displayName

            val now = DateTime()
            val dep = DateTime(busTime.departTimestamp)
            val timeTillDeparture = Period(now, dep)
            val mins = timeTillDeparture.minutes
            if (mins >= 0) {
                holder.duration.text = minsFormatter.print(timeTillDeparture)
            }
        }

        override fun getItemCount(): Int {
            return busTimes.size
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val timetableId = view.timetableId
            val destination = view.destination
            val duration = view.duration
        }
    }
}