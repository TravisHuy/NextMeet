package com.nhathuy.nextmeet.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.nhathuy.nextmeet.AlarmReceiver
import com.nhathuy.nextmeet.model.Notification
import com.nhathuy.nextmeet.model.NotificationAction
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManagerService @Inject constructor(
    private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    // Hệ thống quản lý báo thức và thông báo của Android
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // CoroutineScope để xử lý các tác vụ chạy ngầm
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Quản lý các tác vụ đang chờ xử lý (có thể huỷ khi cần)
    private val pendingOperations = ConcurrentHashMap<Int, Job>()

    companion object {
        const val APPOINTMENT_CHANNEL_ID = "appointment_reminders"
        const val NOTE_CHANNEL_ID = "note_reminders"
        const val REMINDER_MINUTES_DEFAULT = 5L // Nhắc nhở trước 5 phút
        const val MAX_RETRIES = 3 // Số lần thử lại khi lên lịch báo thức
        const val RETRY_DELAY_MS = 100L // Thời gian chờ giữa các lần thử
    }

    init {
        createNotificationChannels()
    }

    /**
     * Tạo các kênh thông báo cho Android 8.0 trở lên
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channels = listOf(
                NotificationChannel(
                    APPOINTMENT_CHANNEL_ID,
                    "Nhắc nhở cuộc hẹn",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Thông báo nhắc nhở về các cuộc hẹn sắp tới"
                    enableVibration(true)
                    setSound(alarmSound, audioAttributes)
                },
                NotificationChannel(
                    NOTE_CHANNEL_ID,
                    "Nhắc nhở ghi chú",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Thông báo nhắc nhở về các ghi chú sắp tới"
                    enableVibration(true)
                    setSound(alarmSound, audioAttributes)
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }

    /**
     * Kiểm tra ứng dụng có quyền lên lịch báo thức chính xác hay không (Android 12+)
     */
    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Log.d("NotificationManager", "Can schedule exact alarms: $canSchedule")
            canSchedule
        } else {
            Log.d("NotificationManager", "Android version < 12, exact alarms allowed by default")
            true
        }
    }

    /**
     * Lên lịch thông báo nhắc cuộc hẹn trước thời gian đã đặt
     */
    suspend fun scheduleAppointmentNotification(
        userId: Int,
        appointmentId: Int,
        title: String,
        description: String,
        appointmentTime: Long,
        location: String? = null,
        contactName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("NotificationManager", "Starting appointment notification scheduling...")
            Log.d("NotificationManager", "UserId: $userId, AppointmentId: $appointmentId")
            Log.d("NotificationManager", "Title: $title, ContactName: $contactName")
            Log.d("NotificationManager", "AppointmentTime: $appointmentTime, Location: $location")
            
            if (!canScheduleExactAlarms()) {
                Log.w("NotificationManager", "Không có quyền báo thức chính xác")
                return@withContext false
            }

            cancelPendingOperation(appointmentId)

            val reminderTime = appointmentTime - REMINDER_MINUTES_DEFAULT * 60 * 1000
            Log.d("NotificationManager", "Calculated reminder time: $reminderTime")
            Log.d("NotificationManager", "Current time: ${System.currentTimeMillis()}")
            
            if (reminderTime <= System.currentTimeMillis()) {
                Log.w("NotificationManager", "Không thể đặt báo thức trong quá khứ")
                Log.w("NotificationManager", "Reminder time: $reminderTime, Current time: ${System.currentTimeMillis()}")
                return@withContext false
            }

            val notification = Notification(
                id = 0,
                userId = userId,
                title = "🕐 Cuộc hẹn sắp diễn ra",
                message = buildAppointmentMessage(title, description, location, contactName, appointmentTime),
                notificationType = NotificationType.APPOINTMENT_REMINDER,
                relatedId = appointmentId,
                scheduledTime = reminderTime,
                actionType = NotificationAction.OPEN_APPOINTMENT
            )

            Log.d("NotificationManager", "Created notification: ${notification.message}")

            // Chuyển đổi Long sang Int an toàn
            val notificationId = notificationRepository.insertNotification(notification).getOrThrow().toInt()
            Log.d("NotificationManager", "Saved notification to database with ID: $notificationId")

            val success = scheduleAlarmWithRetry(
                notificationId, reminderTime, notification.title, notification.message,
                location, appointmentId, NotificationType.APPOINTMENT_REMINDER
            )

            if (success) {
                Log.d("NotificationManager", "Đã lên lịch nhắc cuộc hẹn lúc $reminderTime")
                true
            } else {
                notificationRepository.deleteNotificationById(notificationId)
                Log.e("NotificationManager", "Lỗi lên lịch, xoá thông báo đã lưu")
                false
            }

        } catch (e: Exception) {
            Log.e("NotificationManager", "Lỗi khi đặt lịch cuộc hẹn", e)
            false
        }
    }

    /**
     * Lên lịch thông báo nhắc ghi chú
     */
    suspend fun scheduleNoteNotification(
        userId: Int,
        noteId: Int,
        title: String,
        content: String,
        noteTime: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!canScheduleExactAlarms()) {
                Log.w("NotificationManager", "Không có quyền báo thức chính xác")
                return@withContext false
            }

            val reminderTime = noteTime - REMINDER_MINUTES_DEFAULT * 60 * 1000
            if (reminderTime <= System.currentTimeMillis()) {
                Log.w("NotificationManager", "Không thể đặt báo thức trong quá khứ")
                return@withContext false
            }

            val notification = Notification(
                id = 0,
                userId = userId,
                title = "📝 Nhắc nhở ghi chú",
                message = "📝 $title\n$content",
                notificationType = NotificationType.NOTE_REMINDER,
                relatedId = noteId,
                scheduledTime = reminderTime,
                actionType = NotificationAction.OPEN_NOTE
            )

            // Chuyển đổi Long sang Int an toàn
            val notificationId = notificationRepository.insertNotification(notification).getOrThrow().toInt()

            val success = scheduleAlarmWithRetry(
                notificationId, reminderTime, notification.title, notification.message,
                null, noteId, NotificationType.NOTE_REMINDER
            )

            if (success) {
                Log.d("NotificationManager", "Đã lên lịch nhắc ghi chú lúc $reminderTime")
                true
            } else {
                notificationRepository.deleteNotificationById(notificationId)
                Log.e("NotificationManager", "Lỗi lên lịch, xoá thông báo đã lưu")
                false
            }
        } catch (e: Exception) {
            Log.e("NotificationManager", "Lỗi khi đặt lịch ghi chú", e)
            false
        }
    }

    /**
     * Huỷ thông báo theo ID (từ AlarmManager, NotificationManager, Room)
     */
    suspend fun cancelNotification(notificationId: Int) = withContext(Dispatchers.IO) {
        try {
            cancelPendingOperation(notificationId)

            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            notificationManager.cancel(notificationId)
            notificationRepository.deleteNotificationById(notificationId)

            Log.d("NotificationManager", "Đã huỷ thông báo $notificationId")
        } catch (e: Exception) {
            Log.e("NotificationManager", "Lỗi khi huỷ thông báo", e)
        }
    }

    /**
     * Thử lên lịch báo thức tối đa 3 lần nếu xảy ra lỗi
     */
    private suspend fun scheduleAlarmWithRetry(
        notificationId: Int,
        triggerTime: Long,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ): Boolean {
        repeat(MAX_RETRIES) { attempt ->
            try {
                scheduleAlarm(notificationId, triggerTime, title, message, location, relatedId, notificationType)
                return true
            } catch (e: SecurityException) {
                Log.e("NotificationManager", "Lỗi quyền trên lần thử ${attempt + 1}: ${e.message}")
                return false
            } catch (e: Exception) {
                Log.e("NotificationManager", "Lỗi thử lần ${attempt + 1}", e)
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }
        return false
    }

    /**
     * Lên lịch báo thức (AlarmManager) tại thời điểm chỉ định
     */
    private fun scheduleAlarm(
        notificationId: Int,
        triggerTime: Long,
        title: String,
        message: String,
        location: String?,
        relatedId: Int,
        notificationType: NotificationType
    ) {
        Log.d("NotificationManager", "Setting up alarm for notification $notificationId")
        Log.d("NotificationManager", "Trigger time: $triggerTime, Related ID: $relatedId")
        Log.d("NotificationManager", "Notification type: ${notificationType.name}")
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("location", location)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        Log.d("NotificationManager", "Scheduled exact alarm for Android 12+ with permission")
                    } else {
                        throw SecurityException("Chưa cấp quyền SCHEDULE_EXACT_ALARM")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d("NotificationManager", "Scheduled exact alarm for Android 6-11")
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d("NotificationManager", "Scheduled exact alarm for Android < 6")
                }
            }

            Log.d("NotificationManager", "Đặt báo thức thành công lúc $triggerTime")
        } catch (e: SecurityException) {
            Log.e("NotificationManager", "Thiếu quyền khi đặt báo thức: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("NotificationManager", "Lỗi khi đặt báo thức: ${e.message}")
            throw e
        }
    }

    /**
     * Tạo nội dung thông báo cho cuộc hẹn
     */
    private fun buildAppointmentMessage(
        title: String,
        description: String,
        location: String?,
        contactName: String?,
        appointmentTime: Long
    ): String = buildString {
        val appointmentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(appointmentTime))
        append("⏰ Cuộc hẹn lúc $appointmentTimeStr\n")
        append("📅 $title")
        if (description.isNotBlank()) append("\n📝 $description")
        if (!contactName.isNullOrBlank()) append("\n👤 Với: $contactName")
        if (!location.isNullOrBlank()) append("\n📍 Tại: $location")
    }

    /**
     * Hủy tác vụ đang chờ xử lý (nếu có)
     */
    private fun cancelPendingOperation(id: Int) {
        pendingOperations[id]?.cancel()
        pendingOperations.remove(id)
    }

    /**
     * Kiểm tra quyền đặt báo thức chính xác (dùng được ở UI)
     */
    fun hasExactAlarmPermission(): Boolean = canScheduleExactAlarms()

    /**
     * Dọn dẹp tất cả coroutine khi không dùng nữa
     */
    fun cleanup() {
        serviceScope.cancel()
        pendingOperations.values.forEach { it.cancel() }
        pendingOperations.clear()
    }
}