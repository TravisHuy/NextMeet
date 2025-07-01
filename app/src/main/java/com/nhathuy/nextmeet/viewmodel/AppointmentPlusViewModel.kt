package com.nhathuy.nextmeet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.repository.AppointmentPlusRepository
import com.nhathuy.nextmeet.resource.AppointmentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AppointmentPlusViewModel @Inject constructor(
    private val appointmentRepository: AppointmentPlusRepository
) : ViewModel() {

    private val _appointmentUiState = MutableStateFlow<AppointmentUiState>(AppointmentUiState.Idle)
    val appointmentUiState: StateFlow<AppointmentUiState> = _appointmentUiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery : StateFlow<String>  = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestion:StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private var allAppointments : List<AppointmentPlus> = emptyList()

    /**
     * Tạo cuộc hẹn mới
     */
    fun createAppointment(
        appointment: AppointmentPlus
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                Log.d("AppointmentPlusViewModel", "Creating new appointment: ${appointment.title}")
                Log.d("AppointmentPlusViewModel", "Contact ID: ${appointment.contactId}, User ID: ${appointment.userId}")
                Log.d("AppointmentPlusViewModel", "Start time: ${appointment.startDateTime}")
                
                if (appointment.contactId == null) {
                    Log.e("AppointmentPlusViewModel", "Cannot create appointment: contactId is null")
                    _appointmentUiState.value = AppointmentUiState.Error("Vui lòng chọn liên hệ cho cuộc hẹn")
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
                    val appointmentId = result.getOrThrow()
                    Log.d("AppointmentPlusViewModel", "Appointment created successfully with ID: $appointmentId")
                    _appointmentUiState.value = AppointmentUiState.AppointmentCreated(
                        appointmentId,
                        "Cuộc hẹn đã được tạo thành công"
                    )
                } else {
                    Log.e("AppointmentPlusViewModel", "Failed to create appointment: ${result.exceptionOrNull()?.message}")
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi không xác định"
                    )
                }
            } catch (e: Exception) {
                Log.e("AppointmentPlusViewModel", "Exception during appointment creation", e)
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
                Log.d("AppointmentPlusViewModel", "Updating appointment ID: ${appointment.id}")
                Log.d("AppointmentPlusViewModel", "Title: ${appointment.title}, Contact ID: ${appointment.contactId}")
                Log.d("AppointmentPlusViewModel", "Start time: ${appointment.startDateTime}")
                
                val result = appointmentRepository.updateAppointment(appointment)
                if (result.isSuccess) {
                    Log.d("AppointmentPlusViewModel", "Appointment updated successfully")
                    _appointmentUiState.value = AppointmentUiState.AppointmentUpdated(
                        "Cuộc hẹn đã được cập nhật thành công"
                    )
                } else {
                    Log.e("AppointmentPlusViewModel", "Failed to update appointment: ${result.exceptionOrNull()?.message}")
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi cập nhật cuộc hẹn"
                    )
                }
            } catch (e: Exception) {
                Log.e("AppointmentPlusViewModel", "Exception during appointment update", e)
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
     * Tìm kiếm cuộc hẹn với từ khóa
     */
    fun searchAppointments(
        userId: Int,
        query: String,
        searchInTitle: Boolean = true,
        searchInDescription: Boolean = true,
        searchInLocation: Boolean = true,
        searchInContactName: Boolean = true
    ) {
        _searchQuery.value = query
        _isSearching.value = query.isNotEmpty()

        if (query.isEmpty()) {
            // Nếu query rỗng, hiển thị tất cả appointments
            _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(allAppointments)
            return
        }

        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                appointmentRepository.searchAppointments(
                    userId = userId,
                    query = query,
                    searchInTitle = searchInTitle,
                    searchInDescription = searchInDescription,
                    searchInLocation = searchInLocation,
                    searchInContactName = searchInContactName
                ).collect { appointments ->
                    _appointmentUiState.value = AppointmentUiState.SearchResults(query,appointments)
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.NoSearchResults(
                    e.message ?: "Lỗi khi tìm kiếm cuộc hẹn"
                )
            }
        }
    }

    /**
     * Lấy goi ý tìm kiếm
     */

    fun getSearchSuggestions(currentUserId:Int,query: String){
        viewModelScope.launch {
            try {
                val suggestions = appointmentRepository.getSearchSuggestions(currentUserId,query)
                _searchSuggestions.value = suggestions
            }
            catch (e:Exception){
                _searchSuggestions.value = emptyList()
            }
        }
    }

    /**
     * Lọc cuộn hẹn theo ngày hôm nay
     */
    fun getTodayAppointments(){
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY,0)
                    set(Calendar.MINUTE,0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val todayAppointments = allAppointments.filter { appointment ->
                    appointment.startDateTime >= startOfDay && appointment.startDateTime <= endOfDay
                }.sortedBy { it.startDateTime }

                _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(todayAppointments)
            }
            catch (e:Exception){

            }
        }
    }

    /**
     * Lọc cuộc hẹn sắp tới(từ bây giờ trở đi)
     */
    fun getUpcomingAppointments(){
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading

            try {
                val now = System.currentTimeMillis()
                val upcommingAppointments = allAppointments.filter { appointment ->
                    appointment.startDateTime > now
                }.sortedBy { it.startDateTime }
                _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(upcommingAppointments)
            }
            catch (e:Exception){
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi lọc cuộc hẹn sắp tới"
                )
            }
        }
    }

    /**
     * Lọc cuộc hẹn trong tuần này
     */
    fun getThisWeekAppointments() {

        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val thisWeekAppointments = allAppointments.filter { appointment ->
                    appointment.startDateTime >= startOfWeek && appointment.startDateTime <= endOfWeek
                }.sortedBy { it.startDateTime }

                _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(thisWeekAppointments)
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi lọc cuộc hẹn tuần này"
                )
            }
        }
    }

    /**
     * Lọc cuộc hẹn đã ghim
     */
    fun getPinnedAppointments() {

        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val pinnedAppointments = allAppointments.filter { it.isPinned }
                    .sortedBy { it.startDateTime }

                _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(pinnedAppointments)
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi lọc cuộc hẹn đã ghim"
                )
            }
        }
    }

    /**
     * Lọc cuộc hẹn theo trạng thái
     */
    fun getAppointmentsByStatus(status: AppointmentStatus) {

        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val filteredAppointments = allAppointments.filter {
                    it.status == status
                }.sortedBy { it.startDateTime }

                _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(filteredAppointments)
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi lọc cuộc hẹn theo trạng thái"
                )
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
     * Clear search query và reset về danh sách ban đầu
     */
    fun clearSearch(currentUserId: Int) {
        _searchQuery.value = ""
        _isSearching.value = false
        if (currentUserId != -1) {
            _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(allAppointments)
        }
    }

    /**
     * Lấy số lượng cuộc hẹn theo các tiêu chi
     */
    fun getAppointmentCounts() : Map<String,Int> {
        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return mapOf(
            "total" to allAppointments.size,
            "today" to allAppointments.count {
                it.startDateTime >= startOfToday && it.startDateTime <= endOfToday
            },
            "upcoming" to allAppointments.count { it.startDateTime > now },
            "pinned" to allAppointments.count { it.isPinned },
            "completed" to allAppointments.count { it.status == AppointmentStatus.COMPLETED },
            "cancelled" to allAppointments.count { it.status == AppointmentStatus.CANCELLED }
        )
    }
}