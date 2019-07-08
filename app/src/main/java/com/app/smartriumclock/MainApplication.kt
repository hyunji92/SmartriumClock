package com.app.smartriumclock

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.app.smartriumclock.database.AppDatabase
import android.app.NotificationManager
import android.app.NotificationChannel
import android.R
import android.os.Build



class MainApplication : Application() {

    init {
        instance = this
    }

    companion object {
        lateinit var instance: MainApplication

        val database by lazy {
            Room.databaseBuilder(applicationContext(), AppDatabase::class.java, "smartrium-clock").build()
        }

        fun applicationContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        // initialize for any

        // Use ApplicationContext.
        // example: SharedPreferences etc...
        val context: Context = MainApplication.applicationContext()

        // Initialize Application
        BleManager.instance.initialize(this)

        /**
         * 노티피케이션 채널 생성하기 안드로이드 버전 오레오 이상부터 필요
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "smart_rium" // 채널 아이디
            val channelName = "channel"//채널 이름
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


}
