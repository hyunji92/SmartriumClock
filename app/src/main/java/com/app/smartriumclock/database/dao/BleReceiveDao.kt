package com.app.smartriumclock.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.app.smartriumclock.model.BleReceive
import io.reactivex.Flowable
import io.reactivex.Observable

@Dao
interface BleReceiveDao {

    @Query("SELECT * FROM BleReceive")
    fun getAll(): Observable<List<BleReceive>>

    @Query("SELECT * FROM BleReceive where command = '01' order by date desc")
    fun getAllPM10(): Observable<List<BleReceive>>

    @Query("SELECT * FROM BleReceive where command = '02' order by date desc")
    fun getAllPM2_5(): Observable<List<BleReceive>>

    @Query("SELECT * FROM BleReceive where command = '03' order by date desc")
    fun getAllPM1(): Observable<List<BleReceive>>

    @Query("SELECT * FROM BleReceive where command = '04' order by date desc")
    fun getAllTemperature(): Observable<List<BleReceive>>

    @Query("SELECT * FROM BleReceive where command = '05' order by date desc")
    fun getAllHumidity(): Observable<List<BleReceive>>

    @Query("SELECT * FROM BleReceive where command = '06' order by date desc")
    fun getAllIlluminance(): Observable<List<BleReceive>>

    @Query("SELECT * FROM BleReceive")
    fun getAllFlowable(): Flowable<List<BleReceive>>

    @Insert
    fun insert(bleReceive: BleReceive)
}
