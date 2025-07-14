package com.nhathuy.nextmeet.utils

import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus

class AppointmentStatusManager {
    companion object {
        const val PREPARATION_TIME_MINUTES = 30
        const val MISSED_THRESHOLD_MINUTES = 15
    }

    /**
     * Tính toán status mới dựa trên thời gian và trạng thái hiện tại
     */
    fun calculateNewStatus(
        appointment: AppointmentPlus,
        currentTime: Long = System.currentTimeMillis(),
        hasStartedNavigation: Boolean = false
    ): AppointmentStatus {
        val currentStatus = appointment.status
        val startTime = appointment.startDateTime
        val endTime = appointment.endDateTime
        val travelTime = appointment.travelTimeMinutes

        // thời gian nên bắt đầu chuẩn bị
        val preparationStartTime  = startTime  - (travelTime + PREPARATION_TIME_MINUTES) * 60 * 1000

        // Thời gian nên bắt đầu di chuyển (startTime - travelTime)
        val travelStartTime = startTime - travelTime * 60 * 1000

        // Thời gian được coi là missed (startTime + threshold)
        val missedThresholdTime = startTime + MISSED_THRESHOLD_MINUTES * 60 * 1000

        return when {
            // Đã bị hủy hoặc completed thì không thay đổi
            currentStatus.isFinished() -> currentStatus

            // Đã quá thời gian missed threshold mà chưa đến
            currentTime > missedThresholdTime &&
                    currentStatus in listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.PREPARING,
                AppointmentStatus.TRAVELLING, AppointmentStatus.DELAYED) -> {
                AppointmentStatus.MISSED
            }

            // Cuộc hẹn đã kết thúc
            currentTime > endTime && currentStatus == AppointmentStatus.IN_PROCESS -> {
                AppointmentStatus.COMPLETED
            }
            // Cuộc hẹn đang diễn ra
            currentTime >= startTime && currentTime <= endTime -> {
                when (currentStatus) {
                    AppointmentStatus.SCHEDULED,
                    AppointmentStatus.PREPARING,
                    AppointmentStatus.TRAVELLING,
                    AppointmentStatus.DELAYED -> AppointmentStatus.IN_PROCESS
                    else -> currentStatus
                }
            }

            // User đã bắt đầu navigation
            hasStartedNavigation && currentStatus != AppointmentStatus.TRAVELLING -> {
                AppointmentStatus.TRAVELLING
            }

            // Đã đến thời gian di chuyển
            currentTime >= travelStartTime && currentTime < startTime -> {
                when (currentStatus) {
                    AppointmentStatus.SCHEDULED,
                    AppointmentStatus.PREPARING -> {
                        if (hasStartedNavigation) AppointmentStatus.TRAVELLING
                        else AppointmentStatus.DELAYED
                    }
                    else -> currentStatus
                }
            }

            // Đã đến thời gian chuẩn bị
            currentTime >= preparationStartTime && currentTime < travelStartTime -> {
                when (currentStatus) {
                    AppointmentStatus.SCHEDULED -> AppointmentStatus.PREPARING
                    else -> currentStatus
                }
            }

            else -> currentStatus
        }
    }

    /**
     * Kiểm tra xem có nên cập nhật status hay không
     */
    fun shouldUpdateStatus(currentStatus: AppointmentStatus, newStatus: AppointmentStatus): Boolean {
        return currentStatus != newStatus && currentStatus.canTransitionTo(newStatus)
    }

    /**
     * Lấy thông điệp mô tả cho status transition
     */
    fun getStatusTransitionMessage(oldStatus: AppointmentStatus, newStatus: AppointmentStatus): String {
        return when (newStatus) {
            AppointmentStatus.PREPARING -> "Đã đến lúc chuẩn bị cho cuộc hẹn"
            AppointmentStatus.TRAVELLING -> "Đã đến lúc khởi hành đến cuộc hẹn"
            AppointmentStatus.IN_PROCESS -> "Cuộc hẹn đã bắt đầu"
            AppointmentStatus.COMPLETED -> "Cuộc hẹn đã hoàn thành"
            AppointmentStatus.DELAYED -> "Cuộc hẹn có thể bị trễ"
            AppointmentStatus.MISSED -> "Đã bỏ lỡ cuộc hẹn"
            else -> "Trạng thái cuộc hẹn đã được cập nhật"
        }
    }

    /**
     * Lấy màu sắc cho status
     */
    fun getStatusColor(status: AppointmentStatus): String {
        return when (status) {
            AppointmentStatus.SCHEDULED -> "#2196F3" // Blue
            AppointmentStatus.PREPARING -> "#FF9800" // Orange
            AppointmentStatus.TRAVELLING -> "#9C27B0" // Purple
            AppointmentStatus.IN_PROCESS -> "#4CAF50" // Green
            AppointmentStatus.COMPLETED -> "#8BC34A" // Light Green
            AppointmentStatus.DELAYED -> "#F44336" // Red
            AppointmentStatus.CANCELLED -> "#757575" // Gray
            AppointmentStatus.MISSED -> "#D32F2F" // Dark Red
        }
    }

    /**
     * Lấy icon cho status
     */
    fun getStatusIcon(status: AppointmentStatus): Int {
        return when (status) {
            AppointmentStatus.SCHEDULED -> R.drawable.ic_schedule
            AppointmentStatus.PREPARING -> R.drawable.ic_preparation
            AppointmentStatus.TRAVELLING -> R.drawable.ic_directions
            AppointmentStatus.IN_PROCESS -> R.drawable.ic_meeting
            AppointmentStatus.COMPLETED -> R.drawable.ic_check_circle
            AppointmentStatus.DELAYED -> R.drawable.ic_warning
            AppointmentStatus.CANCELLED -> R.drawable.ic_cancel
            AppointmentStatus.MISSED -> R.drawable.ic_error
        }
    }

}