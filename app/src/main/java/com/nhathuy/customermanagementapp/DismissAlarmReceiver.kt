package com.nhathuy.customermanagementapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DismissAlarmReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val notificationId= intent.getIntExtra("notification_id",-1)
        if(notificationId!=-1){
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            Log.d("DismissAlarmReceiver", "Notification $notificationId dismissed by swipe")
        }
    }

}