package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Data class đại diện cho một cuộc hẹn chi tiết, được kết hợp từ thông tin cuộc hẹn gốc và các dữ liệu bổ sung.
 *
 * @property id ID tự tăng của cuộc hẹn.
 * @property userId ID của người dùng sở hữu cuộc hẹn này (khóa ngoại đến bảng User).
 * @property contactId ID của liên hệ liên quan đến cuộc hẹn.
 * @property title Tiêu đề của cuộc hẹn.
 * @property description Mô tả chi tiết về cuộc hẹn.
 * @property startDateTime Thời gian bắt đầu cuộc hẹn (đơn vị: milliseconds).
 * @property endDateTime Thời gian kết thúc cuộc hẹn (đơn vị: milliseconds).
 * @property location Địa điểm diễn ra cuộc hẹn.
 * @property latitude Vĩ độ của địa điểm (có thể null nếu chưa xác định).
 * @property longitude Kinh độ của địa điểm (có thể null nếu chưa xác định).
 * @property status Trạng thái hiện tại của cuộc hẹn (ví dụ: đã lên lịch, đã hoàn thành...).
 * @property color Màu sắc đại diện cho cuộc hẹn (dạng mã màu hex, mặc định là xanh dương).
 * @property travelTimeMinutes Thời gian di chuyển ước tính tới địa điểm (tính bằng phút).
 * @property navigationStarted Cờ đánh dấu xem người dùng đã bắt đầu điều hướng đến cuộc hẹn hay chưa.
 * @property createdAt Thời điểm tạo cuộc hẹn (tính bằng milliseconds, mặc định là thời gian hiện tại).
 * @property updateAt Thời điểm cập nhật cuộc hẹn gần nhất (tính bằng milliseconds).
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 27.05.2025
 */

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("userId"),
        Index("contactId"),
        Index("startDateTime"),
        Index("status")
    ]
)
data class AppointmentPlus(
    val id: Int,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "contact_id")
    val contactId: Int,
    @ColumnInfo(name = "title")
    val title: String = "",
    @ColumnInfo(name = "description")
    val description: String = "",
    @ColumnInfo(name = "start_date_time")
    val startDateTime: Long,
    @ColumnInfo(name = "end_date_time")
    val endDateTime: Long,
    @ColumnInfo(name = "location")
    val location: String = "",
    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,
    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,
    @ColumnInfo(name = "status")
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    @ColumnInfo(name = "color")
    val color: String = "#2196F3",
    @ColumnInfo(name = "travel_time_minutes")
    val travelTimeMinutes: Int = 0,
    @ColumnInfo(name = "navigation_started")
    val navigationStarted: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updateAt: Long = System.currentTimeMillis()
)
