package com.nhathuy.nextmeet.model

/**
 * Đây là enum class cho cuộc hẹn với trạng thái ví dụ SCHEDULED
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 27/05/2025
 */
enum class AppointmentStatus(val displayName: String, val priority: Int) {
    SCHEDULED("Đã lên lịch", 1),
    PREPARING("Đang chuẩn bị", 2),
    TRAVELLING("Đang di chuyển", 3),
    IN_PROGRESS("Đang diễn ra", 4),
    COMPLETED("Đã hoàn thành", 5),
    DELAYED("Bị trễ", 6),
    CANCELLED("Đã hủy", 7),
    MISSED("Đã bỏ lỡ", 8);

    fun canTransitionTo(newStatus: AppointmentStatus): Boolean {
        return when (this) {
            SCHEDULED -> newStatus in listOf(
                PREPARING,
                TRAVELLING,
                IN_PROGRESS,
                DELAYED,
                CANCELLED,
                MISSED
            )

            PREPARING -> newStatus in listOf(TRAVELLING, IN_PROGRESS, DELAYED, CANCELLED, MISSED)
            TRAVELLING -> newStatus in listOf(IN_PROGRESS, DELAYED, CANCELLED, MISSED)
            DELAYED -> newStatus in listOf(TRAVELLING, IN_PROGRESS, CANCELLED, MISSED)
            IN_PROGRESS -> newStatus in listOf(COMPLETED, CANCELLED)
            COMPLETED -> false
            CANCELLED -> false
            MISSED -> false
        }
    }

    fun isActive(): Boolean = this in listOf(SCHEDULED, PREPARING, TRAVELLING, IN_PROGRESS, DELAYED)
    fun isFinished(): Boolean = this in listOf(COMPLETED, CANCELLED, MISSED)

    /**
     * Kiểm tra xem status này có được hiển thị trong danh sách chính không
     */
    fun shouldShowInMainList(): Boolean = when (this) {
        SCHEDULED, PREPARING, TRAVELLING, IN_PROGRESS, DELAYED -> true
        COMPLETED, CANCELLED, MISSED -> false
    }

    /**
     * Kiểm tra xem appointment có thể bắt đầu navigation không
     */
    fun canStartNavigation(): Boolean = when (this) {
        SCHEDULED, PREPARING, DELAYED -> true
        else -> false
    }

    /**
     * Kiểm tra xem có nên hiển thị button navigation không
     */
    fun shouldShowNavigationButton(): Boolean = when (this) {
        SCHEDULED, PREPARING, DELAYED -> true
        TRAVELLING -> true
        else -> false
    }
}