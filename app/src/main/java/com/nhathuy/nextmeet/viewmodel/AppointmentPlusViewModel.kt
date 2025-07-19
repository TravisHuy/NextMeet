package com.nhathuy.nextmeet.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.NavigationCheckResult
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.model.TimingInfo
import com.nhathuy.nextmeet.model.TransportMode
import com.nhathuy.nextmeet.repository.AppointmentPlusRepository
import com.nhathuy.nextmeet.repository.ContactRepository
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.utils.AppointmentStatusManager
import com.nhathuy.nextmeet.utils.NotificationManagerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AppointmentPlusViewModel @Inject constructor(
    private val appointmentRepository: AppointmentPlusRepository,
    private val contactRepository: ContactRepository,
    private val notificationManagerService: NotificationManagerService
) : ViewModel() {

    private val _appointmentUiState = MutableStateFlow<AppointmentUiState>(AppointmentUiState.Idle)
    val appointmentUiState: StateFlow<AppointmentUiState> = _appointmentUiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestion: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private var allAppointments: List<AppointmentPlus> = emptyList()

    // thông tin navigation session
    private var navigationStartTime: Long = 0
    private var navigationStartLocation: Location? = null

    private val statusManager = AppointmentStatusManager()
    private var statusUpdateJob: Job? = null
    private var isActive = false


    /**
     * Tạo cuộc hẹn mới
     */
    fun createAppointment(
        appointment: AppointmentPlus,
        shouldSetReminder: Boolean = false,
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                if (appointment.contactId == null) {
                    _appointmentUiState.value =
                        AppointmentUiState.Error("Vui lòng chọn liên hệ cho cuộc hẹn")
                    return@launch
                }
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
                    val createdAppointmentId = result.getOrThrow()

                    // hẹn cuộc hẹn
                    val appointmentWithId = appointment.copy(id = createdAppointmentId.toInt())
                    if (shouldSetReminder) {
                        scheduleAppointmentNotification(
                            appointmentWithId,
                            appointment.contactId
                        )
                    }

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
                    allAppointments = appointments
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

                    allAppointments = allAppointments.map { appointment ->
                        if (appointment.id == appointmentId) {
                            appointment.copy(status = status, updateAt = System.currentTimeMillis())
                        } else {
                            appointment
                        }
                    }

                    _appointmentUiState.value = AppointmentUiState.StatusUpdated(
                        status,
                        "Trạng thái cuộc hẹn đã được cập nhật"
                    )

                    _appointmentUiState.value =
                        AppointmentUiState.AppointmentsLoaded(allAppointments)
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
                // Hủy notifications đầu tiên
                cancelAppointmentNotification(appointmentId)

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
     * Lấy cuộc hẹn theo ID
     */
    suspend fun getAppointmentByIdSync(appointmentId: Int): Result<AppointmentPlus> {
        return try {
            appointmentRepository.getAppointmentById(appointmentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cập nhật cuộc hẹn
     */
    fun updateAppointment(
        appointment: AppointmentPlus,
        shouldSetReminder: Boolean = false,
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.updateAppointment(appointment)

                if (result.isSuccess) {
                    // Xóa notification cũ trước (nếu có)
                    cancelAppointmentNotification(appointment.id)

                    // Tạo notification mới nếu user chọn reminder
                    if (shouldSetReminder && appointment.contactId != null) {
                        scheduleAppointmentNotification(
                            appointment,
                            appointment.contactId
                        )
                    }

                    _appointmentUiState.value = AppointmentUiState.AppointmentUpdated(
                        "Cuộc hẹn đã được cập nhật thành công"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi không xác định"
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
     * Lên lịch thông báo cho cuộc hẹn
     */
    fun scheduleAppointmentNotification(
        appointment: AppointmentPlus,
        contactId: Int
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()

                if (appointment.startDateTime > now) {
                    // Lấy thông tin contact để hiển thị trong notification
                    val contactResult = contactRepository.getContactById(contactId)
                    val contactName = if (contactResult.isSuccess) {
                        contactResult.getOrNull()?.name ?: "Không rõ"
                    } else {
                        "Không rõ"
                    }

                    val success = notificationManagerService.scheduleAppointmentNotification(
                        userId = appointment.userId,
                        appointmentId = appointment.id,
                        title = appointment.title,
                        description = appointment.description ?: "",
                        appointmentTime = appointment.startDateTime,
                        location = appointment.location,
                        contactName = contactName
                    )

                    if (!success) {
                        // Log lỗi nhưng không fail toàn bộ process tạo appointment
                        Log.w(
                            "AppointmentViewModel",
                            "Không thể tạo notification cho cuộc hẹn ${appointment.id}"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Lỗi khi tạo notification", e)
            }
        }
    }


    /**
     * Hủy thông báo cho cuộc hẹn
     */
    private suspend fun cancelAppointmentNotification(appointmentId: Int) {
        try {
            // Xóa tất cả notification liên quan đến appointment này
            notificationManagerService.cancelNotificationsByRelatedId(
                appointmentId,
                NotificationType.APPOINTMENT_REMINDER
            )
        } catch (e: Exception) {
            Log.e("AppointmentViewModel", "Lỗi khi hủy notification", e)
        }
    }

    /**
     * Bắt đầu điều hướng đến cuộc hẹn - cập nhật status
     */
    /**
     * Bắt đầu navigation với location tracking
     */
    fun startNavigationToAppointment(appointmentId: Int, startLocation: Location? = null) {
        viewModelScope.launch {
            try {
                // Lưu thông tin navigation session
                navigationStartTime = System.currentTimeMillis()
                navigationStartLocation = startLocation

                // Cập nhật navigation status
                val navResult = appointmentRepository.updateNavigationStatus(appointmentId, true)

                if (navResult.isSuccess) {
                    // Lấy appointment hiện tại để kiểm tra status
                    val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)

                    if (appointmentResult.isSuccess) {
                        val appointment = appointmentResult.getOrThrow()

                        // Tính toán status mới
                        val newStatus = statusManager.calculateNewStatus(
                            appointment = appointment.copy(navigationStarted = true),
                            currentTime = System.currentTimeMillis(),
                            hasStartedNavigation = true
                        )

                        // Cập nhật status nếu cần
                        if (statusManager.shouldUpdateStatus(appointment.status, newStatus)) {
                            val statusResult = appointmentRepository.updateAppointmentStatus(
                                appointmentId,
                                newStatus
                            )

                            if (statusResult.isSuccess) {
                                // Update local cache
                                updateLocalCache(appointmentId) { appt ->
                                    appt.copy(
                                        status = newStatus,
                                        navigationStarted = true,
                                        updateAt = System.currentTimeMillis()
                                    )
                                }

                                _appointmentUiState.value = AppointmentUiState.NavigationStarted(
                                    statusManager.getStatusTransitionMessage(
                                        appointment.status,
                                        newStatus
                                    )
                                )

                                Log.d(
                                    "AppointmentViewModel",
                                    "Started navigation: ${appointment.status} -> $newStatus"
                                )
                            } else {
                                _appointmentUiState.value = AppointmentUiState.Error(
                                    "Lỗi khi cập nhật trạng thái: ${statusResult.exceptionOrNull()?.message}"
                                )
                            }
                        } else {
                            // Chỉ cập nhật navigation started
                            updateLocalCache(appointmentId) { appt ->
                                appt.copy(
                                    navigationStarted = true,
                                    updateAt = System.currentTimeMillis()
                                )
                            }

                            _appointmentUiState.value = AppointmentUiState.NavigationStarted(
                                "Đã bắt đầu điều hướng"
                            )
                        }
                    }
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        "Lỗi khi bắt đầu điều hướng: ${navResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error starting navigation", e)
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi bắt đầu điều hướng"
                )
            }
        }
    }


    /**
     * hủy navigation với logic revert status
     */
    fun cancelNavigationWithMode(
        appointmentId: Int,
        currentLocation: Location? = null,
        transportMode: TransportMode
    ) {
        viewModelScope.launch {
            try {
                val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)

                if (appointmentResult.isSuccess) {
                    val appointment = appointmentResult.getOrThrow()

                    // Luôn set navigation_started = false trước
                    val navResult =
                        appointmentRepository.updateNavigationStatus(appointmentId, false)
                    if (navResult.isFailure) {
                        _appointmentUiState.value =
                            AppointmentUiState.Error("Lỗi khi hủy navigation")
                        return@launch
                    }

                    // Gọi AppointmentStatusManager với transport mode
                    val cancelAction = statusManager.handleNavigationCancellation(
                        appointment = appointment,
                        navigationStartTime = navigationStartTime,
                        currentLocation = currentLocation,
                        startLocation = navigationStartLocation,
                        transportMode = transportMode // Pass transport mode
                    )

                    if (cancelAction.shouldUpdateStatus && cancelAction.newStatus != appointment.status) {
                        val statusResult = appointmentRepository.updateAppointmentStatus(
                            appointmentId,
                            cancelAction.newStatus
                        )

                        if (statusResult.isSuccess) {
                            updateLocalCache(appointmentId) { appt ->
                                appt.copy(
                                    status = cancelAction.newStatus,
                                    navigationStarted = false,
                                    updateAt = System.currentTimeMillis()
                                )
                            }

                            _appointmentUiState.value =
                                AppointmentUiState.NavigationCancelled(cancelAction.message)
                        }
                    } else {
                        updateLocalCache(appointmentId) { appt ->
                            appt.copy(
                                navigationStarted = false,
                                updateAt = System.currentTimeMillis()
                            )
                        }

                        _appointmentUiState.value =
                            AppointmentUiState.NavigationCancelled(cancelAction.message)
                    }

                    resetNavigationSession()
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error cancelling navigation with mode", e)
                _appointmentUiState.value = AppointmentUiState.Error("Lỗi khi hủy điều hướng")
            }
        }
    }

    // Giữ nguyên method cancelNavigation cũ để backward compatibility
    fun cancelNavigation(appointmentId: Int, currentLocation: Location? = null) {
        // Default to DRIVING if no transport mode specified
        cancelNavigationWithMode(appointmentId, currentLocation, TransportMode.DRIVING)
    }

    private fun resetNavigationSession() {
        navigationStartTime = 0
        navigationStartLocation = null
    }

    /**
     * Bắt đầu điều hướng đến cuộc hẹn
     */
    fun updateNavigationStatus(appointmentId: Int, hasStartedNavigation: Boolean) {
        viewModelScope.launch {
            try {
                val result = appointmentRepository.updateNavigationStatus(
                    appointmentId,
                    hasStartedNavigation
                )
                if (result.isSuccess) {
                    // Tự động cập nhật status dựa trên navigation state
                    updateAppointmentBasedOnTime(appointmentId)

                    Log.d(
                        "AppointmentViewModel",
                        "Navigation status updated: hasStarted = $hasStartedNavigation"
                    )
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating navigation status", e)
            }
        }
    }

    /**
     * tự động cập nhật trạng thái dựa trên thời gian hiện tại
     */
    fun updateAppointmentBasedOnTime(appointmentId: Int) {
        viewModelScope.launch {
            try {
                val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
                if (appointmentResult.isSuccess) {
                    val appointment = appointmentResult.getOrThrow()
                    val currentTime = System.currentTimeMillis()

                    Log.d("AppointmentViewModel", "Checking status for appointment $appointmentId")
                    Log.d("AppointmentViewModel", "Current status: ${appointment.status}")
                    Log.d(
                        "AppointmentViewModel",
                        "Navigation started: ${appointment.navigationStarted}"
                    )
                    Log.d("AppointmentViewModel", "Current time: $currentTime")
                    Log.d("AppointmentViewModel", "Start time: ${appointment.startDateTime}")

                    val newStatus = statusManager.calculateNewStatus(
                        appointment = appointment,
                        currentTime = currentTime,
                        hasStartedNavigation = appointment.navigationStarted
                    )

                    if (statusManager.shouldUpdateStatus(appointment.status, newStatus)) {
                        Log.d(
                            "AppointmentViewModel",
                            "Updating status: ${appointment.status} -> $newStatus"
                        )

                        updateAppointmentStatus(appointmentId, newStatus)

                        // Gửi notification quan trọng nếu cần
                        if (shouldNotifyStatusChange(appointment.status, newStatus)) {
                            sendStatusChangeNotification(appointment, newStatus)
                        }
                    } else {
                        Log.d(
                            "AppointmentViewModel",
                            "No status change needed: ${appointment.status}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating appointment status", e)
            }
        }
    }

    private fun shouldNotifyStatusChange(
        oldStatus: AppointmentStatus,
        newStatus: AppointmentStatus
    ): Boolean {
        return when (newStatus) {
            AppointmentStatus.DELAYED,
            AppointmentStatus.MISSED -> true

            AppointmentStatus.IN_PROGRESS -> oldStatus != AppointmentStatus.IN_PROGRESS
            else -> false
        }
    }

    private suspend fun sendStatusChangeNotification(
        appointment: AppointmentPlus,
        newStatus: AppointmentStatus
    ) {
        try {
            val message = statusManager.getStatusTransitionMessage(appointment.status, newStatus)

            notificationManagerService.sendSimpleNotification(
                appointmentId = appointment.id,
                title = "Cập nhật cuộc hẹn",
                message = "${appointment.title}: $message"
            )

            Log.d("AppointmentViewModel", "Sent status change notification: $message")
        } catch (e: Exception) {
            Log.e("AppointmentViewModel", "Error sending status notification", e)
        }
    }

    /**
     *  cập nhật thời gian di chuyển cho cuộn hẹn
     */
    fun updateTravelTime(appointmentId: Int, travelTimeMinutes: Int) {
        viewModelScope.launch {
            try {
                Log.d(
                    "AppointmentViewModel",
                    "Updating travel time for appointment $appointmentId to $travelTimeMinutes minutes"
                )

                allAppointments = allAppointments.map { appointment ->
                    if (appointment.id == appointmentId) {
                        appointment.copy(
                            travelTimeMinutes = travelTimeMinutes,
                            updateAt = System.currentTimeMillis()
                        )
                    } else {
                        appointment
                    }
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating travel time", e)
            }
        }
    }

    /**
     *
     */
    fun updateAppointmentWithRouteInfo(
        appointmentId: Int,
        travelTimeMinutes: Int,
        distance: Double,
        transportMode: TransportMode
    ) {
        viewModelScope.launch {
            try {
                val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
                if (appointmentResult.isSuccess) {
                    val appointment = appointmentResult.getOrThrow()

                    // Cập nhật appointment với travel time mới
                    val updatedAppointment = appointment.copy(
                        travelTimeMinutes = travelTimeMinutes,
                        updateAt = System.currentTimeMillis()
                    )

                    val result = appointmentRepository.updateAppointment(updatedAppointment)
                    if (result.isSuccess) {
                        Log.d(
                            "AppointmentViewModel",
                            "Updated appointment $appointmentId: travel time = $travelTimeMinutes min, distance = $distance km"
                        )

                        // Cập nhật local cache
                        allAppointments = allAppointments.map { appt ->
                            if (appt.id == appointmentId) updatedAppointment else appt
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating appointment with route info", e)
            }
        }
    }

    /**
     * Lấy gợi ý tìm kiếm
     */

    /**
     * Reset UI state về Idle
     */
    fun resetUiState() {
        _appointmentUiState.value = AppointmentUiState.Idle
    }

    /**
     * Bắt đầu theo dõi trạng thái cuộc hẹn
     */
    fun startStatus(userId: Int) {
        if (isActive) return

        isActive = true
        statusUpdateJob = viewModelScope.launch {
            while (isActive) {
                try {
                    updateActiveAppointments(userId)
                    delay(5 * 60 * 1000) // Cập nhật mỗi 5 phút
                } catch (e: Exception) {
                    Log.e("StatusMonitor", "Error: ${e.message}")
                    delay(10 * 60 * 1000)
                }
            }
        }
    }

    /**
     * Dừng theo dõi
     */
    fun stopStatus() {
        isActive = false
        statusUpdateJob?.cancel()
        statusUpdateJob = null
    }

    private suspend fun updateActiveAppointments(userId: Int) {
        try {
            val activeAppointments = appointmentRepository.getAllActiveAppointments(userId)
            var updatedCount = 0

            activeAppointments.forEach { appointment ->
                val newStatus = statusManager.calculateNewStatus(
                    appointment = appointment,
                    currentTime = System.currentTimeMillis(),
                    hasStartedNavigation = appointment.navigationStarted
                )

                if (statusManager.shouldUpdateStatus(appointment.status, newStatus)) {
                    val result =
                        appointmentRepository.updateAppointmentStatus(appointment.id, newStatus)
                    if (result.isSuccess) {
                        updatedCount++

                        if (shouldNotifyUser(newStatus)) {
                            sendImportantNotification(appointment, newStatus)
                        }

                        updateLocalCache(appointment.id, newStatus)
                    }
                }
            }
            if (updatedCount > 0) {
                Log.d("StatusMonitor", "Quietly updated $updatedCount appointments")
            }
        } catch (e: Exception) {
            Log.e("StatusMonitor", "Error updating appointments", e)
        }
    }

    private fun shouldNotifyUser(status: AppointmentStatus): Boolean {
        return when (status) {
            AppointmentStatus.MISSED -> true
            AppointmentStatus.DELAYED -> true
            else -> false
        }
    }

    private suspend fun sendImportantNotification(
        appointment: AppointmentPlus,
        newStatus: AppointmentStatus
    ) {
        try {
            val (title, message) = when (newStatus) {
                AppointmentStatus.DELAYED -> {
                    "⚠️ Sắp trễ cuộc hẹn" to "Cuộc hẹn '${appointment.title}' sắp diễn ra. Bạn nên khởi hành ngay!"
                }

                AppointmentStatus.MISSED -> {
                    "❌ Đã bỏ lỡ cuộc hẹn" to "Cuộc hẹn '${appointment.title}' đã bắt đầu và bạn chưa đến."
                }

                else -> return
            }
            notificationManagerService.sendSimpleNotification(
                appointmentId = appointment.id,
                title = title,
                message = message
            )
            Log.d("StatusMonitor", "Sent important notification: $title")

        } catch (e: Exception) {
            Log.e("StatusMonitor", "Error sending notification", e)
        }
    }

    private fun updateLocalCache(appointmentId: Int, newStatus: AppointmentStatus) {
        allAppointments = allAppointments.map { appointment ->
            if (appointment.id == appointmentId) {
                appointment.copy(status = newStatus, updateAt = System.currentTimeMillis())
            } else {
                appointment
            }
        }
    }

    private fun updateLocalCache(
        appointmentId: Int,
        updateFunction: (AppointmentPlus) -> AppointmentPlus
    ) {
        allAppointments = allAppointments.map { appointment ->
            if (appointment.id == appointmentId) {
                updateFunction(appointment)
            } else {
                appointment
            }
        }
    }

    //kiểm tra cuộc hẹn với trạng thái
    fun checkAppointmentStatus(appointmentId: Int) {
        viewModelScope.launch {
            try {
                val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
                if (appointmentResult.isSuccess) {
                    val appointment = appointmentResult.getOrThrow()
                    val newStatus = statusManager.calculateNewStatus(
                        appointment = appointment,
                        currentTime = System.currentTimeMillis(),
                        hasStartedNavigation = appointment.navigationStarted
                    )

                    if (statusManager.shouldUpdateStatus(appointment.status, newStatus)) {
                        appointmentRepository.updateAppointmentStatus(appointment.id, newStatus)
                        updateLocalCache(appointment.id, newStatus)

                        Log.d(
                            "StatusMonitor",
                            "Updated appointment $appointmentId: ${appointment.status} -> $newStatus"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("StatusMonitor", "Error checking appointment status", e)
            }
        }
    }

    /**
     * Kiểm tra navigation timing cho appointment - Version với Flow
     */
    suspend fun checkNavigationTiming(appointmentId: Int): NavigationCheckResult? {
        return try {
            val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
            if (appointmentResult.isSuccess) {
                val appointment = appointmentResult.getOrThrow()
                statusManager.canStartNavigationNow(appointment)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AppointmentViewModel", "Error checking navigation timing", e)
            null
        }
    }

    /**
     * Lấy thông tin timing cho appointment
     */
    suspend fun getAppointmentTimingInfo(appointmentId: Int): TimingInfo? {
        return try {
            val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
            if (appointmentResult.isSuccess) {
                val appointment = appointmentResult.getOrThrow()
                statusManager.getTimingInfo(appointment)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AppointmentViewModel", "Error getting timing info", e)
            null
        }
    }

    /**
     * Lấy tất cả cuộc hẹn với filter status cụ thể
     */
    fun getAllAppointmentsWithStatusFilter(
        userId: Int,
        searchQuery: String = "",
        showPinnedOnly: Boolean = false,
        allowedStatuses: List<AppointmentStatus>
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                appointmentRepository.getAllAppointmentsWithStatusFilter(
                    userId, searchQuery, showPinnedOnly, allowedStatuses
                ).collect { appointments ->
                    allAppointments = appointments
                    _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(appointments)
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi tải danh sách cuộc hẹn"
                )
            }
        }
    }

    fun cancelAppointment(appointmentId: Int) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {

                // lấy appointment hiện tại
                val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
                if (appointmentResult.isFailure) {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        appointmentResult.exceptionOrNull()?.message ?: "Không tìm thấy cuộc hẹn"
                    )
                    return@launch
                }

                val appointment = appointmentResult.getOrThrow()

                if (!appointment.status.canTransitionTo(AppointmentStatus.CANCELLED)) {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        "Không thể hủy cuộc hẹn ở trạng thái ${appointment.status.displayName}"
                    )
                    return@launch
                }

                // hủy tất cả notifications liên quan
                cancelAppointmentNotification(appointmentId)

                // Nếu đang navigation thì dừng navigation trước
                if (appointment.navigationStarted) {
                    val navResult =
                        appointmentRepository.updateNavigationStatus(appointmentId, false)
                    if (navResult.isFailure) {
                        Log.w(
                            "AppointmentViewModel",
                            "Failed to stop navigation for cancelled appointment"
                        )
                    }
                    resetNavigationSession()
                }

                // Cập nhật status thành CANCELLED
                val result = appointmentRepository.updateAppointmentStatus(
                    appointmentId,
                    AppointmentStatus.CANCELLED
                )

                if (result.isSuccess) {
                    // Update local cache
                    updateLocalCache(appointmentId) { appt ->
                        appt.copy(
                            status = AppointmentStatus.CANCELLED,
                            navigationStarted = false,
                            updateAt = System.currentTimeMillis()
                        )
                    }

                    // Gửi notification thông báo hủy nếu cần
                    sendCancellationNotification(appointment)

                    _appointmentUiState.value = AppointmentUiState.AppointmentCancelled(
                        "Cuộc hẹn '${appointment.title}' đã được hủy thành công"
                    )

                    Log.d(
                        "AppointmentViewModel",
                        "Cancelled appointment $appointmentId: ${appointment.status} -> CANCELLED"
                    )

                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi hủy cuộc hẹn"
                    )
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error cancelling appointment", e)
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi hủy cuộc hẹn"
                )
            }
        }
    }
    /**
     * Gửi notification thông báo hủy cuộc hẹn
     */
    private suspend fun sendCancellationNotification(
        appointment: AppointmentPlus
    ) {
        try {
            val message = buildString {
                append("Cuộc hẹn '${appointment.title}' đã được hủy/n")
                append("Nội dung: ${appointment.description ?: "Không có mô tả"}")
            }

            notificationManagerService.sendSimpleNotification(
                appointmentId = appointment.id,
                title = "🚫 Cuộc hẹn đã hủy",
                message = message
            )

            Log.d("AppointmentViewModel", "Sent cancellation notification")
        } catch (e: Exception) {
            Log.e("AppointmentViewModel", "Error sending cancellation notification", e)
        }
    }


    override fun onCleared() {
        super.onCleared()
        stopStatus()
        resetNavigationSession()
    }
}