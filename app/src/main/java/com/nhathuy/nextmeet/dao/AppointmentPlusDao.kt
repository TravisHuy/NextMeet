package com.nhathuy.nextmeet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhathuy.nextmeet.model.AppointmentPlus
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
        status: com.nhathuy.nextmeet.model.AppointmentStatus
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
        status: com.nhathuy.nextmeet.model.AppointmentStatus,
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
}
