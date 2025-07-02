package com.nhathuy.nextmeet

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
            val isAlarm = intent.getBooleanExtra("is_alarm", false) // QUAN TRỌNG: Phân biệt reminder và alarm

            Log.d(TAG, "Processing notification: ID=$notificationId, Type=$notificationTypeString, IsAlarm=$isAlarm")

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

            // Xử lý khác nhau cho reminder và alarm
            if (isAlarm) {
                // ĐÂY LÀ ALARM - Báo thức đúng giờ (cần toàn màn hình + âm thanh to)
                handleAlarmNotification(
                    context = context,
                    notificationId = notificationId,
                    title = title,
                    message = message,
                    location = location,
                    relatedId = relatedId,
                    notificationType = notificationType
                )
            } else {
                // ĐÂY LÀ REMINDER - Thông báo nhắc nhở (chỉ notification thường)
                handleReminderNotification(
                    context = context,
                    notificationId = notificationId,
                    title = title,
                    message = message,
                    location = location,
                    relatedId = relatedId,
                    notificationType = notificationType
                )
            }

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

    /**
     * Xử lý thông báo REMINDER (5 phút trước) - Chỉ hiển thị notification thường
     */
    private fun handleReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ) {
        Log.d(TAG, "Handling REMINDER notification")

        showReminderNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            location = location,
            relatedId = relatedId,
            notificationType = notificationType
        )
    }

    /**
     * Xử lý thông báo ALARM (đúng giờ) - Hiển thị toàn màn hình + âm thanh báo thức
     */
    private fun handleAlarmNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ) {
        Log.d(TAG, "Handling ALARM notification")

        // 1. Hiển thị notification với âm thanh báo thức mạnh
        showAlarmNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            location = location,
            relatedId = relatedId,
            notificationType = notificationType
        )

        // 2. Mở màn hình báo thức toàn màn hình
        launchAlarmScreen(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            location = location,
            relatedId = relatedId,
            notificationType = notificationType
        )
    }

    /**
     * Hiển thị notification REMINDER (nhẹ nhàng hơn)
     */
    private fun showReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = when (notificationType) {
            NotificationType.APPOINTMENT_REMINDER -> NotificationManagerService.APPOINTMENT_CHANNEL_ID
            NotificationType.NOTE_REMINDER -> NotificationManagerService.NOTE_CHANNEL_ID
            else -> NotificationManagerService.APPOINTMENT_CHANNEL_ID
        }

        // Intent để mở app khi tap vào notification
        val openAppIntent = Intent(context, SolutionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "open_appointment")
            putExtra("appointment_id", relatedId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.notification}")

        val notificationContent = formatReminderContent(title, message, location, notificationType)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(getShortContent(message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Ưu tiên thấp hơn alarm
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(notificationSoundUri)
            .setAutoCancel(true) // Tự động tắt khi tap
            .setOngoing(false) // Không cố định
            .setVibrate(longArrayOf(0, 150, 100, 150)) // Rung nhẹ hơn
            .setContentIntent(openAppPendingIntent)
            .setOnlyAlertOnce(true)

        // Thêm action cho reminder
//        addReminderActions(context, notificationBuilder, notificationType, relatedId, location, notificationId)

//        val notification = notificationBuilder.build()
//        notification.flags = notification.flags and NotificationCompat.FLAG_INSISTENT.inv()

        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Reminder notification shown for ID: $notificationId")
    }

    /**
     * Hiển thị notification ALARM (mạnh mẽ hơn)
     */
    private fun showAlarmNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = when (notificationType) {
            NotificationType.APPOINTMENT_REMINDER -> NotificationManagerService.APPOINTMENT_ALARM_CHANNEL_ID // Dùng alarm channel
            NotificationType.NOTE_REMINDER -> NotificationManagerService.NOTE_ALARM_CHANNEL_ID
            else -> NotificationManagerService.APPOINTMENT_ALARM_CHANNEL_ID
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

        // Dismiss intent
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

//        // Âm thanh báo thức mạnh
//        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notificationContent = formatAlarmContent(title, message, location, notificationType)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ $title")
            .setContentText("ĐÃ ĐẾN GIỜ CUỘC HẸN!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
            .setPriority(NotificationCompat.PRIORITY_MAX) // Ưu tiên cao nhất
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false) // Không tự động tắt
            .setOngoing(true) // Cố định trên thanh thông báo
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000)) // Rung mạnh
//            .setSound(alarmSound) // Âm thanh báo thức
            .setContentIntent(alarmScreenPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setFullScreenIntent(alarmScreenPendingIntent, true) // Toàn màn hình

        // Thêm action cho alarm
        addAlarmActions(context, notificationBuilder, notificationType, relatedId, location, notificationId, title, message)

        val notification = notificationBuilder.build()
//        notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT // Lặp lại âm thanh

        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Alarm notification shown for ID: $notificationId")
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

            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("location", location)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType.name)
            putExtra("customer_id", notificationId)

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

    /**
     * Thêm action cho REMINDER (ít action hơn)
     */
    private fun addReminderActions(
        context: Context,
        builder: NotificationCompat.Builder,
        notificationType: NotificationType,
        relatedId: Int,
        location: String?,
        notificationId: Int
    ) {
        // Action xem chi tiết
        val viewIntent = Intent(context, SolutionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "view_appointment")
            putExtra("appointment_id", relatedId)
        }
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1000,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.ic_eye, "Xem", viewPendingIntent)

        // Action điều hướng (chỉ khi có địa chỉ)
        if (!location.isNullOrBlank()) {
            val navIntent = Intent(context, SolutionActivity::class.java).apply {
                putExtra("location", location)
                putExtra("notification_id", notificationId)
            }
            val navPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2000,
                navIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_navigation, "Dẫn đường", navPendingIntent)
        }
    }

    /**
     * Thêm action cho ALARM (nhiều action hơn)
     */
    private fun addAlarmActions(
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

        // Navigation action (chỉ cho appointment)
        if (notificationType == NotificationType.APPOINTMENT_REMINDER && !location.isNullOrBlank()) {
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

    private fun formatReminderContent(
        title: String,
        message: String,
        location: String?,
        notificationType: NotificationType
    ): String {
        return buildString {
//            append("🔔 NHẮC NHỞ: Còn 5 phút nữa!\n\n")
//            append("📅 $title\n")
            append(message)
//            if (!location.isNullOrBlank()) {
//                append("\n📍 $location")
//            }
            append("\n💡 Hãy chuẩn bị để bắt đầu cuộc hẹn!")
        }
    }

    private fun formatAlarmContent(
        title: String,
        message: String,
        location: String?,
        notificationType: NotificationType
    ): String {
        return buildString {
            append("🚨 ĐÃ ĐẾN GIỜ!\n\n")
            append("📅 $title\n")
            append(message)
            if (!location.isNullOrBlank()) {
                append("\n📍 $location")
            }
            append("\n\n⚡ Cuộc hẹn đã bắt đầu!")
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
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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