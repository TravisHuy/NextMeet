package com.nhathuy.nextmeet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.repository.AppointmentPlusRepository
import com.nhathuy.nextmeet.resource.AppointmentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentPlusViewModel @Inject constructor(
    private val appointmentRepository: AppointmentPlusRepository
) : ViewModel() {

    private val _appointmentUiState = MutableStateFlow<AppointmentUiState>(AppointmentUiState.Idle)
    val appointmentUiState: StateFlow<AppointmentUiState> = _appointmentUiState

    /**
     * Tạo cuộc hẹn mới
     */
    fun createAppointment(
        appointment: AppointmentPlus
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.createAppointment(
                    userId = appointment.userId,
                    contactId = appointment.contactId,
                    title = appointment.title,
                    description = appointment.description,
                    startDateTime = appointment.startDateTime,
                    endDateTime = appointment.endDateTime,
                    location = appointment.location,
                    latitude = appointment.latitude,
                    longitude = appointment.longitude,
                    status = appointment.status,
                    color = appointment.color,
                    travelTimeMinutes = appointment.travelTimeMinutes,
                    isPinned = appointment.isPinned
                )

                if (result.isSuccess) {
                    _appointmentUiState.value = AppointmentUiState.AppointmentCreated(
                        result.getOrThrow(),
                        "Cuộc hẹn đã được tạo thành công"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi không xác định"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi tạo cuộc hẹn"
                )
            }
        }
    }

    /**
     * Lấy tất cả cuộc hẹn của người dùng với các bộ lọc
     */
    fun getAllAppointments(
        userId: Int,
        searchQuery: String = "",
        showPinnedOnly: Boolean = false,
        status: AppointmentStatus? = null
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                appointmentRepository.getAllAppointmentsWithFilter(
                    userId, searchQuery, showPinnedOnly, status
                ).collect { appointments ->
                    _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(appointments)
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi tải danh sách cuộc hẹn"
                )
            }
        }
    }

    /**
     * Toggle pin/unpin cuộc hẹn
     */
    fun togglePin(appointmentId: Int) {
        viewModelScope.launch {
            try {
                appointmentRepository.togglePin(appointmentId)
                    .onSuccess { isPinned ->
                        val message = if (isPinned) "Đã pin cuộc hẹn" else "Đã bỏ pin cuộc hẹn"
                        _appointmentUiState.value = AppointmentUiState.PinToggled(isPinned, message)
                    }
                    .onFailure { error ->
                        _appointmentUiState.value = AppointmentUiState.Error(
                            error.message ?: "Lỗi khi pin cuộc hẹn"
                        )
                    }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi pin cuộc hẹn"
                )
            }
        }
    }

    /**
     * Cập nhật trạng thái cuộc hẹn
     */
    fun updateAppointmentStatus(appointmentId: Int, status: AppointmentStatus) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.updateAppointmentStatus(appointmentId, status)
                if (result.isSuccess) {
                    _appointmentUiState.value = AppointmentUiState.StatusUpdated(
                        status,
                        "Trạng thái cuộc hẹn đã được cập nhật"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi cập nhật trạng thái"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi cập nhật trạng thái cuộc hẹn"
                )
            }
        }
    }

    /**
     * Xóa cuộc hẹn theo ID
     */
    fun deleteAppointment(appointmentId: Int) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.deleteAppointment(appointmentId)
                if (result.isSuccess) {
                    _appointmentUiState.value = AppointmentUiState.AppointmentDeleted(
                        "Cuộc hẹn đã được xóa thành công"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi xóa cuộc hẹn"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi xóa cuộc hẹn"
                )
            }
        }
    }

    /**
     * Lấy cuộc hẹn theo ID
     */
    fun getAppointmentById(appointmentId: Int) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.getAppointmentById(appointmentId)
                if (result.isSuccess) {
                    _appointmentUiState.value = AppointmentUiState.AppointmentLoaded(
                        result.getOrThrow()
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi tải cuộc hẹn"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi tải cuộc hẹn"
                )
            }
        }
    }

    /**
     * Cập nhật thông tin cuộc hẹn
     */
    fun updateAppointment(appointment: AppointmentPlus) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.updateAppointment(appointment)
                if (result.isSuccess) {
                    _appointmentUiState.value = AppointmentUiState.AppointmentUpdated(
                        "Cuộc hẹn đã được cập nhật thành công"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi cập nhật cuộc hẹn"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi cập nhật cuộc hẹn"
                )
            }
        }
    }

    /**
     * Bắt đầu điều hướng đến cuộc hẹn
     */
    fun startNavigation(appointmentId: Int) {
        viewModelScope.launch {
            try {
                val result = appointmentRepository.startNavigation(appointmentId)
                if (result.isSuccess) {
                    _appointmentUiState.value = AppointmentUiState.NavigationStarted(
                        "Đã bắt đầu điều hướng"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi bắt đầu điều hướng"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi bắt đầu điều hướng"
                )
            }
        }
    }

    /**
     * Reset UI state về Idle
     */
    fun resetUiState() {
        _appointmentUiState.value = AppointmentUiState.Idle
    }
}