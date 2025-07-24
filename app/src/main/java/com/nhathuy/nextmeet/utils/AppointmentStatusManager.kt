package com.nhathuy.nextmeet.utils

import android.content.Context
import android.location.Location
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.CancellationType
import com.nhathuy.nextmeet.model.NavigationCancelAction
import com.nhathuy.nextmeet.model.NavigationCheckResult
import com.nhathuy.nextmeet.model.NavigationRevertResult
import com.nhathuy.nextmeet.model.TimingInfo
import com.nhathuy.nextmeet.model.TransportMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AppointmentStatusManager(private val context: Context) {
    companion object {
        // 30 phút trước cuộc hẹn -> PREPARING
        private const val PREPARING_TIME_MINUTES = 30

        // 15 phút trước cuộc hẹn (nếu có travel time) -> DELAYED warning
        private const val DEPARTURE_WARNING_MINUTES = 15

        // 5 phút sau thời gian bắt đầu -> MISSED
        private const val MISSED_GRACE_PERIOD_MINUTES = 5

        // 30 phút sau thời gian kết thúc -> Auto COMPLETED
        private const val AUTO_COMPLETE_MINUTES = 30

        // Thời gian tối đa trước cuộc hẹn có thể bắt đầu navigation (12 tiếng)
        private const val MAX_EARLY_NAVIGATION_HOURS = 12

        // Thời gian tối thiểu trước cuộc hẹn nên bắt đầu navigation (based on travel time)
        private const val MIN_BUFFER_MINUTES = 10

        // thời gian tối thiểu thực sự di chuyển
        private const val MIN_MOVEMENT_TIME_MINUTES = 2

        // Helper methods cho transport mode - ĐƠN GIẢN
        private fun getMovementThreshold(transportMode: TransportMode?): Float {
            return when (transportMode) {
                TransportMode.WALKING -> 15f
                TransportMode.DRIVING -> 50f
                TransportMode.TRANSIT -> 25f
                else -> 30f // default
            }
        }

        private fun getGracePeriodMinutes(transportMode: TransportMode?): Long {
            return when (transportMode) {
                TransportMode.WALKING -> 1L
                TransportMode.DRIVING -> 3L
                TransportMode.TRANSIT -> 2L
                else -> 2L // default
            }
        }
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

        // thoi gian can khoi hanh
        val departureTime = startTime - TimeUnit.MINUTES.toMillis(travelTime.toLong())

        return when (currentStatus) {
            AppointmentStatus.SCHEDULED -> {
                when {
                    // Quá thời gian bắt đầu 5 phút mà chưa bắt đầu navigation -> MISSED
                    currentTime > startTime + TimeUnit.MINUTES.toMillis(MISSED_GRACE_PERIOD_MINUTES.toLong())
                            && !hasStartedNavigation -> AppointmentStatus.MISSED

                    // Đã qua thời gian bắt đầu -> IN_PROGRESS
                    currentTime >= startTime -> AppointmentStatus.IN_PROGRESS

                    // Đã bắt đầu navigation -> TRAVELLING
                    hasStartedNavigation -> AppointmentStatus.TRAVELLING

                    // Trễ thời gian khởi hành mà chưa bắt đầu navigation -> DELAYED
                    travelTime > 0 && currentTime > departureTime + TimeUnit.MINUTES.toMillis(
                        DEPARTURE_WARNING_MINUTES.toLong()
                    )
                            && !hasStartedNavigation -> AppointmentStatus.DELAYED

                    // 30 phút trước cuộc hẹn -> PREPARING
                    currentTime >= startTime - TimeUnit.MINUTES.toMillis(PREPARING_TIME_MINUTES.toLong()) -> AppointmentStatus.PREPARING

                    else -> AppointmentStatus.SCHEDULED
                }
            }

            AppointmentStatus.PREPARING -> {
                when {
                    currentTime > startTime + TimeUnit.MINUTES.toMillis(MISSED_GRACE_PERIOD_MINUTES.toLong())
                            && !hasStartedNavigation -> AppointmentStatus.MISSED

                    currentTime >= startTime -> AppointmentStatus.IN_PROGRESS
                    hasStartedNavigation -> AppointmentStatus.TRAVELLING
                    travelTime > 0 && currentTime > departureTime + TimeUnit.MINUTES.toMillis(
                        DEPARTURE_WARNING_MINUTES.toLong()
                    )
                            && !hasStartedNavigation -> AppointmentStatus.DELAYED

                    else -> AppointmentStatus.PREPARING
                }
            }

            AppointmentStatus.TRAVELLING -> {
                when {
                    // Nếu đã tắt navigation và qua giờ hẹn -> có thể là MISSED hoặc IN_PROGRESS
                    !hasStartedNavigation && currentTime > startTime + TimeUnit.MINUTES.toMillis(MISSED_GRACE_PERIOD_MINUTES.toLong()) -> AppointmentStatus.MISSED
                    currentTime >= startTime -> AppointmentStatus.IN_PROGRESS
                    // Nếu tắt navigation trước giờ hẹn -> revert về PREPARING hoặc SCHEDULED
                    !hasStartedNavigation -> {
                        if (currentTime >= startTime - TimeUnit.MINUTES.toMillis(PREPARING_TIME_MINUTES.toLong())) {
                            AppointmentStatus.PREPARING
                        } else {
                            AppointmentStatus.SCHEDULED
                        }
                    }
                    else -> AppointmentStatus.TRAVELLING
                }
            }

            AppointmentStatus.DELAYED -> {
                when {
                    currentTime > startTime + TimeUnit.MINUTES.toMillis(MISSED_GRACE_PERIOD_MINUTES.toLong())
                            && !hasStartedNavigation -> AppointmentStatus.MISSED

                    currentTime >= startTime -> AppointmentStatus.IN_PROGRESS
                    hasStartedNavigation -> AppointmentStatus.TRAVELLING
                    else -> AppointmentStatus.DELAYED
                }
            }

            AppointmentStatus.IN_PROGRESS -> {
                when {
                    // Tự động hoàn thành sau 30 phút kết thúc
                    currentTime > endTime + TimeUnit.MINUTES.toMillis(AUTO_COMPLETE_MINUTES.toLong()) -> AppointmentStatus.COMPLETED
                    else -> AppointmentStatus.IN_PROGRESS
                }
            }

            AppointmentStatus.COMPLETED,
            AppointmentStatus.CANCELLED,
            AppointmentStatus.MISSED -> currentStatus
        }
    }

    /**
     * Kiểm tra xem có nên cập nhật status hay không
     */
    fun shouldUpdateStatus(
        currentStatus: AppointmentStatus,
        newStatus: AppointmentStatus
    ): Boolean {
        return currentStatus != newStatus && currentStatus.canTransitionTo(newStatus)
    }

    /**
     * Lấy thông điệp mô tả cho status transition
     */
    fun getStatusTransitionMessage(
        oldStatus: AppointmentStatus,
        newStatus: AppointmentStatus
    ): String {
        return when (newStatus) {
            AppointmentStatus.PREPARING -> context.getString(R.string.status_preparing)
            AppointmentStatus.TRAVELLING -> context.getString(R.string.status_travelling)
            AppointmentStatus.IN_PROGRESS -> context.getString(R.string.status_in_progresss)
            AppointmentStatus.COMPLETED -> context.getString(R.string.status_completeds)
            AppointmentStatus.DELAYED -> context.getString(R.string.status_delayed)
            AppointmentStatus.MISSED -> context.getString(R.string.status_misseds)
            else -> context.getString(R.string.status_updated)
        }
    }

    /**
     * kiểm tra xem có thể revert navigation status không
     */
    fun canRevertNavigationStatus(
        appointment: AppointmentPlus,
        navigationStartTime: Long,
        currentLocation: Location?,
        startLocation: Location?
    ): NavigationRevertResult {
        val currentTime = System.currentTimeMillis()
        val timeSinceStart = currentTime - navigationStartTime

        // kiểm tra thời gian
        val isQuickCancel = timeSinceStart < MIN_MOVEMENT_TIME_MINUTES * 60 * 1000

        // kiểm tra khoảng cách
        val hasActuallyMoved = if (currentLocation != null && startLocation != null) {
            currentLocation.distanceTo(startLocation) > getMovementThreshold(null)
        } else {
            false
        }

        return when {
            isQuickCancel && !hasActuallyMoved -> {
                NavigationRevertResult(
                    canRevert = true,
                    shouldRevertStatus = true,
                    newStatus = getPreviousStatus(appointment.status),
                    reason = context.getString(R.string.reason_cancel_not_moved)
                )
            }

            hasActuallyMoved || !isQuickCancel -> {
                NavigationRevertResult(
                    canRevert = true,
                    shouldRevertStatus = false,
                    newStatus = appointment.status, // Giữ nguyên
                    reason = context.getString(R.string.navigation_cancelled)
                )
            }

            else -> {
                NavigationRevertResult(
                    canRevert = true,
                    shouldRevertStatus = false,
                    newStatus = appointment.status,
                    reason = context.getString(R.string.navigation_cancelled)
                )
            }
        }
    }

    /**
     * xử lý việc set navigation_start về false và update status về trạng thái trước đó
     */
    fun handleNavigationCancellation(
        appointment: AppointmentPlus,
        navigationStartTime: Long,
        currentLocation: Location?,
        startLocation: Location?,
        transportMode: TransportMode? = null // Thêm parameter này
    ): NavigationCancelAction {
        val currentTime = System.currentTimeMillis()
        val timeSinceStart = currentTime - navigationStartTime
        val timeMinutes = timeSinceStart / (60 * 1000)

        // Sử dụng threshold phù hợp với transport mode
        val movementThreshold = getMovementThreshold(transportMode)
        val gracePeriod = getGracePeriodMinutes(transportMode)

        // Kiểm tra khoảng cách di chuyển
        val actualDistance = if (currentLocation != null && startLocation != null) {
            currentLocation.distanceTo(startLocation)
        } else 0f

        // Logic đơn giản dựa trên transport mode
        val cancellationType = when {
            // Immediate cancel - trong 1 phút và chưa di chuyển
            timeMinutes < 1 && actualDistance < 10f -> CancellationType.IMMEDIATE_CANCEL

            // Grace period cancel - trong grace period và chưa di chuyển đủ xa
            timeMinutes <= gracePeriod && actualDistance < movementThreshold -> {
                CancellationType.GRACE_PERIOD_CANCEL
            }

            // After movement - đã di chuyển đủ xa hoặc đủ lâu
            actualDistance >= movementThreshold || timeMinutes >= gracePeriod -> {
                CancellationType.AFTER_MOVEMENT
            }

            else -> CancellationType.LATE_CANCEL
        }

        return when (cancellationType) {
            CancellationType.IMMEDIATE_CANCEL -> {
                NavigationCancelAction(
                    shouldUpdateNavigationStarted = true,
                    shouldUpdateStatus = true,
                    newStatus = getRevertStatus(appointment),
                    message = context.getString(R.string.cancel_immediate)
                )
            }

            CancellationType.GRACE_PERIOD_CANCEL -> {
                NavigationCancelAction(
                    shouldUpdateNavigationStarted = true,
                    shouldUpdateStatus = true,
                    newStatus = getRevertStatus(appointment),
                    message = context.getString(R.string.cancel_grace)
                )
            }

            CancellationType.AFTER_MOVEMENT -> {
                NavigationCancelAction(
                    shouldUpdateNavigationStarted = true,
                    shouldUpdateStatus = false,
                    newStatus = appointment.status,
                    message = context.getString(R.string.cancel_after_movement)
                )
            }

            CancellationType.LATE_CANCEL -> {
                val newStatus = calculateAppropriateStatusAfterLateCancel(appointment, currentTime)
                NavigationCancelAction(
                    shouldUpdateNavigationStarted = true,
                    shouldUpdateStatus = newStatus != appointment.status,
                    newStatus = newStatus,
                    message = getLateCancelMessage(appointment.status, newStatus)
                )
            }
        }
    }

    private fun getRevertStatus(appointment: AppointmentPlus): AppointmentStatus {
        return when (appointment.status) {
            AppointmentStatus.TRAVELLING -> {
                // Revert về PREPARING hoặc SCHEDULED tùy vào thời gian
                val currentTime = System.currentTimeMillis()
                val timeUntilAppointment = appointment.startDateTime - currentTime
                val thirtyMinutes = 30 * 60 * 1000L

                if (timeUntilAppointment <= thirtyMinutes) {
                    AppointmentStatus.PREPARING
                } else {
                    AppointmentStatus.SCHEDULED
                }
            }
            AppointmentStatus.IN_PROGRESS -> {
                // Từ IN_PROGRESS về PREPARING (trường hợp đặc biệt)
                AppointmentStatus.PREPARING
            }
            else -> appointment.status
        }
    }

    private fun calculateAppropriateStatusAfterLateCancel(
        appointment: AppointmentPlus,
        currentTime: Long
    ): AppointmentStatus {
        // Tính toán status mới dựa trên thời gian hiện tại, bỏ qua navigation state
        return calculateNewStatus(
            appointment = appointment.copy(navigationStarted = false),
            currentTime = currentTime,
            hasStartedNavigation = false
        )
    }

    private fun getLateCancelMessage(oldStatus: AppointmentStatus, newStatus: AppointmentStatus): String {
        return if (oldStatus != newStatus) {
            context.getString(R.string.nav_stopped_with_status,getStatusTransitionMessage(oldStatus, newStatus))
        } else {
            context.getString(R.string.nav_stopped)
        }
    }

    /**
     * Lấy status trước đó phù hợp
     */
    private fun getPreviousStatus(currentStatus: AppointmentStatus): AppointmentStatus {
        return when (currentStatus) {
            AppointmentStatus.TRAVELLING -> {
                // Từ TRAVELLING về PREPARING hoặc SCHEDULED
                AppointmentStatus.PREPARING
            }

            AppointmentStatus.IN_PROGRESS -> {
                // Từ IN_PROGRESS về TRAVELLING (trường hợp đặc biệt)
                AppointmentStatus.TRAVELLING
            }

            else -> {
                // Các trường hợp khác giữ nguyên
                currentStatus
            }
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
            AppointmentStatus.IN_PROGRESS -> "#4CAF50" // Green
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
            AppointmentStatus.IN_PROGRESS -> R.drawable.ic_meeting
            AppointmentStatus.COMPLETED -> R.drawable.ic_check_circle
            AppointmentStatus.DELAYED -> R.drawable.ic_warning
            AppointmentStatus.CANCELLED -> R.drawable.ic_cancel
            AppointmentStatus.MISSED -> R.drawable.ic_error
        }
    }

    /**
     * Kiểm tra xem có thể bắt đầu navigation không
     */
    fun canStartNavigationNow(appointment: AppointmentPlus): NavigationCheckResult {
        val currentTime = System.currentTimeMillis()
        val appointmentStartTime = appointment.startDateTime
        val travelTimeMs = TimeUnit.MINUTES.toMillis(appointment.travelTimeMinutes.toLong())

        // Tính thời gian cần khởi hành
        val idealDepartureTime =
            appointmentStartTime - travelTimeMs - TimeUnit.MINUTES.toMillis(MIN_BUFFER_MINUTES.toLong())

        // Thời gian sớm nhất có thể bắt đầu navigation
        val earliestNavigationTime =
            appointmentStartTime - TimeUnit.HOURS.toMillis(MAX_EARLY_NAVIGATION_HOURS.toLong())

        return when {
            // Quá muộn - đã qua thời gian hẹn
            currentTime > appointmentStartTime -> {
                NavigationCheckResult(
                    canStart = false,
                    reason = context.getString(R.string.nav_too_late_reason,formatTime(appointmentStartTime)),
                    buttonText = context.getString(R.string.nav_too_late_button),
                    showWarning = true
                )
            }

            // Quá sớm - hơn 12 tiếng trước cuộc hẹn
            currentTime < earliestNavigationTime -> {
                val hoursUntilEarliest = (earliestNavigationTime - currentTime) / (1000 * 60 * 60)
                NavigationCheckResult(
                    canStart = false,
                    reason = context.getString(
                        R.string.nav_too_early_reason,
                        hoursUntilEarliest
                    ),
                    buttonText = "Quá sớm",
                    showInfo = true
                )
            }

            // Đang trong thời gian hợp lý
            currentTime >= idealDepartureTime -> {
                NavigationCheckResult(
                    canStart = true,
                    reason = context.getString(R.string.nav_depart_now_reason),
                    buttonText = context.getString(R.string.nav_depart_now_button),
                    showSuccess = true
                )
            }

            // Hơi sớm nhưng vẫn có thể navigation
            currentTime >= earliestNavigationTime -> {
                val minutesEarly = (idealDepartureTime - currentTime) / (1000 * 60)
                NavigationCheckResult(
                    canStart = true,
                    reason = context.getString(R.string.nav_early_but_ok_reason,minutesEarly ),
                    buttonText = context.getString(R.string.nav_early_but_ok_button),
                    showWarning = true
                )
            }

            else -> {
                NavigationCheckResult(
                    canStart = false,
                    reason = context.getString(R.string.nav_unknown_reason),
                    buttonText = context.getString(R.string.nav_unknown_button)
                )
            }
        }
    }

    /**
     * Lấy thông tin timing để hiển thị
     */
    fun getTimingInfo(appointment: AppointmentPlus): TimingInfo {
        val currentTime = System.currentTimeMillis()
        val appointmentStartTime = appointment.startDateTime
        val travelTimeMs = TimeUnit.MINUTES.toMillis(appointment.travelTimeMinutes.toLong())
        val idealDepartureTime =
            appointmentStartTime - travelTimeMs - TimeUnit.MINUTES.toMillis(MIN_BUFFER_MINUTES.toLong())

        val timeUntilAppointment = appointmentStartTime - currentTime
        val timeUntilDeparture = idealDepartureTime - currentTime

        return TimingInfo(
            appointmentTime = formatDateTime(appointmentStartTime),
            timeUntilAppointment = formatDuration(timeUntilAppointment),
            idealDepartureTime = formatDateTime(idealDepartureTime),
            timeUntilDeparture = formatDuration(timeUntilDeparture),
            travelTime = context.getString(R.string.travel_duration_minutes,appointment.travelTimeMinutes),
            isToday = isToday(appointmentStartTime),
            isTomorrow = isTomorrow(appointmentStartTime)
        )
    }

    private fun formatTime(timeMs: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
    }

    private fun formatDateTime(timeMs: Long): String {
        return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timeMs))
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs < 0) return context.getString(R.string.travel_duration_past)

        val hours = durationMs / (1000 * 60 * 60)
        val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)

        return when {
            hours > 24 -> context.getString(R.string.travel_duration_days_hours,hours / 24,hours % 24)
            hours > 0 -> context.getString(R.string.travel_duration_hours_minutes,hours,minutes)
            else -> context.getString(R.string.travel_duration_minutes, minutes)
        }
    }

    private fun isToday(timeMs: Long): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timeMs }

        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTomorrow(timeMs: Long): Boolean {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val target = Calendar.getInstance().apply { timeInMillis = timeMs }

        return tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
}