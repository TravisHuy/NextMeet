package com.nhathuy.customermanagementapp

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class SnoozeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val customerId= intent.getIntExtra("customer_id",-1)
        val date = intent.getStringExtra("date")
        val time = intent.getStringExtra("time")
        val address = intent.getStringExtra("address")
        val notes = intent.getStringExtra("notes")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(customerId)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newAlarmIntent= Intent(context,AlarmReceiver::class.java).apply {
            putExtra("customer_id",customerId)
            putExtra("date",date)
            putExtra("time",time)
            putExtra("address",address)
            putExtra("notes",notes)
        }

        val pendingIntent =PendingIntent.getBroadcast(
            context,
            customerId,
            newAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val snoozeTime= System.currentTimeMillis() +5*60*1000 //5 minutes from now
        alarmManager.setExact(AlarmManager.RTC_WAKEUP,snoozeTime,pendingIntent)

        val dismissIntent= Intent(context,DismissAlarmReceiver::class.java).apply {
            putExtra("notification_id",customerId)
        }

        val dismissPendingIntent= PendingIntent.getBroadcast(
            context,
            customerId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // show a notification that the alarm is snoozed
        val snoozeNotification =  NotificationCompat.Builder(context,"appointment_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText("Alarm will ring again in 5 minutes")
            .setContentTitle("Alarm Snoozed")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_dismiss, "Dismiss",dismissPendingIntent)
            .build()

        notificationManager.notify(customerId,snoozeNotification)

        Log.d("SnoozeAlarmReceiver", "Alarm snoozed for customer $customerId. Will ring again in 5 minutes.")

    }

}