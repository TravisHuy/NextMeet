package com.nhathuy.nextmeet.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentWithContact
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.HistoryCounts
import com.nhathuy.nextmeet.model.HistoryStatistics
import com.nhathuy.nextmeet.repository.AppointmentPlusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val appointmentRepository: AppointmentPlusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // UI States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Filter States
    private val _currentFilter = MutableStateFlow(HistoryFilter.ALL)
    val currentFilter: StateFlow<HistoryFilter> = _currentFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Data States
    private val _historyCounts = MutableStateFlow(HistoryCounts(0, 0, 0, 0))
    val historyCounts: StateFlow<HistoryCounts> = _historyCounts.asStateFlow()

    private val _historyStatistics = MutableStateFlow<HistoryStatistics?>(null)
    val historyStatistics: StateFlow<HistoryStatistics?> = _historyStatistics.asStateFlow()

    // Raw data from repository
    private val _allHistoryAppointments =
        MutableStateFlow<List<AppointmentWithContact>>(emptyList())

    // Filtered and processed data for UI
    val filteredAppointments: StateFlow<List<AppointmentWithContact>> = combine(
        _allHistoryAppointments,
        _currentFilter,
        _searchQuery
    ) { appointments, filter, query ->
        var filtered = when (filter) {
            HistoryFilter.ALL -> appointments
            HistoryFilter.COMPLETED -> appointments.filter { it.appointment.status == AppointmentStatus.COMPLETED }
            HistoryFilter.CANCELLED -> appointments.filter { it.appointment.status == AppointmentStatus.CANCELLED }
            HistoryFilter.MISSED -> appointments.filter { it.appointment.status == AppointmentStatus.MISSED }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { appointmentWithContact ->
                val appointment = appointmentWithContact.appointment
                val contactName = appointmentWithContact.contactName ?: ""

                appointment.title.contains(query, ignoreCase = true) ||
                        appointment.description.contains(query, ignoreCase = true) ||
                        appointment.location.contains(query, ignoreCase = true) ||
                        contactName.contains(query, ignoreCase = true)
            }
        }

        filtered.sortedByDescending { it.appointment.startDateTime }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Grouped appointments by date for better UI organization
    val groupedAppointments: StateFlow<Map<String, List<AppointmentWithContact>>> =
        filteredAppointments.map { appointments ->
            appointments.groupByDate()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private var loadDataJob: Job? = null

    enum class HistoryFilter {
        ALL, COMPLETED, CANCELLED, MISSED
    }


    /**
     * Load all history data for user
     */
    fun loadHistoryData(userId: Int) {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Load history appointments
                appointmentRepository.getAllHistoryAppointments(userId)
                    .catch { e ->
                        Log.e("HistoryViewModel", "Error loading history appointments", e)
                        _errorMessage.value =
                            context.getString(R.string.error_loading_history, e.message)
                        _isLoading.value = false
                    }
                    .collect { appointmentsWithContact ->
                        // THAY ĐỔI: Không cần chuyển đổi, sử dụng trực tiếp AppointmentWithContact
                        _allHistoryAppointments.value = appointmentsWithContact
                        updateCounts(appointmentsWithContact)

                        // Load statistics sau khi đã có data
                        loadHistoryStatistics(userId)

                        // Đảm bảo set loading = false sau khi collect xong
                        _isLoading.value = false
                    }

            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error in loadHistoryData", e)
                _errorMessage.value = context.getString(R.string.error_loading_data, e.message)
                _isLoading.value = false
            }
        }
    }

    /**
     * Set filter for history appointments
     */
    fun setFilter(filter: HistoryFilter) {
        _currentFilter.value = filter
    }

    /**
     * Set search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Refresh history data
     */
    fun refresh(userId: Int) {
        loadHistoryData(userId)
    }

    /**
     * Get appointments in date range
     */
    fun getAppointmentsInDateRange(
        userId: Int,
        startDate: Calendar,
        endDate: Calendar
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                appointmentRepository.getHistoryAppointmentsInRange(
                    userId = userId,
                    startTime = startDate.timeInMillis,
                    endTime = endDate.timeInMillis
                ).catch { e ->
                    Log.e("HistoryViewModel", "Error loading appointments in range", e)
                    _errorMessage.value = context.getString(R.string.error_loading_range)
                    _isLoading.value = false
                }.collect { appointmentsWithContact ->
                    _allHistoryAppointments.value = appointmentsWithContact
                    updateCounts(appointmentsWithContact)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error in getAppointmentsInDateRange", e)
                _errorMessage.value = context.getString(R.string.error_loading_statistics)
                _isLoading.value = false
            }
        }
    }

    /**
     * Get latest appointment by status
     */
    suspend fun getLatestAppointmentByStatus(
        userId: Int,
        status: AppointmentStatus
    ): AppointmentPlus? {
        return try {
            val result = appointmentRepository.getLatestAppointmentByStatus(userId, status)
            result.getOrNull()
        } catch (e: Exception) {
            Log.e("HistoryViewModel", "Error getting latest appointment", e)
            null
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }


    private suspend fun loadHistoryStatistics(userId: Int) {
        try {
            val result = appointmentRepository.getHistoryStatistics(userId)
            if (result.isSuccess) {
                _historyStatistics.value = result.getOrThrow()
            } else {
                Log.e("HistoryViewModel", "Error loading statistics: ${result.exceptionOrNull()}")
            }
        } catch (e: Exception) {
            Log.e("HistoryViewModel", "Error loading statistics", e)
        }
    }

    // THAY ĐỔI: updateCounts nhận AppointmentWithContact
    private fun updateCounts(appointments: List<AppointmentWithContact>) {
        val completed = appointments.count { it.appointment.status == AppointmentStatus.COMPLETED }
        val cancelled = appointments.count { it.appointment.status == AppointmentStatus.CANCELLED }
        val missed = appointments.count { it.appointment.status == AppointmentStatus.MISSED }
        val total = appointments.size

        _historyCounts.value = HistoryCounts(
            completed = completed,
            cancelled = cancelled,
            missed = missed,
            total = total
        )
    }


    /**
     * Get completion rate as percentage string
     */
    fun getCompletionRateText(): String {
        val stats = _historyStatistics.value ?: return "0%"
        val percentage = (stats.completionRate * 100).toInt()
        return "$percentage%"
    }

    /**
     * Get summary text for current filter
     */
    fun getSummaryText(): String {
        val counts = _historyCounts.value
        return when (_currentFilter.value) {
            HistoryFilter.ALL -> context.getString(R.string.history_summary_all, counts.total)
            HistoryFilter.COMPLETED -> context.getString(
                R.string.history_summary_completed,
                counts.completed
            )
            HistoryFilter.CANCELLED -> context.getString(R.string.history_summary_cancelled,counts.cancelled)
            HistoryFilter.MISSED -> context.getString(R.string.history_summary_missed,counts.missed)
        }
    }

    /**
     * Check if current filter has any appointments
     */
    fun hasAppointmentsForCurrentFilter(): Boolean {
        val counts = _historyCounts.value
        return when (_currentFilter.value) {
            HistoryFilter.ALL -> counts.total > 0
            HistoryFilter.COMPLETED -> counts.completed > 0
            HistoryFilter.CANCELLED -> counts.cancelled > 0
            HistoryFilter.MISSED -> counts.missed > 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadDataJob?.cancel()
    }
}

/**
 * Group appointments by date string - THAY ĐỔI: sử dụng AppointmentWithContact
 */
private fun List<AppointmentWithContact>.groupByDate(): Map<String, List<AppointmentWithContact>> {
    val calendar = Calendar.getInstance()
    return this.groupBy { appointmentWithContact ->
        calendar.timeInMillis = appointmentWithContact.appointment.startDateTime
        "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${
            calendar.get(
                Calendar.YEAR
            )
        }"
    }
}

/**
 * Format date for grouping
 */
private fun Long.toDateKey(): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    return "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${
        calendar.get(
            Calendar.YEAR
        )
    }"
}