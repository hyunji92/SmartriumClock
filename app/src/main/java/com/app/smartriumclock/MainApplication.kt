package com.app.smartriumclock

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.app.smartriumclock.database.AppDatabase

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

    }
}
