package com.nhathuy.nextmeet.repository

import com.nhathuy.nextmeet.dao.NotificationDao
import com.nhathuy.nextmeet.model.Notification
import com.nhathuy.nextmeet.model.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(private val notificationDao: NotificationDao) {

    /**
     * lưu thông báo
     */
    suspend fun insertNotification(notification: Notification) : Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val id = notificationDao.insertNotification(notification)
                Result.success(id)
            }
            catch(e: Exception){
                Result.failure(e)
            }
        }
    }

    /**
     * Cập nhật thông báo
     */
    suspend fun updateNotification(notification: Notification) : Result<Boolean>{
        return withContext(Dispatchers.IO) {
            try {
                notificationDao.updateNotification(notification)
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Xoa thông báo theo id
     */
    suspend fun deleteNotification(notificationId: Int) : Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val notification = notificationDao.getNotificationById(notificationId)
                    ?: return@withContext Result.failure(IllegalArgumentException("Thông báo không tồn tại"))

                notificationDao.deleteNotification(notification)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Xoa thông báo theo id
     */
    suspend fun deleteNotificationById(notificationId: Int) : Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val rowsAffected = notificationDao.deleteNotificationById(notificationId)
                Result.success(rowsAffected > 0)
            }
            catch (e: Exception){
                Result.failure(e)
            }
        }
    }

    /**
     * Lay thông báo theo id
     */
    suspend fun getNotificationById(notificationId: Int) : Result<Notification?> {
        return withContext(Dispatchers.IO) {
            try {
                val notification = notificationDao.getNotificationById(notificationId)
                Result.success(notification)
            }
            catch (e: Exception){
                Result.failure(e)
            }
        }
    }

    /**
     * Lay thong bao user id
     */
    fun getNotificationsByUserId(userId: Int) : Flow<List<Notification>> {
        return notificationDao.getNotificationsByUserId(userId).catch {
            emit(emptyList())
        }
    }

    /**
     * Lay thong bao dua moi lien he
     */
    fun getNotificationsByRelatedId(relatedId:Int, type: NotificationType) : Flow<List<Notification>> {
        return notificationDao.getNotificationsByRelatedId(relatedId, type).catch {
            emit(emptyList())
        }
    }

    /**
     * Lấy thông báo đang pending - Simplified version
     */
    fun getPendingNotifications(userId: Int) : Flow<List<Notification>> {
        return flow {
            val currentTime = System.currentTimeMillis()
            notificationDao.getPendingNotifications(userId, currentTime).collect { notifications ->
                emit(notifications)
            }
        }.catch {
            emit(emptyList())
        }
    }

    /**
     * Lấy lịch sử thông báo
     */
    fun getNotificationHistory(userId: Int): Flow<List<Notification>> {
        return notificationDao.getNotificationHistory(userId).catch {
            emit(emptyList())
        }
    }

    /**
     * Mark notification as sent
     */
    suspend fun markAsSent(notificationId: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                notificationDao.markAsSent(notificationId) > 0
            }
        }

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                notificationDao.markAsRead(notificationId) > 0
            }
        }

    /**
     * Lấy thong bao chua doc
     */
    fun getUnreadNotifications(userId: Int) : Flow<List<Notification>> =
        notificationDao.getUnreadNotifications(userId).catch {
            emit(emptyList())
        }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    suspend fun markAllAsRead(userId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val rowsAffected = notificationDao.markAllAsRead(userId)
                Result.success(rowsAffected > 0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Xóa tất cả thông báo của user
     */
    suspend fun clearAllNotifications(userId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val rowsAffected = notificationDao.clearAllNotifications(userId)
                Result.success(rowsAffected > 0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Xóa thong bao theo relatedId
     */
    suspend fun deleteNotificationsByRelatedId(relatedId: Int, type: NotificationType): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val rowsAffected = notificationDao.deleteNotificationsByRelatedId(relatedId, type)
                Result.success(rowsAffected > 0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Xóa thông báo đã hết hạn (30 ngày trước)
     */
    suspend fun deleteExpiredNotifications(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                val rowsAffected = notificationDao.deleteExpiredNotifications(thirtyDaysAgo)
                Result.success(rowsAffected > 0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Đếm số thông báo chưa đọc
     */
    suspend fun getUnreadCount(userId: Int): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val count = notificationDao.getUnreadCount(userId)
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Đếm số thông báo đang pending
     */
    suspend fun getPendingCount(userId: Int): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val count = notificationDao.getPendingCount(userId, currentTime)
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}