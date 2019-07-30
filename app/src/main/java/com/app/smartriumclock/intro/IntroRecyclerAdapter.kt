package com.app.smartriumclock.intro

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.app.smartriumclock.R
import com.app.smartriumclock.search.SearchHardwareActivity
import kotlinx.android.synthetic.main.list_item.view.*


class IntroRecyclerAdapter(var context: Context, var arrayList: ArrayList<String>) :
    RecyclerView.Adapter<IntroRecyclerAdapter.IntroImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): IntroImageViewHolder {
        return IntroImageViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item, parent, false))
    }

    override fun getItemCount(): Int = arrayList.size

    override fun onBindViewHolder(holder: IntroImageViewHolder, position: Int) {
        holder.bindView(position)
    }

    inner class IntroImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(position: Int) {
            var mOutTutorialBtn = itemView.findViewById<ImageView>(R.id.tutorial_to_main)
            var mIntroImage = itemView.findViewById<ImageView>(R.id.item_image)
            when (position) {
                0 -> {
                    mIntroImage.setImageResource(R.drawable.tutorial_0)
                }
                1 -> {
                    mIntroImage.setImageResource(R.drawable.tutorial_2)
                }
                2 -> {
                    mIntroImage.setImageResource(R.drawable.tutorial_1)
                    mOutTutorialBtn.setOnClickListener {

                        val intent = Intent(context, SearchHardwareActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}
