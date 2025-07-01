package com.nhathuy.nextmeet

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nhathuy.nextmeet.model.NotificationAction
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.repository.NotificationRepository
import com.nhathuy.nextmeet.ui.SolutionActivity
import com.nhathuy.nextmeet.utils.NotificationManagerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val WAKE_LOCK_TIMEOUT = 10 * 60 * 1000L // 10 minutes
        private const val WAKE_LOCK_TAG = "NextMeet:AlarmWakeLock"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver triggered")

        // Acquire wake lock to ensure notification is shown
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            WAKE_LOCK_TAG
        )

        try {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT)

            // Extract notification data from intent
            val notificationId = intent.getIntExtra("notification_id", -1)
            val title = intent.getStringExtra("title") ?: ""
            val message = intent.getStringExtra("message") ?: ""
            val location = intent.getStringExtra("location")
            val relatedId = intent.getIntExtra("related_id", -1)
            val notificationTypeString = intent.getStringExtra("notification_type") ?: ""

            Log.d(TAG, "Processing notification: ID=$notificationId, Type=$notificationTypeString")

            if (notificationId == -1 || notificationTypeString.isEmpty()) {
                Log.e(TAG, "Invalid notification data received")
                return
            }

            val notificationType = try {
                NotificationType.valueOf(notificationTypeString)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid notification type: $notificationTypeString")
                return
            }

            // Show notification
            showNotification(
                context = context,
                notificationId = notificationId,
                title = title,
                message = message,
                location = location,
                relatedId = relatedId,
                notificationType = notificationType
            )

            // Launch AlarmScreenActivity for full-screen experience
            launchAlarmScreen(
                context = context,
                notificationId = notificationId,
                title = title,
                message = message,
                location = location,
                relatedId = relatedId,
                notificationType = notificationType
            )

            // Update notification status in database
            updateNotificationStatus(notificationId)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing alarm", e)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Determine channel ID based on notification type
        val channelId = when (notificationType) {
            NotificationType.APPOINTMENT_REMINDER -> NotificationManagerService.APPOINTMENT_CHANNEL_ID
            NotificationType.NOTE_REMINDER -> NotificationManagerService.NOTE_CHANNEL_ID
            else -> NotificationManagerService.APPOINTMENT_CHANNEL_ID
        }

        // Create AlarmScreen intent
        val alarmScreenIntent = createAlarmScreenIntent(
            context, notificationId, title, message, location, relatedId, notificationType
        )
        val alarmScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            alarmScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create dismiss intent
        val dismissIntent = Intent(context, DismissAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType.name)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get alarm sound
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Build notification content
        val notificationContent = formatNotificationContent(title, message, location, notificationType)

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(getShortContent(message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(alarmSound)
            .setContentIntent(alarmScreenPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setFullScreenIntent(alarmScreenPendingIntent, true)

        // Add action buttons
        addNotificationActions(context, notificationBuilder, notificationType, relatedId, location, notificationId, title, message)

        val notification = notificationBuilder.build()
        notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT

        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notification shown for ID: $notificationId")
    }

    private fun launchAlarmScreen(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ) {
        val alarmScreenIntent = createAlarmScreenIntent(
            context, notificationId, title, message, location, relatedId, notificationType
        )

        try {
            context.startActivity(alarmScreenIntent)
            Log.d(TAG, "AlarmScreenActivity launched for notification: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch AlarmScreenActivity", e)
        }
    }

    private fun createAlarmScreenIntent(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ): Intent {
        return Intent(context, AlarmScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            // New notification system data
            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("location", location)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType.name)

            // Legacy compatibility for AlarmScreenActivity
            putExtra("customer_id", notificationId) // Use notification_id as customer_id for compatibility

            when (notificationType) {
                NotificationType.APPOINTMENT_REMINDER -> {
                    putExtra("date", extractDateFromMessage(message))
                    putExtra("time", extractTimeFromMessage(message))
                    putExtra("address", location ?: "")
                    putExtra("notes", message)
                }
                NotificationType.NOTE_REMINDER -> {
                    putExtra("date", getCurrentDate())
                    putExtra("time", getCurrentTime())
                    putExtra("address", "")
                    putExtra("notes", message)
                }
                else -> {
                    putExtra("date", getCurrentDate())
                    putExtra("time", getCurrentTime())
                    putExtra("address", location ?: "")
                    putExtra("notes", message)
                }
            }
        }
    }

    private fun addNotificationActions(
        context: Context,
        builder: NotificationCompat.Builder,
        notificationType: NotificationType,
        relatedId: Int,
        location: String?,
        notificationId: Int,
        title: String,
        message: String
    ) {
        // Dismiss action
        val dismissIntent = Intent(context, DismissAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType.name)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.alarm_off, "Tắt", dismissPendingIntent)

        // Snooze action
        val snoozeIntent = Intent(context, SnoozeAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType.name)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("location", location)

            // Legacy compatibility
            putExtra("customer_id", notificationId)
            putExtra("date", extractDateFromMessage(message))
            putExtra("time", extractTimeFromMessage(message))
            putExtra("address", location ?: "")
            putExtra("notes", message)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.zzz, "Báo lại", snoozePendingIntent)

        // Type-specific actions
        when (notificationType) {
            NotificationType.APPOINTMENT_REMINDER -> {
                if (!location.isNullOrBlank()) {
                    val navigationIntent = Intent(context, SolutionActivity::class.java).apply {
                        putExtra("location", location)
                        putExtra("notification_id", notificationId)
                    }
                    val navigationPendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId + 3000,
                        navigationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(R.drawable.ic_navigation, "Dẫn đường", navigationPendingIntent)
                }
            }
            NotificationType.NOTE_REMINDER -> {
                val openNoteIntent = Intent(context, SolutionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("action", "open_note")
                    putExtra("note_id", relatedId)
                }
                val openNotePendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 3000,
                    openNoteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_note, "Mở ghi chú", openNotePendingIntent)
            }
            else -> {}
        }
    }

    private fun formatNotificationContent(
        title: String,
        message: String,
        location: String?,
        notificationType: NotificationType
    ): String {
        return buildString {
            val icon = when (notificationType) {
                NotificationType.APPOINTMENT_REMINDER -> "📅"
                NotificationType.NOTE_REMINDER -> "📝"
                else -> "🔔"
            }

            append("$icon $title\n")
            append(message)

            if (!location.isNullOrBlank()) {
                append("\n📍 $location")
            }

            append("\n\n⏰ ${getCurrentTime()}")
        }
    }

    private fun getShortContent(message: String): String {
        return if (message.length > 50) {
            message.substring(0, 47) + "..."
        } else {
            message
        }
    }

    private fun extractDateFromMessage(message: String): String {
        return getCurrentDate()
    }

    private fun extractTimeFromMessage(message: String): String {
        val timeRegex = "lúc (\\d{2}:\\d{2})".toRegex()
        val matchResult = timeRegex.find(message)
        return matchResult?.groupValues?.get(1) ?: getCurrentTime()
    }

    private fun getCurrentDate(): String {
        val formatter =SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun updateNotificationStatus(notificationId: Int) {
        receiverScope.launch {
            try {
                notificationRepository.markAsSent(notificationId)
                Log.d(TAG, "Notification status updated for ID: $notificationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification status", e)
            }
        }
    }
}