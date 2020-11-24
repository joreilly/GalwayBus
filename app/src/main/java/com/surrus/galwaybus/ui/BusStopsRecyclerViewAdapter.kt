package com.surrus.galwaybus.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.surrus.galwaybus.R
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Departure
//import com.surrus.galwaybus.model.BusStop
//import com.surrus.galwaybus.model.Departure
import com.surrus.galwaybus.util.ext.inflate
import kotlinx.android.synthetic.main.bus_stops_list_item.view.*
import kotlinx.android.synthetic.main.bus_times_list_item.view.*
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder


class BusStopsRecyclerViewAdapter(val listener: (BusStop) -> Unit) : RecyclerView.Adapter<BusStopsRecyclerViewAdapter.ViewHolder>() {
    var busStopList: List<BusStop> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.inflate(R.layout.bus_stops_list_item))

    override fun getItemCount() = busStopList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(busStopList[position], listener)


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(busStop: BusStop, listener: (BusStop) -> Unit) = with(itemView) {
            title.text = busStop.longName

            subtitle.visibility = View.VISIBLE
            subtitle.text = "${busStop.stopRef}"

            if (busStop.times.isNotEmpty()) {
                timesLayout.visibility = View.VISIBLE

                with (busTimesList) {
                    setHasFixedSize(true)
                    layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                    val busTimesAdapter = BusTimesAdapter()
                    busTimesAdapter.busTimes = busStop.times
                    adapter = busTimesAdapter
                }
            } else {
                timesLayout.visibility = View.GONE
            }

            setOnClickListener { listener(busStop) }
        }
    }
}


class BusTimesAdapter : RecyclerView.Adapter<BusTimesAdapter.ViewHolder>() {
    var busTimes: List<Departure> = arrayListOf()
    val minsFormatter: PeriodFormatter = PeriodFormatterBuilder()
            .appendHours()
            .appendSuffix(" hr ", " hrs ")
            .printZeroAlways()
            .appendMinutes()
            .appendSuffix(" min", " mins")
            .toFormatter()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.inflate(R.layout.bus_times_list_item))

    override fun getItemCount() = busTimes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(busTimes[position])


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(busTime: Departure) = with(itemView) {
            timetableId.text = busTime.timetableId
            destination.text = busTime.displayName

            val now = DateTime()
            val dep = DateTime(busTime.departTimestamp)
            val timeTillDeparture = Period(now, dep)
            val mins = timeTillDeparture.minutes
            if (mins >= 0) {
                if (mins == 0) {
                    duration.text = context.getString(R.string.bus_time_due)
                } else {
                    duration.text = minsFormatter.print(timeTillDeparture)
                }
            }
        }
    }
}
