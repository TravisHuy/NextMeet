package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/**
 * Entity đại diện cho một thông báo của người dùng trong ứng dụng.
 *
 * Mỗi thông báo có thể liên quan đến một sự kiện như cuộc hẹn, lời nhắc hoặc hành động cụ thể.
 * Thông báo có thể được đặt lịch gửi, đánh dấu là đã đọc, đã gửi, và có thể dẫn đến hành động cụ thể trong app.
 *
 * @property id ID tự tăng, là khóa chính của bảng.
 * @property userId ID của người dùng sở hữu thông báo (khóa ngoại đến bảng User).
 * @property title Tiêu đề của thông báo.
 * @property message Nội dung chi tiết của thông báo.
 * @property notificationType Kiểu thông báo (ví dụ: CUỘC HẸN, NHẮC NHỞ, HỆ THỐNG, v.v.).
 * @property relatedId ID liên quan đến thông báo (ví dụ: id cuộc hẹn, id nhắc nhở,...).
 * @property scheduledTime Thời gian thông báo dự kiến được gửi (milliseconds).
 * @property isSent Cờ xác định thông báo đã được gửi đi hay chưa.
 * @property isRead Cờ xác định người dùng đã đọc thông báo hay chưa.
 * @property actionType Kiểu hành động liên kết với thông báo (nếu có), dùng để xử lý khi người dùng nhấn vào.
 * @property createdAt Thời điểm tạo thông báo.
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 27/05/2025
 */
@Entity(
    tableName = "notifications",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("userId"),
        Index("notificationType"),
        Index("isRead"),
        Index("scheduledTime")
    ]
)
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "notification_type")
    val notificationType: NotificationType,
    @ColumnInfo(name = "related_id")
    val relatedId: Int,
    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: Long,
    @ColumnInfo(name = "is_sent")
    val isSent: Boolean = false,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
    @ColumnInfo(name = "action_type")
    val actionType: NotificationAction? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
