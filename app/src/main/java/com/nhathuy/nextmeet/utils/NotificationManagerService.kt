package com.nhathuy.nextmeet.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nhathuy.nextmeet.AlarmReceiver
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.Notification
import com.nhathuy.nextmeet.model.NotificationAction
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.repository.NotificationRepository
import com.nhathuy.nextmeet.ui.SolutionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
        const val APPOINTMENT_ALARM_CHANNEL_ID = "appointment_alarms"
        const val NOTE_CHANNEL_ID = "note_reminders"
        const val NOTE_ALARM_CHANNEL_ID = "note_alarms"
        const val REMINDER_MINUTES_DEFAULT = 5L // Nhắc nhở trước 5 phút
        const val MAX_RETRIES = 3 // Số lần thử lại khi lên lịch báo thức
        const val RETRY_DELAY_MS = 100L // Thời gian chờ giữa các lần thử

        // Thêm constant để phân biệt reminder và alarm
        const val NOTIFICATION_TYPE_REMINDER = "reminder"
        const val NOTIFICATION_TYPE_ALARM = "alarm"
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
            val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notificationSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.notification}")

            val alarmAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val notificationAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channels = listOf(
                // Kênh cho thông báo nhắc nhở (5 phút trước)
                NotificationChannel(
                    APPOINTMENT_CHANNEL_ID,
                    "Nhắc nhở cuộc hẹn",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Thông báo nhắc nhở về các cuộc hẹn sắp tới"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 100, 250)
                    setSound(notificationSoundUri, notificationAudioAttributes)
                    setShowBadge(true)
                    enableLights(true)
                },
                // Kênh cho báo thức (đúng giờ)
                NotificationChannel(
                    APPOINTMENT_ALARM_CHANNEL_ID,
                    "Báo thức cuộc hẹn",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Báo thức khi đến giờ cuộc hẹn"
                    enableVibration(false)
                    setSound(null,null)
                },
                NotificationChannel(
                    NOTE_CHANNEL_ID,
                    "Nhắc nhở ghi chú",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Thông báo nhắc nhở về các ghi chú sắp tới"
                    vibrationPattern = longArrayOf(0, 250, 100, 250)
                    enableVibration(true)
                    setSound(notificationSoundUri, notificationAudioAttributes)
                },
                NotificationChannel(
                    NOTE_ALARM_CHANNEL_ID,
                    "Nhắc nhở ghi chú",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Thông báo nhắc nhở về các ghi chú đến giờ"
                    enableVibration(false)
                    setSound(null,null)
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
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Lên lịch cả thông báo nhắc nhở (5 phút trước) và báo thức (đúng giờ) cho cuộc hẹn
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
            if (!canScheduleExactAlarms()) {
                Log.w("NotificationManager", "Không có quyền báo thức chính xác")
                return@withContext false
            }

            cancelPendingOperation(appointmentId)

            val currentTime = System.currentTimeMillis()
            val reminderTime = appointmentTime - REMINDER_MINUTES_DEFAULT * 60 * 1000

            var reminderSuccess = true
            var alarmSuccess = true

            // 1. Lên lịch thông báo nhắc nhở (5 phút trước) nếu chưa quá giờ
            if (reminderTime > currentTime) {
                val reminderNotification = Notification(
                    id = 0,
                    userId = userId,
                    title = "🔔 Nhắc nhở cuộc hẹn",
                    message = buildReminderMessage(title, description, location, contactName, appointmentTime),
                    notificationType = NotificationType.APPOINTMENT_REMINDER,
                    relatedId = appointmentId,
                    scheduledTime = reminderTime,
                    actionType = NotificationAction.OPEN_APPOINTMENT
                )

                val reminderNotificationId = notificationRepository.insertNotification(reminderNotification).getOrThrow().toInt()

                reminderSuccess = scheduleAlarmWithRetry(
                    reminderNotificationId,
                    reminderTime,
                    reminderNotification.title,
                    reminderNotification.message,
                    location,
                    appointmentId,
                    NotificationType.APPOINTMENT_REMINDER,
                    isAlarm = false
                )

                if (!reminderSuccess) {
                    notificationRepository.deleteNotificationById(reminderNotificationId)
                    Log.e("NotificationManager", "Lỗi lên lịch reminder, xoá thông báo đã lưu")
                }
            }

            // 2. Lên lịch báo thức (đúng giờ cuộc hẹn) nếu chưa quá giờ
            if (appointmentTime > currentTime) {
                val alarmNotification = Notification(
                    id = 0,
                    userId = userId,
                    title = "⏰ Đến giờ cuộc hẹn!",
                    message = buildAlarmMessage(title, description, location, contactName),
                    notificationType = NotificationType.APPOINTMENT_REMINDER,
                    relatedId = appointmentId,
                    scheduledTime = appointmentTime,
                    actionType = NotificationAction.OPEN_APPOINTMENT
                )

                val alarmNotificationId = notificationRepository.insertNotification(alarmNotification).getOrThrow().toInt()

                alarmSuccess = scheduleAlarmWithRetry(
                    alarmNotificationId,
                    appointmentTime,
                    alarmNotification.title,
                    alarmNotification.message,
                    location,
                    appointmentId,
                    NotificationType.APPOINTMENT_REMINDER,
                    isAlarm = true
                )

                if (!alarmSuccess) {
                    notificationRepository.deleteNotificationById(alarmNotificationId)
                    Log.e("NotificationManager", "Lỗi lên lịch alarm, xoá thông báo đã lưu")
                }
            }

            val overallSuccess = reminderSuccess && alarmSuccess

            if (overallSuccess) {
                Log.d("NotificationManager", "Đã lên lịch reminder với notification lúc $reminderTime và alarm lúc $appointmentTime")
            }

            return@withContext overallSuccess

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
            cancelPendingOperation(noteId)

            val currentTime = System.currentTimeMillis()
            val reminderTime = noteTime - REMINDER_MINUTES_DEFAULT * 60 * 1000

            var reminderSuccess = true
            var alarmSuccess = true

            if(reminderTime > currentTime){
                val reminderNotification = Notification(
                    id = 0,
                    userId = userId,
                    title = "📝 Nhắc nhở ghi chú",
                    message = buildReminderNotification(title,content,noteTime),
                    notificationType = NotificationType.NOTE_REMINDER,
                    relatedId = noteId,
                    scheduledTime = reminderTime,
                    actionType = NotificationAction.OPEN_NOTE
                )

                val notificationId = notificationRepository.insertNotification(reminderNotification).getOrThrow().toInt()

                reminderSuccess = scheduleAlarmWithRetry(
                    notificationId, reminderTime, reminderNotification.title, reminderNotification.message,
                    null, noteId, NotificationType.NOTE_REMINDER, isAlarm = false
                )

                if (!reminderSuccess) {
                    Log.e("NotificationManager",
                        "Lỗi lên lịch, xoá thông báo đã lưu")
                    notificationRepository.deleteNotificationById(notificationId)
                }
            }

            if(noteTime > currentTime){
                val alarmNotification = Notification(
                    id = 0,
                    userId = userId,
                    title = "⏰ Đã đến giờ ghi chú",
                    message = buildAlarmNotification(title,content),
                    notificationType = NotificationType.NOTE_REMINDER,
                    relatedId = noteId,
                    scheduledTime = noteTime,
                    actionType = NotificationAction.OPEN_NOTE
                )

                val notificationId = notificationRepository.insertNotification(alarmNotification).getOrThrow().toInt()

                alarmSuccess = scheduleAlarmWithRetry(
                    notificationId, noteTime, alarmNotification.title, alarmNotification.message,
                    null, noteId, NotificationType.NOTE_REMINDER, isAlarm = true
                )

                if (!alarmSuccess) {
                    Log.e("NotificationManager", "Lỗi lên lịch, xoá thông báo đã lưu")
                    notificationRepository.deleteNotificationById(notificationId)
                }
            }

            val overallSuccess = reminderSuccess && alarmSuccess

            if (overallSuccess) {
                Log.d("NotificationManager", "Đã lên lịch reminder lúc $reminderTime và alarm lúc $noteTime")
            }
            return@withContext overallSuccess
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
     * gửi thông báo cho status của cuộc hẹn
     */
    suspend fun sendSimpleNotification(
        appointmentId : Int,
        title:String,
        message: String
    ) = withContext(Dispatchers.IO) {
        try {
            val notificationId = appointmentId + 1000

            val openAppIntent = Intent(context, SolutionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("appointment_id", appointmentId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, notificationId, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(
                context, APPOINTMENT_CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 200, 100, 200))
                .build()
            notificationManager.notify(notificationId, notification)
            Log.d("NotificationManager", "Sent simple notification: $title")
        }
        catch (e: Exception){
            Log.e("NotificationManager", "Error sending simple notification", e)
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
        notificationType: NotificationType,
        isAlarm: Boolean = false
    ): Boolean {
        repeat(MAX_RETRIES) { attempt ->
            try {
                scheduleAlarm(notificationId, triggerTime, title, message, location, relatedId, notificationType, isAlarm)
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
        notificationType: NotificationType,
        isAlarm: Boolean = false
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("location", location)
            putExtra("related_id", relatedId)
            putExtra("notification_type", notificationType.name)
            putExtra("is_alarm", isAlarm) // Thêm flag để phân biệt reminder và alarm
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
                    } else {
                        throw SecurityException("Chưa cấp quyền SCHEDULE_EXACT_ALARM")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }

            val type = if (isAlarm) "alarm" else "reminder"
            Log.d("NotificationManager", "Đặt $type thành công lúc $triggerTime")
        } catch (e: SecurityException) {
            Log.e("NotificationManager", "Thiếu quyền khi đặt báo thức: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("NotificationManager", "Lỗi khi đặt báo thức: ${e.message}")
            throw e
        }
    }

    /**
     * Tạo nội dung thông báo nhắc nhở (5 phút trước)
     */
    private fun buildReminderMessage(
        title: String,
        description: String,
        location: String?,
        contactName: String?,
        appointmentTime: Long
    ): String = buildString {
        val appointmentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(appointmentTime))
        append("⏰ Cuộc hẹn sắp bắt đầu lúc $appointmentTimeStr\n")
        append("📅 Tiêu đề: $title")
        if (description.isNotBlank()) append("\n📝 Nội dung: $description")
        if (!contactName.isNullOrBlank()) append("\n👤 Với: $contactName")
        if (!location.isNullOrBlank()) append("\n📍 Tại: $location")
        append("\n💡 Bạn có 5 phút để chuẩn bị!")
    }

    /**
     * Tạo nội dung báo thức (đúng giờ)
     */
    private fun buildAlarmMessage(
        title: String,
        description: String,
        location: String?,
        contactName: String?
    ): String = buildString {
        append("🚨 ĐÃ ĐẾN GIỜ CUỘC HẸN!\n")
        append("📅 $title")
        if (description.isNotBlank()) append("\n📝 $description")
        if (!contactName.isNullOrBlank()) append("\n👤 Với: $contactName")
        if (!location.isNullOrBlank()) append("\n📍 Tại: $location")
        append("\n\n⚡ Hãy bắt đầu cuộc hẹn ngay!")
    }

    /**
     * Tạo nội dung cho ghi chú trước 5 phút
     */
    private fun buildReminderNotification(title:String,content:String,reminderTime:Long):String = buildString{
        val appointmentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminderTime))
        append("⏰ Ghi chú sắp bắt đầu lúc $appointmentTimeStr\n")
        append("📅 Tiêu đề: $title\n")
        append("📝 Nội dung: $content\n")
        append("💡 Bạn có 5 phút để chuẩn bị!")
    }


    /**
     * Tạo noi dung ghi chu dung hẹn
     */
    private fun buildAlarmNotification(
        title:String,content:String):String = buildString {
        append("🚨 ĐÃ ĐẾN GIỜ CUỘC HẸN!\n")
        append("📅 Tiêu đề: $title\n")
        append("📝 Nội dung: $content\n")
        append("⚡ Hãy bắt đầu cuộc hẹn ngay!")
    }

    /**
     * Huỷ tất cả thông báo theo relatedId và loại thông báo
     */
    suspend fun cancelNotificationsByRelatedId(
        relatedId: Int,
        notificationType: NotificationType
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val notificationList = notificationRepository
                .getNotificationsByRelatedId(relatedId, notificationType)
                .first()

            notificationList.forEach { notification ->
                try {
                    cancelPendingOperation(notification.id)

                    val intent = Intent(context, AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        notification.id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.cancel(pendingIntent)
                    notificationManager.cancel(notification.id)

                    Log.d("NotificationManager", "Đã huỷ thông báo ${notification.id}")
                } catch (e: Exception) {
                    Log.e("NotificationManager", "Lỗi khi huỷ thông báo ${notification.id}", e)
                }
            }

            val result = notificationRepository.deleteNotificationsByRelatedId(relatedId, notificationType)

            if (result.isSuccess) {
                Log.d("NotificationManager", "Đã huỷ tất cả thông báo cho relatedId: $relatedId, type: $notificationType")
                result.getOrDefault(false)
            } else {
                Log.e("NotificationManager", "Lỗi khi xoá thông báo từ database", result.exceptionOrNull())
                false
            }

        } catch (e: Exception) {
            Log.e("NotificationManager", "Lỗi khi huỷ thông báo theo relatedId", e)
            false
        }
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