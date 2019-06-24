package com.app.smartriumclock

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.app.smartriumclock.intro.TutorialActivity
import com.app.smartriumclock.setting.SettingActivity
import kotlinx.android.synthetic.main.activity_dust_main.*

class DustMainActivity : AppCompatActivity() {

    private lateinit var textMessage: TextView
    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_all -> {
                textMessage.setText(R.string.title_all)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dust -> {
                textMessage.setText(R.string.title_dust)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_ultra_dust -> {
                textMessage.setText(R.string.title_ultra)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_super_ultra_dust -> {
                textMessage.setText(R.string.title_super_ultra_dust)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_setting -> {
                textMessage.setText(R.string.title_setting)
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dust_main)
        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)


    }
}
