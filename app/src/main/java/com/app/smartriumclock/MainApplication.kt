package com.app.smartriumclock

import android.app.Application
import android.content.Context

class MainApplication : Application() {

    init {
        instance = this
    }

    companion object {
        lateinit var instance: MainApplication

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

    }
}
