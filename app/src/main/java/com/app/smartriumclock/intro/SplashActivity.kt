package com.app.smartriumclock.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.app.smartriumclock.DustMainActivity
import com.app.smartriumclock.R
import com.app.smartriumclock.search.SearchHardwareActivity
import com.app.smartriumclock.search.SellectPlantActivity

class SplashActivity : AppCompatActivity() {

    var userFirst: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        var prefs = getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        var editor = prefs.edit()

        userFirst = prefs.getBoolean("first_user", false)
        if (!userFirst) {
            //Tutorial 최초 앱 실행시 1회, 이후에는 바로 블루투스 검색화면
            val handler = Handler()
            handler.postDelayed({
                val intent = Intent(this, TutorialActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }, 2000)

            editor.putBoolean("first_user", true)
            editor.commit()
        } else {
            val intent = Intent(this, SearchHardwareActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

