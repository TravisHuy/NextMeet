package com.nhathuy.nextmeet

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
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.model.NotificationAction
import com.nhathuy.nextmeet.utils.Constant

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Extract new notification system extras
        val notificationId = intent.getIntExtra("notification_id", -1)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val message = intent.getStringExtra("message") ?: ""
        val location = intent.getStringExtra("location")
        val relatedId = intent.getIntExtra("related_id", -1)
        val notificationTypeStr = intent.getStringExtra("notification_type") ?: NotificationType.APPOINTMENT_REMINDER.name
        
        // Parse notification type
        val notificationType = try {
            NotificationType.valueOf(notificationTypeStr)
        } catch (e: IllegalArgumentException) {
            Log.w("AlarmReceiver", "Unknown notification type: $notificationTypeStr, defaulting to APPOINTMENT_REMINDER")
            NotificationType.APPOINTMENT_REMINDER
        }

        Log.d("AlarmReceiver", "Received alarm for notification $notificationId, type: $notificationType, related_id: $relatedId")

        // Wake up the device with improved error handling
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, 
            "NextMeet:AlarmWakeLock"
        )
        
        try {
            wakeLock.acquire(5 * 60 * 1000L) // 5 minutes timeout
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to acquire wake lock", e)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Get default alarm sound
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Create notification channels for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(context, notificationManager, alarmSound)
        }

        // Generate notification content based on type
        val (notificationTitle, notificationContent, bigText) = generateNotificationContent(
            title, message, location, notificationType
        )

        // Create pending intents based on notification type
        val contentIntent = createContentIntent(context, notificationType, relatedId, notificationId)
        val dismissIntent = createDismissIntent(context, notificationId)
        val snoozeIntent = createSnoozeIntent(context, intent, notificationId)

        // Get appropriate channel ID
        val channelId = when (notificationType) {
            NotificationType.NOTE_REMINDER -> Constant.NOTE_CHANNEL_ID
            else -> Constant.APPOINTMENT_CHANNEL_ID
        }

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(alarmSound)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissIntent)
            .addAction(R.drawable.zzz, "Snooze", snoozeIntent)

        // Add additional actions based on notification type
        if (notificationType == NotificationType.APPOINTMENT_REMINDER && !location.isNullOrBlank()) {
            val navigationIntent = createNavigationIntent(context, relatedId, notificationId)
            notificationBuilder.addAction(R.drawable.ic_navigation, "Navigate", navigationIntent)
        }

        val notification = notificationBuilder.build()
        notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT

        notificationManager.notify(notificationId, notification)
        Log.d("AlarmReceiver", "Notification sent for ID $notificationId")

        // Release wake lock safely
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to release wake lock", e)
        }
    }

    /**
     * Create notification channels for both appointment and note reminders
     */
    private fun createNotificationChannels(context: Context, notificationManager: NotificationManager, alarmSound: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Appointment reminders channel
            val appointmentChannel = NotificationChannel(
                Constant.APPOINTMENT_CHANNEL_ID,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for appointment reminders"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(
                    alarmSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            // Note reminders channel
            val noteChannel = NotificationChannel(
                Constant.NOTE_CHANNEL_ID,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for note reminders"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(
                    alarmSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            notificationManager.createNotificationChannel(appointmentChannel)
            notificationManager.createNotificationChannel(noteChannel)
        }
    }

    /**
     * Generate notification content based on notification type
     */
    private fun generateNotificationContent(
        title: String,
        message: String,
        location: String?,
        notificationType: NotificationType
    ): Triple<String, String, String> {
        val notificationTitle = when (notificationType) {
            NotificationType.NOTE_REMINDER -> "Note Reminder"
            NotificationType.APPOINTMENT_REMINDER -> "Appointment Reminder"
            else -> title
        }

        val content = message.ifEmpty {
            when (notificationType) {
                NotificationType.NOTE_REMINDER -> "You have a note reminder"
                NotificationType.APPOINTMENT_REMINDER -> "You have an upcoming appointment"
                else -> "You have a reminder"
            }
        }

        val bigText = buildString {
            append(content)
            if (!location.isNullOrBlank()) {
                append("\n📍 Location: $location")
            }
        }

        return Triple(notificationTitle, content, bigText)
    }

    /**
     * Create content intent based on notification type
     */
    private fun createContentIntent(
        context: Context,
        notificationType: NotificationType,
        relatedId: Int,
        notificationId: Int
    ): PendingIntent {
        val intent = when (notificationType) {
            NotificationType.NOTE_REMINDER -> {
                // Intent to open note detail activity
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(Constant.EXTRA_NOTE_ID, relatedId)
                    putExtra("action", NotificationAction.OPEN_NOTE.name)
                }
            }
            NotificationType.APPOINTMENT_REMINDER -> {
                // Intent to open appointment detail activity
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(Constant.EXTRA_APPOINTMENT_ID, relatedId)
                    putExtra("action", NotificationAction.OPEN_APPOINTMENT.name)
                }
            }
            else -> {
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
        }

        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Create dismiss intent
     */
    private fun createDismissIntent(context: Context, notificationId: Int): PendingIntent {
        val dismissIntent = Intent(context, DismissAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Create snooze intent
     */
    private fun createSnoozeIntent(context: Context, originalIntent: Intent, notificationId: Int): PendingIntent {
        val snoozeIntent = Intent(context, SnoozeAlarmReceiver::class.java).apply {
            // Pass through the original intent extras for snoozing
            putExtra("notification_id", notificationId)
            putExtra("title", originalIntent.getStringExtra("title"))
            putExtra("message", originalIntent.getStringExtra("message"))
            putExtra("location", originalIntent.getStringExtra("location"))
            putExtra("related_id", originalIntent.getIntExtra("related_id", -1))
            putExtra("notification_type", originalIntent.getStringExtra("notification_type"))
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId + 10000, // Offset to avoid conflicts
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Create navigation intent for appointments
     */
    private fun createNavigationIntent(context: Context, appointmentId: Int, notificationId: Int): PendingIntent {
        val navigationIntent = Intent(context, TurnByTurnNavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Constant.EXTRA_APPOINTMENT_ID, appointmentId)
            putExtra("action", NotificationAction.START_NAVIGATION.name)
        }
        return PendingIntent.getActivity(
            context,
            notificationId + 20000, // Offset to avoid conflicts
            navigationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}