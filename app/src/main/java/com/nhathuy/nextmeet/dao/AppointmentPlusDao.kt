package com.nhathuy.nextmeet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import kotlinx.coroutines.flow.Flow

/**
 * Dao cho các thao tác với bảng appointments trong cơ sở dữ liệu.
 * Cung cấp các phương thức để thêm, lấy theo id, và lấy danh sách cuộc hẹn theo user.
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 06.10.2025
 */
@Dao
interface AppointmentPlusDao {
    /**
     * Thêm một cuộc hẹn mới vào cơ sở dữ liệu.
     * Nếu đã tồn tại (trùng khoá chính), sẽ ghi đè lên bản ghi cũ.
     * @param appointmentPlus Đối tượng AppointmentPlus cần thêm.
     * @return Id của bản ghi vừa thêm hoặc cập nhật.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointmentPlus(appointmentPlus: AppointmentPlus): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentPlus): Long

    /**
     * Lấy thông tin cuộc hẹn theo id.
     * @param id Id của cuộc hẹn.
     * @return Đối tượng AppointmentPlus tương ứng.
     */
    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Int): AppointmentPlus?


    /**
     * Lấy tất cả cuộc hẹn của một user, sắp xếp theo trạng thái ghim và thời gian cập nhật.
     * @param userId Id của người dùng.
     * @return Flow danh sách các cuộc hẹn.
     */
    @Query(
        """
        SELECT * FROM appointments
        WHERE user_id = :userId
        ORDER BY is_pinned DESC, updated_at DESC
    """
    )
    fun getAllAppointments(userId: Int): Flow<List<AppointmentPlus>>

    @Query("SELECT * FROM appointments WHERE user_id = :userId AND is_pinned = 1 ORDER BY start_date_time ASC")
    fun getPinnedAppointments(userId: Int): Flow<List<AppointmentPlus>>

    /**
     * Lấy danh sách cuộc hẹn theo trạng thái.
     * @param userId ID người dùng
     * @param status Trạng thái cuộc hẹn
     * @return Flow danh sách các cuộc hẹn theo trạng thái
     */
    @Query("SELECT * FROM appointments WHERE user_id = :userId AND status = :status ORDER BY start_date_time ASC")
    fun getAppointmentsByStatus(
        userId: Int,
        status: AppointmentStatus
    ): Flow<List<AppointmentPlus>>

    /**
     * Cập nhật trạng thái cuộc hẹn.
     * @param appointmentId ID cuộc hẹn
     * @param status Trạng thái mới
     * @param updatedAt Thời gian cập nhật
     */
    @Query("UPDATE appointments SET status = :status, updated_at = :updatedAt WHERE id = :appointmentId")
    suspend fun updateAppointmentStatus(
        appointmentId: Int,
        status: AppointmentStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Cập nhật trạng thái pin của cuộc hẹn.
     * @param appointmentId ID cuộc hẹn
     * @param isPinned Trạng thái pin mới
     * @param updatedAt Thời gian cập nhật
     */
    @Query("UPDATE appointments SET is_pinned = :isPinned, updated_at = :updatedAt WHERE id = :appointmentId")
    suspend fun updatePinStatus(
        appointmentId: Int,
        isPinned: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Xóa cuộc hen
     */
    @Delete
    suspend fun deleteAppointment(appointment: AppointmentPlus)

    /**
     * Xóa cuộc hẹn theo ID.
     * @param id ID cuộc hẹn
     */
    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Int)

    /**
     * Cập nhật thông tin cuộc hẹn.
     * @param appointment Đối tượng AppointmentPlus đã cập nhật
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAppointment(appointment: AppointmentPlus)

    /**
     * Cập nhật trạng thái bắt đầu điều hướng.
     * @param appointmentId ID cuộc hẹn
     * @param navigationStarted Đã bắt đầu điều hướng hay chưa
     * @param updatedAt Thời gian cập nhật
     */
    @Query("UPDATE appointments SET navigation_started = :navigationStarted, updated_at = :updatedAt WHERE id = :appointmentId")
    suspend fun updateNavigationStatus(
        appointmentId: Int,
        navigationStarted: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Lấy tất cả cuộc hẹn của user, sắp xếp theo pin và thời gian cập nhật.
     * @param userId ID người dùng
     * @return Flow danh sách các cuộc hẹn
     */
    @Query("SELECT * FROM appointments WHERE user_id = :userId ORDER BY is_pinned DESC, updated_at DESC")
    fun getAllAppointmentsByUser(userId: Int): Flow<List<AppointmentPlus>>

    /**
     * Lấy tìm kiếm cuộc hẹn theo từ khóa.
     */
    @Query(
        """
        SELECT * FROM appointments 
        WHERE user_id = :userId 
        AND (title LIKE '%' || :query || '%' 
             OR description LIKE '%' || :query || '%' 
             OR location LIKE '%' || :query || '%')
        ORDER BY 
            CASE WHEN is_pinned = 1 THEN 0 ELSE 1 END,
            CASE 
                WHEN title LIKE :query || '%' THEN 1
                WHEN title LIKE '%' || :query || '%' THEN 2
                ELSE 3
            END,
            start_date_time ASC
    """
    )
    fun searchAppointments(userId: Int, query: String): Flow<List<AppointmentPlus>>

    /**
     * Lấy danh sách gợi ý tiêu đề cuộc hẹn dựa trên từ khóa.
     */
    @Query("SELECT DISTINCT title FROM appointments WHERE user_id = :userId AND title LIKE :query || '%' ORDER BY title LIMIT :limit")
    fun getTitleSuggestions(userId: Int, query: String, limit: Int = 5): Flow<List<String>>

    /**
     * Lấy danh sách gợi ý địa điểm cuộc hẹn dựa trên từ khóa.
     */
    @Query("SELECT DISTINCT location FROM appointments WHERE user_id = :userId AND location != '' AND location LIKE :query || '%' ORDER BY location LIMIT :limit")
    fun getLocationSuggestions(userId: Int, query: String, limit: Int = 5): Flow<List<String>>

    /**
     * Lấy cuôc hẹn trong ngày hôm nay
     */
    @Query(
        "SELECT * FROM appointments \n" +
                "        WHERE user_id = :userId \n" +
                "        AND DATE(start_date_time / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')\n" +
                "        AND status = :status\n" +
                "        ORDER BY start_date_time ASC"
    )
    fun getTodayAppointments(
        userId: Int,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    ): Flow<List<AppointmentPlus>>

    /**
     * Lấy cuộn hẹn trong tuần này
     */
    @Query(
        """
    SELECT * FROM appointments 
    WHERE user_id = :userId 
      AND end_date_time >= :weekStart 
      AND start_date_time <= :weekEnd 
      AND status = :status 
    ORDER BY start_date_time ASC
"""
    )
    fun getThisWeekAppointments(
        userId: Int,
        weekStart: Long,
        weekEnd: Long,
        status: String
    ): Flow<List<AppointmentPlus>>

    /**
     * Lấy cuộc hẹn sắp tới
     */
    @Query(
        """
        SELECT * FROM appointments 
        WHERE user_id = :userId 
          AND start_date_time >= :currentTime 
          AND status = :status 
        ORDER BY start_date_time ASC
        """
    )
    fun getUpcomingAppointments(
        userId: Int,
        currentTime: Long = System.currentTimeMillis(),
        status: AppointmentStatus = AppointmentStatus.SCHEDULED
    ): Flow<List<AppointmentPlus>>

    /**
     * Lấy cuộc hẹn đã ghim
     */
    @Query(
        """
        SELECT * FROM appointments 
        WHERE user_id = :userId 
        AND is_pinned = 1
        AND status = :status
        ORDER BY start_date_time ASC
    """
    )
    fun getPinnedAppointments(
        userId: Int,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED
    ): Flow<List<AppointmentPlus>>

    /**
     * Đếm số lượng cuộc hẹn cho mỗi filter
     */
    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE user_id = :userId 
        AND DATE(start_date_time / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
        AND status = :status
    """
    )
    suspend fun getTodayAppointmentsCount(
        userId: Int,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE user_id = :userId 
        AND start_date_time >= :weekStart 
        AND start_date_time <= :weekEnd
        AND status = :status
    """
    )
    suspend fun getThisWeekAppointmentsCount(
        userId: Int,
        weekStart: Long,
        weekEnd: Long,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE user_id = :userId 
        AND start_date_time >= :currentTime
        AND status = :status
    """
    )
    suspend fun getUpcomingAppointmentsCount(
        userId: Int,
        currentTime: Long = System.currentTimeMillis(),
        status: AppointmentStatus = AppointmentStatus.SCHEDULED
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE user_id = :userId 
        AND is_pinned = 1
        AND status = :status
    """
    )
    suspend fun getPinnedAppointmentsCount(
        userId: Int,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED
    ): Int

    /**
     * lấy appointment voi contact tương ứng
     */
    @Query(
        """
        SELECT *  FROM appointments
        WHERE user_id = :userId 
        AND contact_id = :contactId 
        AND status = :status
        ORDER BY start_date_time ASC
    """
    )
    suspend fun getAppointmentByContactId(
        userId: Int,
        contactId: Int,
        status: AppointmentStatus
    ): List<AppointmentPlus>


    /**
     * Lấy tất cả cuộc hẹn có hoạt động
     */
    @Query(
        """
        SELECT * FROM appointments
        WHERE user_id = :userId
        AND status IN ('SCHEDULED', 'PREPARING', 'TRAVELLING', 'IN_PROCESS', 'DELAYED')
         AND start_date_time BETWEEN :startTime AND :endTime
        ORDER BY start_date_time ASC
    """
    )
    suspend fun getAllActiveAppointments(
        userId: Int,
        startTime: Long = System.currentTimeMillis() - 60 * 60 * 1000,
        endTime: Long = System.currentTimeMillis() + 6 * 60 * 60 * 1000
    ): List<AppointmentPlus>



    /**
     * Lấy cuoc hen trong thoi gian
     */
    @Query(
        """
            SELECT * FROM appointments 
        WHERE user_id = :userId 
        AND start_date_time BETWEEN :startTime AND :endTime
        AND status NOT IN ('COMPLETED', 'CANCELLED', 'MISSED')
        ORDER BY start_date_time ASC
        """
    )
    suspend fun getAppointmentsInTimeRange(
        userId: Int,
        startTime: Long,
        endTime: Long
    ): List<AppointmentPlus>

    /**
     * cập nhật thời gian di chuyển
     */
    @Query(
        """
        UPDATE appointments SET travel_time_minutes = :travelTimeMinutes, updated_at = :updatedAt
        WHERE id = :appointmentId
    """
    )
    suspend fun updateTravelTime(
        appointmentId: Int,
        travelTimeMinutes: Int,
        updatedAt: Long = System.currentTimeMillis()
    )
}
