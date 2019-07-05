package com.app.smartriumclock.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BleReceive(
    @ColumnInfo(name = "command") val command: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "data") val data: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null

}
