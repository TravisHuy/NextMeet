package com.nhathuy.nextmeet

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DismissAlarmReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val notificationId= intent.getIntExtra("notification_id",-1)
        if(notificationId!=-1){
            // cancel the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            //cancel any pending alarms
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                Intent(context,AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            Log.d("DismissAlarmReceiver", "Notification $notificationId dismissed by swipe")
        }
    }

}