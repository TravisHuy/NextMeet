package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entity đại diện cho tuyến đường di chuyển liên quan đến một cuộc hẹn.
 *
 * Lưu thông tin về địa điểm bắt đầu và kết thúc, khoảng cách, thời gian di chuyển,
 * phương tiện di chuyển và liên kết với cuộc hẹn cụ thể trong bảng Appointment.
 *
 * @property id ID tự tăng, là khóa chính của tuyến đường.
 * @property appointmentId ID của cuộc hẹn liên quan (khóa ngoại đến bảng Appointment).
 * @property startAddress Địa chỉ bắt đầu của hành trình.
 * @property startLatitude Vĩ độ của điểm bắt đầu.
 * @property startLongitude Kinh độ của điểm bắt đầu.
 * @property endAddress Địa chỉ kết thúc của hành trình.
 * @property endLatitude Vĩ độ của điểm kết thúc.
 * @property endLongitude Kinh độ của điểm kết thúc.
 * @property distanceMetres Tổng quãng đường di chuyển (tính bằng mét).
 * @property durationMinutes Tổng thời gian di chuyển (tính bằng phút).
 * @property transportMode Phương tiện di chuyển (ví dụ: đi bộ, xe máy, ô tô).
 * @property createdAt Thời điểm tạo bản ghi tuyến đường (milliseconds).
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 27/05/2025
 */
@Entity(
    tableName = "travel_routes",
    foreignKeys = [ForeignKey(
        entity = AppointmentPlus::class,
        parentColumns = ["id"],
        childColumns = ["appointment_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("appointment_id")]
)
data class TravelRoute(
    val id: Int = 0,
    @ColumnInfo(name = "appointment_id")
    val appointmentId: Int,
    @ColumnInfo(name = "start_address")
    val startAddress: String,
    @ColumnInfo(name = "start_latitude")
    val startLatitude: Double,
    @ColumnInfo(name = "start_longitude")
    val startLongitude: Double,
    @ColumnInfo(name = "end_address")
    val endAddress: String,
    @ColumnInfo(name = "end_latitude")
    val endLatitude: Double,
    @ColumnInfo(name = "end_longitude")
    val endLongitude: Double,
    @ColumnInfo(name = "distance_metres")
    val distanceMetres: Int,
    @ColumnInfo(name = "distance_minutes")
    val durationMinutes: Int,
    @ColumnInfo(name = "transport_mode")
    val transportMode: TransportMode,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)