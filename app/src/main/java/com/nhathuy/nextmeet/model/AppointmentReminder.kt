package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho lời nhắc cuộc hẹn trong cơ sở dữ liệu Room.
 *
 * Mỗi lời nhắc liên kết đến một cuộc hẹn cụ thể và có thông tin về thời gian nhắc,
 * loại nhắc và trạng thái đã kích hoạt hay chưa.
 *
 * @property id ID tự động tăng, là khóa chính của bảng.
 * @property appointmentId ID của cuộc hẹn được liên kết (khóa ngoại đến bảng AppointmentPlus).
 * @property reminderTime Thời gian lời nhắc sẽ được kích hoạt (milliseconds).
 * @property minusBefore Số phút trước cuộc hẹn mà lời nhắc được bật.
 * @property reminderType Loại lời nhắc (ví dụ: thông báo hoặc chuông báo).
 * @property isTriggered Trạng thái cho biết lời nhắc đã được kích hoạt hay chưa.
 * @property createAt Thời điểm tạo lời nhắc (tính bằng milliseconds).
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 27.05.2025
 */
@Entity(
    tableName = "appointment_reminders",
    foreignKeys = [
        ForeignKey(
            entity = AppointmentPlus::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("appointmentId"),Index("reminderTime")]
)
data class AppointmentReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "appointment_id")
    val appointmentId: Int,
    @ColumnInfo(name = "reminder_time")
    val reminderTime:Long,
    @ColumnInfo(name = "minus_before")
    val minusBefore :Int,
    @ColumnInfo(name = "reminder_type")
    val reminderType: ReminderType = ReminderType.NOTIFICATION,
    @ColumnInfo(name = "is_triggered")
    val isTriggered:Boolean = false,
    @ColumnInfo(name = "created_at")
    val createAt: Long = System.currentTimeMillis()
)
