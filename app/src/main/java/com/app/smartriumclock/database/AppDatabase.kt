package com.app.smartriumclock.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.smartriumclock.database.dao.BleReceiveDao
import com.app.smartriumclock.model.BleReceive

@Database(entities = arrayOf(BleReceive::class), version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bleReceiveDao(): BleReceiveDao
}
