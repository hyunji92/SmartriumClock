package com.app.smartriumclock.search

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.hardware_list_item.view.*

class SellectPlantAdapter(
        private val sellctPlantActivity: SellectPlantActivity) :
        RecyclerView.Adapter<SellectPlantAdapter.ViewHolder>() {

    var items: Array<String> = arrayOf("선인장 cactus", "다육식물 Succulent", "틸란드시아 Tillandsia", "스칸디아모스 Skandia Moss")

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(p0.context).inflate(R.layout.hardware_list_item, p0, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        p0.bind(items[p1])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


        init {
            var check = false
            itemView.setOnClickListener {
                if (!check){
                    itemView.check_point.visibility = View.VISIBLE
                    check = true
                } else {
                    itemView.check_point.visibility = View.GONE
                    check = false
                }

            }
        }

        fun bind(s: String) {
            itemView.hardwareListItemTitle.text = s
        }
    }

}
