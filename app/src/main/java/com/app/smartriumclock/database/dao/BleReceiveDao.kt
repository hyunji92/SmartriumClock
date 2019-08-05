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

    @Query("SELECT * FROM BleReceive where command = :command order by date asc")
    fun getAll(command: String): Observable<List<BleReceive>>

    @Query("SELECT b_r.command, strftime('%Y-%m-%d %H', b_r.date / 1000, 'unixepoch') as time, avg(data) as data, strftime('%s', datetime(strftime('%Y-%m-%d %H:00', b_r.date / 1000, 'unixepoch'))) * 1000  as date FROM BleReceive b_r where command = :command and b_r.date >= :startedAt and b_r.date < :endedAt group by time order by date asc")
    fun getToday(command: String, startedAt: Long, endedAt: Long): Observable<List<BleReceive>>

    @Query("SELECT b_r.command, b_r.date, strftime('%Y-%m-%d', b_r.date / 1000, 'unixepoch') as time, avg(data) as data FROM BleReceive b_r where command = :command group by time order by date asc")
    fun getDaily(command: String): Observable<List<BleReceive>>

    @Query("SELECT b_r.command, b_r.date, strftime('%Y-%m-%W', b_r.date / 1000, 'unixepoch') as time, avg(data) as data FROM BleReceive b_r where command = :command group by time order by date asc")
    fun getWeekly(command: String): Observable<List<BleReceive>>

    @Query("SELECT b_r.command, b_r.date, strftime('%Y-%m', b_r.date / 1000, 'unixepoch') as time, avg(data) as data FROM BleReceive b_r where command = :command group by time order by date asc")
    fun getMonthly(command: String): Observable<List<BleReceive>>

    @Query("SELECT b_r.command, b_r.date, strftime('%Y', b_r.date / 1000, 'unixepoch') as time, avg(data) as data FROM BleReceive b_r where command = :command group by time order by date asc")
    fun getYearly(command: String): Observable<List<BleReceive>>

    @Query("SELECT * FROM BleReceive where command = :command order by date desc LIMIT 1")
    fun getRecent(command: String): Observable<List<BleReceive>>

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
