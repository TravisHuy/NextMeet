package com.nhathuy.customermanagementapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity lưu lich sử các thông báo (báo thức) đã được hiện thị cho người dùng.
 *
 * Dùng để ghi lại mỗi lần hệ thống hiển thị một thông báo nhắc nhở cuoo hen den, cung vơi hanh dong phan hoi người dùng
 *
 * @property id Khóa chính tự động tăng
 * @property appointmentId ID của cuộc hẹn liên quan đến thông báo
 * @property time Giờ hiển thị thông báo (định dạng: "HH:mm")
 * @property date Ngày hiển thị thông báo (định dạng: "yyyy-MM-dd")
 * @property notes Ghi chú kèm theo thông báo (ví dụ: nội dung nhắc nhở)
 * @property wasDisplayed Cờ đánh dấu thông báo đã được hiển thị (true/false)
 * @property userAction Hành động của người dùng khi nhận thông báo (VD: "Dismissed", "Snoozed")
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 16.05.2025
 */
@Entity(tableName = "alarm_history")
data class AlarmHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "appointment_id")
    val appointmentId: Int,
    @ColumnInfo(name = "time")
    val time: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "notes")
    val notes: String,
    @ColumnInfo(name = "was_displayed")
    val wasDisplayed: Boolean = false,
    @ColumnInfo(name = "user_action")
    val userAction: String? = null
)