package com.nhathuy.nextmeet.model

/**
 * Đây là enum class cho cuộc hẹn với trạng thái ví dụ SCHEDULED
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 27/05/2025
 */
enum class AppointmentStatus {
    SCHEDULED, // đã len lich
    IN_PROCESS,// Đang diễn ra
    COMPLETED, // hoàn thành
    CANCELLED // đã hủy
}