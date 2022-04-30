package com.idivisiontech.transporttracker.adapter.maintenance

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.idivisiontech.transporttracker.R
import com.idivisiontech.transporttracker.ServerOperator.Data.maintenance.SensorItem

class SensorItemAdapter internal constructor(private val context: Context): BaseAdapter() {
    internal var sensorItems: List<SensorItem> = listOf()


    override fun getItem(position: Int): SensorItem = sensorItems[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = sensorItems.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var itemView = convertView
        if(itemView == null){
            itemView = LayoutInflater.from(context).inflate(R.layout.sensor_item, parent, false)
        }

        val viewHolder = ViewHolder(itemView as View)
        viewHolder.bind(position, getItem(position))
        return itemView
    }

    private inner class ViewHolder internal constructor(view: View) {
        val tvRowIndex = view.findViewById<TextView>(R.id.rowIndex)
        val tvRowName = view.findViewById<TextView>(R.id.rowName)
//        val tvRowBatas = view.findViewById<TextView>(R.id.rowBatas)
        val tvRowStatus = view.findViewById<TextView>(R.id.rowNilai)

        internal fun bind(index: Int, item: SensorItem){
            tvRowIndex.text = (index + 1).toString()
            tvRowName.text = item.sensor
//            tvRowBatas.text = "${item.batas} KM"
            tvRowStatus.text = item.nilai.format(2)
        }
    }

    fun Double.format(digits: Int) = "%.${digits}f".format(this)

}