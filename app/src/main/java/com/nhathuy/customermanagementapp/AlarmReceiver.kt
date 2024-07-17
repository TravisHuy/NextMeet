package com.nhathuy.customermanagementapp

import android.app.AlarmManager
import android.app.Application
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DismissEvent
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.model.AlarmHistory
import com.nhathuy.customermanagementapp.viewmodel.AlarmHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val customerId = intent.getIntExtra("customer_id", -1)
        val date = intent.getStringExtra("date")
        val time = intent.getStringExtra("time")
        val address = intent.getStringExtra("address")
        val notes = intent.getStringExtra("notes")

        val title = "Appointment Reminder"
        val content = "You have an appointment at $time on $date"
        val bigText = "You have an appointment at $time on $date\nAddress: $address\nNotes: $notes"

        Log.d("AlarmReceiver", "Received alarm for customer $customerId at $time on $date")


        //wake up the device
        val powerManager= context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, "TravisHuy:AlarmWakeLock")
        wakeLock.acquire(10*60*1000L)


        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager



        // get the default alram sound
        val alarmSound : Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        //create a notification channel for android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("appointment_channel", "Appointments", NotificationManager.IMPORTANCE_HIGH).apply {
                description="Appointment reminders"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0,1000,500,1000)
                setSound(alarmSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            }
            notificationManager.createNotificationChannel(channel)
        }

        // create an intent for opening the AlarmScreenActivity
        val alarmScreenIntent = Intent(context,AlarmScreenActivity::class.java).apply {
            flags=  Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

            putExtra("customer_id", customerId)
            putExtra("date", date)
            putExtra("time", time)
            putExtra("address", address)
            putExtra("notes", notes)
        }

        val alarmScreenPendingIntent = PendingIntent.getActivity(
            context,
            customerId,
            alarmScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Log the alarm history
        val alarmHistoryIntent = Intent(context, AlarmHistoryActivity::class.java).apply {
            putExtra("customer_id", customerId)
            putExtra("date", date)
            putExtra("time", time)
            putExtra("notes", notes)
        }



        //create an intent for the swipe action
        val dismissIntent= Intent(context,DismissAlarmReceiver::class.java).apply {
            putExtra("notification_id",customerId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(context,
            customerId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, "appointment_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0,1000,500,1000))
            .setSound(alarmSound)
            .setFullScreenIntent(alarmScreenPendingIntent,true)
            .build()


        notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT

        notificationManager.notify(customerId, notification)
        Log.d("AlarmReceiver", "Notification sent for customer $customerId")


        context.startActivity(alarmHistoryIntent)
        context.startActivity(alarmScreenIntent)
    }

}