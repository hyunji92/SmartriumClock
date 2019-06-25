package com.app.smartriumclock.search

import android.content.Intent
import android.content.Intent.*
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.app.smartriumclock.DustMainActivity
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.activity_search_hardware.*
import kotlinx.android.synthetic.main.activity_sellect_plant.*


class SellectPlantActivity : AppCompatActivity() {
    // Hardware RecyclerView Adapter
    private val sellctPlantAdapter by lazy { SellectPlantAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sellect_plant)

        // 리사이클러뷰 설정
        plant_list.apply {
            layoutManager = LinearLayoutManager(this@SellectPlantActivity)
            adapter = sellctPlantAdapter
        }

        into_main.setOnClickListener {
            val intent = Intent(this, DustMainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

    }
}
