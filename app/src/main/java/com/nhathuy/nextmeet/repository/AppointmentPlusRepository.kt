package com.nhathuy.nextmeet.repository

import com.nhathuy.nextmeet.dao.AppointmentPlusDao
import com.nhathuy.nextmeet.dao.ContactDao
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository quản lý các thao tác dữ liệu cho AppointmentPlus.
 * Đóng vai trò là lớp trung gian giữa tầng truy cập dữ liệu (DAO) và tầng nghiệp vụ,
 * cung cấp các phương thức thao tác với dữ liệu cuộc hẹn.
 *
 * @author TravisHuy(Ho Nhat Huy)
 * @since 06.10.2025
 * @version 2.0
 */
@Singleton
class AppointmentPlusRepository @Inject constructor(private val appointmentPlusDao: AppointmentPlusDao,
    private val contactDao: ContactDao) {

    /**
     * Lấy tất cả cuộc hẹn theo userId, cho phép tìm kiếm và lọc theo trạng thái.
     * Ưu tiên cuộc hẹn được pin, sắp xếp theo thời gian bắt đầu.
     * @param userId ID người dùng
     * @param searchQuery Từ khóa tìm kiếm (tiêu đề/mô tả)
     * @param showPinnedOnly Chỉ lấy cuộc hẹn được pin
     * @param status Lọc theo trạng thái cuộc hẹn
     * @return Flow danh sách cuộc hẹn đã lọc và sắp xếp
     */
    fun getAllAppointmentsWithFilter(
        userId: Int,
        searchQuery: String = "",
        showPinnedOnly: Boolean = false,
        status: AppointmentStatus? = null
    ): Flow<List<AppointmentPlus>> {
        return flow {
            try {
                val appointmentsFlow = when {
                    showPinnedOnly -> appointmentPlusDao.getPinnedAppointments(userId)
                    status != null -> appointmentPlusDao.getAppointmentsByStatus(userId, status)
                    searchQuery.isNotEmpty() -> appointmentPlusDao.getAllAppointmentsByUser(userId)
                        .map { appointments ->
                            appointments.filter { appointment ->
                                appointment.title.contains(searchQuery, ignoreCase = true) ||
                                        appointment.description.contains(
                                            searchQuery,
                                            ignoreCase = true
                                        )
                            }
                        }

                    else -> appointmentPlusDao.getAllAppointmentsByUser(userId)
                }

                appointmentsFlow.collect { appointments ->
                    var filteredAppointments = appointments

                    if (searchQuery.isNotBlank() && !showPinnedOnly && status == null) {
                        filteredAppointments = filteredAppointments.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.description.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    val sortedAppointments = filteredAppointments.sortedWith(
                        compareByDescending<AppointmentPlus> { it.isPinned }
                            .thenBy { it.startDateTime }
                    )

                    emit(sortedAppointments)
                }
            } catch (e: Exception) {
                emit(emptyList())
                throw e
            }
        }
    }

    /**
     * Tạo cuộc hẹn mới.
     *
     * @param userId ID của người dùng liên kết với cuộc hẹn.
     * @param contactId ID của liên hệ liên quan đến cuộc hẹn.
     * @param title Tiêu đề của cuộc hẹn.
     * @param description Mô tả chi tiết về cuộc hẹn.
     * @param startDateTime Thời gian bắt đầu cuộc hẹn.
     * @param endDateTime Thời gian kết thúc cuộc hẹn.
     * @param location Địa điểm diễn ra cuộc hẹn.
     * @param latitude Vĩ độ của địa điểm.
     * @param longitude Kinh độ của địa điểm.
     * @param status Trạng thái của cuộc hẹn.
     * @param travelTimeMinutes Thời gian di chuyển ước tính.
     * @param isPinned Cuộc hẹn có được pin không.
     * @return Đối tượng [Result] chứa ID của cuộc hẹn vừa được tạo thành công,
     * hoặc một ngoại lệ nếu thất bại.
     */
    suspend fun createAppointment(
        userId: Int,
        contactId: Int,
        title: String = "",
        description: String = "",
        startDateTime: Long? = null,
        endDateTime: Long? = null,
        location: String = "",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED,
        color: String = "color_white",
        travelTimeMinutes: Int = 0,
        isPinned: Boolean = false
    ): Result<Long> {
        if (userId <= 0) {
            return Result.failure(IllegalArgumentException("User ID không hợp lệ"))
        }

        if (contactId <= 0) {
            return Result.failure(IllegalArgumentException("Contact ID không hợp lệ"))
        }

        val validationResult = validateAppointmentInputs(title, startDateTime, endDateTime)
        if (validationResult.isFailure) {
            return Result.failure(
                IllegalArgumentException(
                    validationResult.exceptionOrNull()?.message ?: "Dữ liệu đầu vào không hợp lệ"
                )
            )
        }

        if (!isValidHexColor(color)) {
            return Result.failure(IllegalArgumentException("Màu sắc không hợp lệ"))
        }


        val appointment = AppointmentPlus(
            id = 0, // Auto-generated
            userId = userId,
            contactId = contactId,
            title = title,
            description = description,
            startDateTime = startDateTime!!,
            endDateTime = endDateTime!!,
            location = location,
            latitude = latitude,
            longitude = longitude,
            status = status,
            travelTimeMinutes = travelTimeMinutes,
            color = color,
            navigationStarted = false,
            isPinned = isPinned,
            createdAt = System.currentTimeMillis(),
            updateAt = System.currentTimeMillis()
        )

        return try {
            val appointmentId = appointmentPlusDao.insertAppointment(appointment)
            Result.success(appointmentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xác thực các trường đầu vào của cuộc hẹn.
     * @param title Tiêu đề cuộc hẹn.
     * @param startDateTime Thời gian bắt đầu.
     * @param endDateTime Thời gian kết thúc.
     * @return Kết quả xác thực, trả về lỗi nếu có.
     */
    private fun validateAppointmentInputs(
        title: String,
        startDateTime: Long?,
        endDateTime: Long?
    ): Result<Unit> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Tiêu đề cuộc hẹn không được để trống"))
        }

        if (startDateTime == null || startDateTime <= 0) {
            return Result.failure(IllegalArgumentException("Thời gian bắt đầu không hợp lệ"))
        }

        if (endDateTime == null || endDateTime <= 0) {
            return Result.failure(IllegalArgumentException("Thời gian kết thúc không hợp lệ"))
        }

        if (endDateTime <= startDateTime) {
            return Result.failure(IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu"))
        }

        return Result.success(Unit)
    }

    /**
     * Kiểm tra hex color format hoặc tên màu hợp lệ
     */
    private fun isValidHexColor(color: String): Boolean {
        // Chấp nhận tên màu resource hoặc mã hex
        val allowedColorNames = setOf(
            "color_white", "color_red", "color_orange", "color_yellow", "color_green",
            "color_teal", "color_blue", "color_dark_blue", "color_purple", "color_pink",
            "color_brown", "color_gray"
        )
        return color.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) || allowedColorNames.contains(
            color
        )
    }


    /**
     * Cập nhật trạng thái cuộc hẹn.
     */
    suspend fun updateAppointmentStatus(
        appointmentId: Int,
        status: AppointmentStatus
    ): Result<Unit> {
        return try {
            val appointment = appointmentPlusDao.getAppointmentById(appointmentId)
                ?: return Result.failure(IllegalArgumentException("Cuộc hẹn không tồn tại"))

            appointmentPlusDao.updateAppointmentStatus(appointmentId, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle pin/unpin cuộc hẹn.
     */
    suspend fun togglePin(appointmentId: Int): Result<Boolean> {
        return try {
            val appointment = appointmentPlusDao.getAppointmentById(appointmentId)
                ?: return Result.failure(IllegalArgumentException("Cuộc hẹn không tồn tại"))

            val newPinStatus = !appointment.isPinned
            appointmentPlusDao.updatePinStatus(appointmentId, newPinStatus)
            Result.success(newPinStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa cuộc hẹn theo ID.
     */
    suspend fun deleteAppointment(appointmentId: Int): Result<Unit> {
        return try {
            val appointment = appointmentPlusDao.getAppointmentById(appointmentId)
                ?: return Result.failure(IllegalArgumentException("Cuộc hẹn không tồn tại"))

            appointmentPlusDao.deleteAppointment(appointment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cập nhật thông tin cuộc hẹn.
     */
    suspend fun updateAppointment(appointment: AppointmentPlus): Result<Unit> {
        return try {
            val existingAppointment = appointmentPlusDao.getAppointmentById(appointment.id)
                ?: return Result.failure(IllegalArgumentException("Cuộc hẹn không tồn tại"))

            val validationResult = validateAppointmentInputs(
                appointment.title,
                appointment.startDateTime,
                appointment.endDateTime
            )
            if (validationResult.isFailure) {
                return Result.failure(
                    IllegalArgumentException(
                        validationResult.exceptionOrNull()?.message
                            ?: "Dữ liệu đầu vào không hợp lệ"
                    )
                )
            }

            val updatedAppointment = appointment.copy(updateAt = System.currentTimeMillis())
            appointmentPlusDao.updateAppointment(updatedAppointment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy cuộc hẹn theo ID.
     */
    suspend fun getAppointmentById(appointmentId: Int): Result<AppointmentPlus> {
        return try {
            val appointment = appointmentPlusDao.getAppointmentById(appointmentId)
                ?: return Result.failure(IllegalArgumentException("Cuộc hẹn không tồn tại"))
            Result.success(appointment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bắt đầu điều hướng đến cuộc hẹn.
     */
    suspend fun startNavigation(appointmentId: Int): Result<Unit> {
        return try {
            appointmentPlusDao.updateNavigationStatus(appointmentId, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tìm kiếm cuộc hẹn theo từ khóa.
     */

    /**
     * Tìm kiếm các cuộc hẹn theo từ khóa.
     * Cho phép tìm kiếm trong tiêu đề, mô tả, địa điểm, và tên liên hệ.
     *
     * @param userId ID của người dùng.
     * @param query Từ khóa tìm kiếm.
     * @param searchInTitle Có tìm kiếm trong tiêu đề hay không.
     * @param searchInDescription Có tìm kiếm trong mô tả hay không.
     * @param searchInLocation Có tìm kiếm trong địa điểm hay không.
     * @param searchInContactName Có tìm kiếm trong tên liên hệ hay không.
     * @return Flow danh sách các cuộc hẹn phù hợp với từ khóa.
     */
    suspend fun searchAppointments(
        userId: Int,
        query: String,
        searchInTitle: Boolean = true,
        searchInDescription: Boolean = true,
        searchInLocation: Boolean = true,
        searchInContactName: Boolean = true
    ): Flow<List<AppointmentPlus>> = flow {
        val appointments = appointmentPlusDao.getAllAppointmentsByUser(userId).first()

        val filteredAppointments = appointments.filter { appointment ->
            var matchFound = false

            if (searchInTitle && appointment.title.contains(query, ignoreCase = true)) {
                matchFound = true
            }

            if (searchInDescription && appointment.description.contains(query, ignoreCase = true)) {
                matchFound = true
            }

            if (searchInLocation && appointment.location.contains(query, ignoreCase = true)) {
                matchFound = true
            }

            if(searchInContactName){
                try {
                    val contact = contactDao.getContactById(appointment.contactId!!)
                    if (contact?.name?.contains(query, ignoreCase = true) == true) {
                        matchFound = true
                    }
                } catch (e: Exception) {
                    // Liên hệ có thể không tồn tại.
                }
            }
            matchFound
        }

        emit(filteredAppointments.sortedBy { it.startDateTime })
    }

    /**
     * Lấy gợi ý tìm kiếm dựa trên dữ liệu các cuộc hẹn hiện có.
     * Phương thức này sẽ kiểm tra tiêu đề và địa điểm của các cuộc hẹn,
     * sau đó trả về danh sách gợi ý phù hợp với từ khóa tìm kiếm.
     *
     * @param userId ID của người dùng.
     * @param query Từ khóa tìm kiếm.
     * @return Danh sách tối đa 10 gợi ý tìm kiếm.
     */
    suspend fun getSearchSuggestions(userId:Int, query: String) : List<String> {
        if(query.isBlank()){
            return emptyList()
        }

        val appointments = appointmentPlusDao.getAllAppointmentsByUser(userId).first()
        val suggestions = mutableSetOf<String>()

        appointments.forEach { appointment ->
            if(appointment.title.contains(query, ignoreCase = true) && appointment.title.isNotEmpty()){
                suggestions.add(appointment.title)
            }

            if (appointment.location.contains(query, ignoreCase = true) && appointment.location.isNotEmpty()) {
                suggestions.add(appointment.location)
            }
        }

        return suggestions.take(10).toList()
    }


}