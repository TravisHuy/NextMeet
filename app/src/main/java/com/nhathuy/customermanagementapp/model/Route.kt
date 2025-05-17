package com.nhathuy.customermanagementapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entity đại diện cho tuyến đường (Route) gắn với một cuộc hẹn cụ thể
 *
 * Mỗi Route chỉ chứa thông tin tuyến đường di chuyển như polyline , khoảng cách, thời gian di chuyển,
 * loại phương tiên và trạng thái ưu tiên.
 *
 * Route đươc liên kết với bản Appointment qua khóa ngoại appointmentId
 *
 * @property id Id duy nhất kết nối với bản Appointment qua khóa ngoại appointmentId.
 * @property appointmentId Khóa ngoại liên kết với cuộc hẹn
 * @property polyline Ma hoa tuyến đường hiển thị trên bản đồ
 * @property distance Khoảng cách tuyến đường (ví dụ 5Km)
 * @property duration Thoi gian ứng tính để hoàn thiện tuyến (ví dụ 10 phut)
 * @property travelMode phương thức di chuyển (lái xe , đi bộ, đi phà,..)
 * @property createAt thường điểm tạo tuyến đường (timestamp,milliseconds)
 * @property isPreferred Đánh dấu tuyến đường đươợc ưu tuyên hiển thị, chọn
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 16.05.2025
 */
@Entity(
    tableName = "routes",
    foreignKeys = [ForeignKey(
        entity = Appointment::class,
        parentColumns = ["id"],
        childColumns = ["appointmentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("appointmentId")]
)
data class Route(
    val id: Int = 0,
    val appointmentId: Int,
    val polyline: String,
    val distance: String,
    val duration: String,
    val travelMode: String,
    val createAt: Long,
    val isPreferred: Boolean = true
)