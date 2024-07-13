package com.nhathuy.customermanagementapp.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nhathuy.customermanagementapp.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val customerId = intent.getIntExtra("customer_id", -1)
        val date = intent.getStringExtra("date")
        val time = intent.getStringExtra("time")
        val address = intent.getStringExtra("address")
        val notes = intent.getStringExtra("notes")

        Log.d("AlarmReceiver", "Received alarm for customer $customerId at $time on $date")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("appointment_channel", "Appointments", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "appointment_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Appointment Reminder")
            .setContentText("You have an appointment at $time on $date")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You have an appointment at $time on $date \nAddress: $address\nNotes: $notes"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(customerId, notification)
        Log.d("AlarmReceiver", "Notification sent for customer $customerId")
    }
}