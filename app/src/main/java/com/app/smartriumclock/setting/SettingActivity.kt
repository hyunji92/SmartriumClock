package com.app.smartriumclock.setting

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.app.smartriumclock.PlantInfoMainActivity
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        plant_info_layout.setOnClickListener {
            val intent = Intent(this, PlantInfoMainActivity::class.java)
            startActivity(intent)
        }
    }

}
