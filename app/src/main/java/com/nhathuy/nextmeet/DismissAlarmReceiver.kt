package com.nhathuy.nextmeet

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

//@AndroidEntryPoint
class DismissAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "DismissAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", -1)
        val relatedId = intent.getIntExtra("related_id", -1)
        val notificationTypeString = intent.getStringExtra("notification_type") ?: ""

        Log.d(TAG, "Dismissing alarm - ID: $notificationId, RelatedID: $relatedId, Type: $notificationTypeString")

        if (notificationId == -1) {
            Log.e(TAG, "Invalid notification ID")
            return
        }

        try {
            // Cancel the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // Cancel any pending alarms for new notification system
            cancelPendingAlarm(context, notificationId)

            // Update notification status in database
            updateNotificationStatusToDismissed(notificationId)

            // Handle type-specific dismiss actions
            handleTypeSpecificDismiss(notificationTypeString, relatedId)

            Log.d(TAG, "Successfully dismissed notification: $notificationId")

        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing alarm", e)
        }
    }

    private fun cancelPendingAlarm(context: Context, notificationId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel alarm for new notification system
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Cancel any potential snooze alarms
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000, // Snooze offset
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(snoozePendingIntent)
    }

    private fun updateNotificationStatusToDismissed(notificationId: Int) {
        receiverScope.launch {
            try {
//                notificationRepository.markAsDismissed(notificationId)
                Log.d(TAG, "Notification status updated to dismissed for ID: $notificationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification status to dismissed", e)
            }
        }
    }

    private fun handleTypeSpecificDismiss(notificationTypeString: String, relatedId: Int) {
        if (notificationTypeString.isEmpty()) return

        try {
            val notificationType = NotificationType.valueOf(notificationTypeString)

            when (notificationType) {
                NotificationType.APPOINTMENT_REMINDER -> {
                    // Could mark appointment as completed or acknowledged
                    Log.d(TAG, "Dismissed appointment reminder for ID: $relatedId")
                }
                NotificationType.NOTE_REMINDER -> {
                    // Could update note status
                    Log.d(TAG, "Dismissed note reminder for ID: $relatedId")
                }
                NotificationType.LOCATION_REMINDER -> {
                    // Could log location reminder dismissal
                    Log.d(TAG, "Dismissed location reminder for ID: $relatedId")
                }
                NotificationType.TRAVEL_TIME -> {
                    // Could update travel reminder status
                    Log.d(TAG, "Dismissed travel time reminder for ID: $relatedId")
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid notification type: $notificationTypeString")
        }
    }
}