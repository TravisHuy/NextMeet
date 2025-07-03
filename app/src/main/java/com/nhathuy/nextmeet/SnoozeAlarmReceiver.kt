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
import com.nhathuy.nextmeet.repository.NotificationRepository
import com.nhathuy.nextmeet.utils.NotificationManagerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

//@AndroidEntryPoint
class SnoozeAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "SnoozeAlarmReceiver"
        private const val SNOOZE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val SNOOZE_OFFSET = 10000 // Offset for snooze notification IDs
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Extract notification data
        val notificationId = intent.getIntExtra("notification_id", -1)
        val relatedId = intent.getIntExtra("related_id", -1)
        val notificationTypeString = intent.getStringExtra("notification_type") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val message = intent.getStringExtra("message") ?: ""
        val location = intent.getStringExtra("location")

        // Legacy compatibility
        val customerId = intent.getIntExtra("customer_id", notificationId)
        val date = intent.getStringExtra("date") ?: getCurrentDate()
        val time = intent.getStringExtra("time") ?: getCurrentTime()
        val address = intent.getStringExtra("address") ?: location ?: ""
        val notes = intent.getStringExtra("notes") ?: message

        Log.d(TAG, "Snoozing alarm - ID: $notificationId, Type: $notificationTypeString")

        if (notificationId == -1) {
            Log.e(TAG, "Invalid notification ID")
            return
        }

        try {
            // Cancel current notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // Schedule new alarm for snooze
            scheduleSnoozeAlarm(
                context = context,
                notificationId = notificationId,
                title = title,
                message = message,
                location = location,
                relatedId = relatedId,
                notificationTypeString = notificationTypeString,
                customerId = customerId,
                date = date,
                time = time,
                address = address,
                notes = notes
            )

            // Show snooze confirmation notification
            showSnoozeNotification(context, notificationId, notificationTypeString)

            // Update notification status in database
            updateNotificationStatusToSnoozed(notificationId)

            Log.d(TAG, "Successfully snoozed notification: $notificationId")

        } catch (e: Exception) {
            Log.e(TAG, "Error snoozing alarm", e)
        }
    }

    private fun scheduleSnoozeAlarm(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationTypeString: String,
        customerId: Int,
        date: String,
        time: String,
        address: String,
        notes: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val newAlarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            // New notification system data
            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("location", location)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationTypeString)

            // Legacy compatibility
            putExtra("customer_id", customerId)
            putExtra("date", date)
            putExtra("time", time)
            putExtra("address", address)
            putExtra("notes", notes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + SNOOZE_OFFSET, // Use offset to avoid conflicts
            newAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + SNOOZE_DURATION_MS

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            Log.d(TAG, "Snooze alarm scheduled for ${Date(snoozeTime)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze alarm", e)
            // Fallback to regular setExact
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
        }
    }

    private fun showSnoozeNotification(context: Context, notificationId: Int, notificationTypeString: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Determine appropriate channel
        val channelId = when {
            notificationTypeString == NotificationType.APPOINTMENT_REMINDER.name ->
                NotificationManagerService.APPOINTMENT_CHANNEL_ID
            notificationTypeString == NotificationType.NOTE_REMINDER.name ->
                NotificationManagerService.NOTE_CHANNEL_ID
            else -> NotificationManagerService.APPOINTMENT_CHANNEL_ID
        }

        // Create dismiss intent for snooze notification
        val dismissIntent = Intent(context, DismissAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId + SNOOZE_OFFSET)
            putExtra("related_id", -1)
            putExtra("notification_type", notificationTypeString)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + SNOOZE_OFFSET,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build snooze notification
        val snoozeNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.zzz)
            .setContentTitle("Đã báo lại")
            .setContentText("Sẽ nhắc lại sau 5 phút")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(R.drawable.alarm_off, "Hủy", dismissPendingIntent)
            .build()

        // Use a different ID for snooze notification to avoid conflicts
        notificationManager.notify(notificationId + SNOOZE_OFFSET, snoozeNotification)
    }

    private fun updateNotificationStatusToSnoozed(notificationId: Int) {
        receiverScope.launch {
            try {
//                notificationRepository.markAsSnoozed(notificationId)
                Log.d(TAG, "Notification status updated to snoozed for ID: $notificationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification status to snoozed", e)
            }
        }
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }
}