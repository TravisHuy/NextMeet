package com.nhathuy.nextmeet.model

/**
 * Lớp dữ liệu thống kê cho một địa điểm cụ thể.
 *
 * @property locationName Tên địa điểm.
 * @property appointmentCount Tổng số cuộc hẹn tại địa điểm này.
 * @property latitude Vĩ độ của địa điểm.
 * @property longitude Kinh độ của địa điểm.
 * @property upcomingCount Số lượng cuộc hẹn sắp tới tại địa điểm này. Mặc định là 0.
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 29.06.2025
 */
data class LocationStatistics(
    val locationName: String,
    val appointmentCount : Int,
    val latitude: Double,
    val longitude: Double,
    val upcomingCount: Int = 0,
)
