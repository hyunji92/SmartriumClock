package com.app.smartriumclock.setting

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.app.smartriumclock.R
import com.app.smartriumclock.search.SearchHardwareActivity
import com.app.smartriumclock.search.SellectPlantActivity
import kotlinx.android.synthetic.main.activity_mypage.*

class MyPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        connect_bluetooth.setOnClickListener {
            val intent = Intent(this, SearchHardwareActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        register_plant.setOnClickListener {
            val intent = Intent(this, SellectPlantActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

}
