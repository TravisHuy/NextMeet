package com.nhathuy.nextmeet.model

/**
 * Đây là enum class của loại thông báo như nhắc cuộc hẹn, ghi chú..
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 27.05.2025
 */

enum class NotificationType {
    APPOINTMENT_REMINDER, // nhắc cuộc hẹn
    NOTE_REMINDER, // nhắc ghi chú
    LOCATION_REMINDER, // nhắc theo vị trí
    TRAVEL_TIME, // thời gian
}