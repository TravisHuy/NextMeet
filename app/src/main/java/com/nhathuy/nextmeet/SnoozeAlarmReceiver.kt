package com.nhathuy.nextmeet

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.utils.Constant

class SnoozeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", -1)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val message = intent.getStringExtra("message") ?: ""
        val location = intent.getStringExtra("location")
        val relatedId = intent.getIntExtra("related_id", -1)
        val notificationTypeStr = intent.getStringExtra("notification_type") ?: NotificationType.APPOINTMENT_REMINDER.name

        if (notificationId == -1) {
            Log.e("SnoozeAlarmReceiver", "Invalid notification ID")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newAlarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("location", location)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationTypeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            newAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes from now
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)

        val dismissIntent = Intent(context, DismissAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine channel ID based on notification type
        val notificationType = try {
            NotificationType.valueOf(notificationTypeStr)
        } catch (e: IllegalArgumentException) {
            NotificationType.APPOINTMENT_REMINDER
        }

        val channelId = when (notificationType) {
            NotificationType.NOTE_REMINDER -> Constant.NOTE_CHANNEL_ID
            else -> Constant.APPOINTMENT_CHANNEL_ID
        }

        // Show a notification that the alarm is snoozed
        val snoozeNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText("Reminder will ring again in 5 minutes")
            .setContentTitle("Reminder Snoozed")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)
            .build()

        notificationManager.notify(notificationId, snoozeNotification)

        Log.d("SnoozeAlarmReceiver", "Alarm snoozed for notification $notificationId. Will ring again in 5 minutes.")
    }

}