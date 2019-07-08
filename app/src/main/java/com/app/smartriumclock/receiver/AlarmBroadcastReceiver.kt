package com.app.smartriumclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat
import android.util.Log
import com.app.smartriumclock.MainApplication


class AlarmBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d("AlarmBroadcastReceiver", "onReceive")

        val builder = NotificationCompat.Builder(MainApplication.applicationContext(), "smart_rium")
            //.setSmallIcon(R.drawable.ic_love_p_64px) //알람 아이콘
            .setContentTitle("Title")  //알람 제목
            .setContentText("Text") //알람 내용
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) //알람 중요도

        val notificationManager = NotificationManagerCompat.from(MainApplication.applicationContext())
        //notificationManager.notify(NOTICATION_ID, builder.build()) //알람 생성
    }
}
