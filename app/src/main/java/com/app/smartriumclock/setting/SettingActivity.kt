package com.app.smartriumclock.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.smartriumclock.DustMainActivity
import com.app.smartriumclock.PlantInfoMainActivity
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        plant_info_layout.setOnClickListener {
            val intent = Intent(this, PlantInfoMainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        dust_info_layout.setOnClickListener {
            val intent = Intent(this, DustMainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        setting_info_layout.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        website_info_layout.setOnClickListener {
            intent = Intent(Intent.ACTION_VIEW)
            var uri = Uri.parse("http://www.it-sago.com")
            intent.data = uri
            startActivity(intent)

        }
    }




}
