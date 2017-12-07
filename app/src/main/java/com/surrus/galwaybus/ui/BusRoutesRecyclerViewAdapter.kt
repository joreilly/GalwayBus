package com.surrus.galwaybus.ui

import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surrus.galwaybus.Constants
import com.surrus.galwaybus.R
import com.surrus.galwaybus.model.BusRoute
import kotlinx.android.synthetic.main.busroutes_list_item.view.*
import org.jetbrains.anko.intentFor


class BusRoutesRecyclerViewAdapter : RecyclerView.Adapter<BusRoutesRecyclerViewAdapter.ViewHolder>() {

    var busRouteList: List<BusRoute> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.busroutes_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val busRoute = busRouteList[position]

        holder.title.text = busRoute.longName
        holder.subtitle.text = busRoute.timetableId

        val context = holder.itemView.context
        holder.itemView.setOnClickListener {

            val intent = Intent(context, BusStopListActivity::class.java)
            intent.putExtra(Constants.ROUTE_ID, busRoute.timetableId)
            intent.putExtra(Constants.ROUTE_NAME, busRoute.longName)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return busRouteList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.title
        val subtitle = view.subtitle
    }
}