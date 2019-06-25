package com.app.smartriumclock.setting

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.activity_mypage.*

class MyPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        connect_bluetooth.setOnClickListener {

        }
    }

}
