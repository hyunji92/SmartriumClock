package com.app.smartriumclock.intro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        var arrayList = object : ArrayList<String>() {
            init {
                add("drawable/tutotial_0.png")
                add("drawable/tutotial_1.png")
                add("drawable/tutotial_2.png")
            }
        }
        val introAdapter = IntroRecyclerAdapter(this, arrayList)
        val layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        intro_recycler_view.apply {
            setLayoutManager(layoutManager)
            adapter = introAdapter
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(LinePagerIndicatorDecoration())
        }
        val pagerSnapHelper = PagerSnapHelper()
        pagerSnapHelper.attachToRecyclerView(intro_recycler_view)

    }
}
