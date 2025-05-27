package com.nhathuy.nextmeet.model

/**
 * Đây là enum class của hoạt động của thông báo
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 27.05.2025
 */
enum class NotificationAction {
    OPEN_APPOINTMENT, // mở cuộn hẹn
    OPEN_NOTE, // mở note
    START_NAVIGATION, // bat đầu dẫn đường
    CALL_CONTACT,//gọi liên hệ
    SNOOZE // báo lại sau
}