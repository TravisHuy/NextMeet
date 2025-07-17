package com.nhathuy.nextmeet.resource

import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus

/**
 * Sealed class đại diện cho các trạng thái UI của cuộc hẹn.
 * Được sử dụng để quản lý các trạng thái khác nhau trong quá trình
 * tương tác với dữ liệu cuộc hẹn.
 *
 * @author TravisHuy(Ho Nhat Huy)
 * @since 06.10.2025
 * @version 2.0
 */
sealed class AppointmentUiState {

    /**
     * Trạng thái ban đầu, chưa có thao tác nào được thực hiện.
     */
    object Idle : AppointmentUiState()

    /**
     * Trạng thái đang tải dữ liệu.
     */
    object Loading : AppointmentUiState()

    /**
     * Trạng thái cuộc hẹn đã được tạo thành công.
     * @param appointmentId ID của cuộc hẹn vừa được tạo
     * @param message Thông báo thành công
     */
    data class AppointmentCreated(val appointmentId: Long, val message: String) : AppointmentUiState()

    /**
     * Trạng thái danh sách cuộc hẹn đã được tải.
     * @param appointments Danh sách cuộc hẹn
     */
    data class AppointmentsLoaded(val appointments: List<AppointmentPlus>) : AppointmentUiState()

    /**
     * Trạng thái một cuộc hẹn cụ thể đã được tải.
     * @param appointment Cuộc hẹn được tải
     */
    data class AppointmentLoaded(val appointment: AppointmentPlus) : AppointmentUiState()

    /**
     * Trạng thái cuộc hẹn đã được cập nhật.
     * @param message Thông báo cập nhật thành công
     */
    data class AppointmentUpdated(val message: String) : AppointmentUiState()

    /**
     * Trạng thái cuộc hẹn đã được xóa.
     * @param message Thông báo xóa thành công
     */
    data class AppointmentDeleted(val message: String) : AppointmentUiState()

    /**
     * Trạng thái pin/unpin cuộc hẹn đã được toggle.
     * @param isPinned Trạng thái pin hiện tại
     * @param message Thông báo toggle thành công
     */
    data class PinToggled(val isPinned: Boolean, val message: String) : AppointmentUiState()

    /**
     * Trạng thái của cuộc hẹn đã được cập nhật.
     * @param status Trạng thái mới của cuộc hẹn
     * @param message Thông báo cập nhật thành công
     */
    data class StatusUpdated(val status: AppointmentStatus, val message: String) : AppointmentUiState()

    /**
     * Navigation hủy
     */
    data class NavigationCancelled(val message: String) : AppointmentUiState()
    /**
     * Trạng thái đã bắt đầu điều hướng.
     * @param message Thông báo bắt đầu điều hướng
     */
    data class NavigationStarted(val message: String) : AppointmentUiState()

    /**
     * Trạng thái tìm kiếm cuộc hẹn
     * @param query Từ khóa tìm kiếm
     * @param results Kết quả tìm kiếm
     */
    data class SearchResults(
        val query: String,
        val results: List<AppointmentPlus>
    ) : AppointmentUiState()

    /**
     * Trạng thái không tìm thấy kết quả
     * @param query Từ khóa tìm kiếm
     */
    data class NoSearchResults(
        val query: String
    ) : AppointmentUiState()



    /**
     * Trạng thái có lỗi xảy ra.
     * @param message Thông báo lỗi
     */
    data class Error(val message: String) : AppointmentUiState()

    /**
     * Trạng thái cập nhật thông tin điều hướng.
     */
    data class NavigationStatusUpdated(
        val isNavigating: Boolean,
        val message: String
    ) : AppointmentUiState()

    /**
     * Trạng thái gợi ý thay đổi trạng thái cuộc hẹn.
     * @param currentStatus Trạng thái hiện tại của cuộc hẹn
     * @param suggestedStatus Trạng thái được gợi ý
     * @param reason Lý do gợi ý
     */
    data class StatusSuggestion(
        val currentStatus: AppointmentStatus,
        val suggestedStatus: AppointmentStatus,
        val reason: String
    ) : AppointmentUiState()

    /**
     * Trạng thái xung đột khi cố gắng thay đổi trạng thái cuộc hẹn.
     * @param requestedStatus Trạng thái được yêu cầu
     * @param suggestedStatus Trạng thái được gợi ý
     * @param reason Lý do xung đột
     */
    data class StatusConflict(
        val requestedStatus: AppointmentStatus,
        val suggestedStatus: AppointmentStatus,
        val reason: String
    ) : AppointmentUiState()

    /**
     * Trạng thái hoàn thành điều hướng.
     * @param completed Trạng thái hoàn thành
     * @param message Thông báo hoàn thành
     */
    data class NavigationCompleted(
        val completed: Boolean,
        val message: String
    ) : AppointmentUiState()
}