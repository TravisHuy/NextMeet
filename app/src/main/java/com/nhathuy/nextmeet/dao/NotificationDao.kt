package com.nhathuy.nextmeet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nhathuy.nextmeet.model.Notification
import com.nhathuy.nextmeet.model.NotificationType
import kotlinx.coroutines.flow.Flow

/**
 * DAO với thông báo
 *
 * @author TravisHuy(Ho Nhat Huy)
 * @since 30.06.2025
 */
@Dao
interface NotificationDao {

    /**
     * Tạo ghi chú
     */
    @Insert
    suspend fun insertNotification(notification: Notification) : Long

    /**
     * Cập nhật ghi chú
     */
    @Update
    suspend fun updateNotification(notification: Notification)

    /**
     * Xóa ghi chú
     */
    @Delete
    suspend fun deleteNotification(notification: Notification)

    /**
     * Xóa ghi chú theo id
     */
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Int) : Int

    /**
     * Lấy thông báo theo id
     */
    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    suspend fun getNotificationById(notificationId: Int): Notification?

    /**
     * Lay thông báo từ user id tương ứng
     */
    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    fun getNotificationsByUserId(userId: Int): Flow<List<Notification>>

    /**
     * lấy thong báo dựa theo loại thông báo
     */
    @Query("SELECT * FROM notifications WHERE related_id = :relatedId AND notification_type = :type")
    fun getNotificationsByRelatedId(relatedId: Int, type: NotificationType): Flow<List<Notification>>

    /**
     * Lấy thông báo đang pending
     */
    @Query("SELECT * FROM notifications WHERE user_id = :userId AND scheduled_time > :currentTime AND is_sent = 0 ORDER BY scheduled_time ASC")
    fun getPendingNotifications(userId: Int, currentTime: Long): Flow<List<Notification>>

    /**
     * Lấy lịch su thông báo
     */
    @Query("SELECT * FROM notifications WHERE user_id = :userId AND is_sent = 1 ORDER BY scheduled_time DESC")
    fun getNotificationHistory(userId: Int): Flow<List<Notification>>

    /**
     * Đánh dấu thông báo đã được gửi
     */
    @Query("UPDATE notifications SET is_sent = 1 WHERE id = :notificationId")
    suspend fun markAsSent(notificationId: Int) : Int

    /**
     * Đánh dấu thông báo đã đọc
     */
    @Query("UPDATE notifications SET is_read = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int) : Int

    /**
     * Lấy thông báo chưa đọc
     */
    @Query("SELECT * FROM notifications WHERE user_id = :userId AND is_read = 0 ORDER BY created_at DESC")
    fun getUnreadNotifications(userId: Int): Flow<List<Notification>>

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId AND is_read = 0")
    suspend fun markAllAsRead(userId: Int): Int

    /**
     * Xóa tất cả thông báo của user
     */
    @Query("DELETE FROM notifications WHERE user_id = :userId")
    suspend fun clearAllNotifications(userId: Int): Int

    /**
     * Xóa thông báo theo quan hệ (appointment , notes)
     */
    @Query("DELETE FROM notifications WHERE related_id = :relatedId AND notification_type = :type")
    suspend fun deleteNotificationsByRelatedId(relatedId: Int, type: NotificationType): Int

    /**
     * Xóa thông báo đã hết hạn (cũ hơn thời gian chỉ định)
     */
    @Query("DELETE FROM notifications WHERE created_at < :expiredTime")
    suspend fun deleteExpiredNotifications(expiredTime: Long): Int

    /**
     * Đếm số thông báo chưa đọc
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    suspend fun getUnreadCount(userId: Int): Int

    /**
     * Đếm số thông báo đang pending
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND scheduled_time > :currentTime AND is_sent = 0")
    suspend fun getPendingCount(userId: Int, currentTime: Long): Int
}