package com.nhathuy.nextmeet.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.dao.UserDao
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.utils.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kho lưu trữ để xử lý các hoạt động liên quan đến người dùng.
 *
 * Lớp này đóng vai trò là trung gian giữa ViewModel và các nguồn dữ liệu (cơ sở dữ liệu cục bộ).
 * Nó cũng xử lý việc quản lý phiên thông qua SessionManager.
 *
 * @property userDao Data Access Object cho các hoạt động cơ sở dữ liệu người dùng
 * @property sessionManager Trình quản lý để xử lý phiên xác thực người dùng
 *
 * @author TravisHuy(Ho Nhat Huy)
 * @since 16.05.2025
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val context: Context
    ) {

    suspend fun register(user: User): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (userDao.isPhoneExists(user.phone)) {
                emit(Resource.Error(context.getString(R.string.phone_already_registered)))
                return@flow
            }
            val userId = userDao.register(user)
            emit(Resource.Success(userId > 0))
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.registration_faileds, e.message)))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun login(phone: String, password: String, rememberMe: Boolean): Flow<Resource<User>> =
        flow {
            emit(Resource.Loading())
            try {
                val user = userDao.login(phone, password)

                if (user != null) {
                    sessionManager.createLoginSession(user.id, rememberMe, phone)
                    emit(Resource.Success(user))
                } else {
                    emit(Resource.Error(context.getString(R.string.invalid_credentials)))
                }
            } catch (e: Exception) {
                emit(Resource.Error(context.getString(R.string.invalid_phone_or_password)))
            }
        }.flowOn(Dispatchers.IO)

    /**
     * Tạo phiên đăng nhập mới
     */
    fun createLoginSession(userId: Int, rememberMe: Boolean, phone: String) {
        sessionManager.createLoginSession(userId, rememberMe, phone)
    }

    suspend fun updateUser(user: User): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            // kiểm tra số điện thoại đã tồn tại cho người dùng khác
            if (userDao.isPhoneExistsForOtherUser(user.phone, user.id)) {
                emit(Resource.Error(context.getString(R.string.phone_used_by_another_user)))
                return@flow
            }
            userDao.updateUser(user)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.update_failed, e.message)))
        }
    }.flowOn(Dispatchers.IO)

    fun getCurrentUser(): LiveData<User?> {
        val userId = sessionManager.getUserId()
        return if (userId != -1 && sessionManager.isLoggedIn()) {
            userDao.getUserById(userId)
        } else {
            object : LiveData<User?>() {
                init {
                    value = null
                }
            }
        }
    }

    suspend fun logout(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            sessionManager.logout()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.logout_faileds,e.message)))
        }
    }

    suspend fun updatePassword(phone: String, newPassword: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val rowsUpdated = userDao.updatePassword(phone, newPassword)
            if (rowsUpdated > 0) {
                emit(Resource.Success(true))
            }
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.update_password_failed, e.message)))
        }
    }

    fun isRememberMeEnabled(): Boolean {
        return sessionManager.isRememberMeEnable()
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    fun setRememberMe(enabled: Boolean) {
        sessionManager.setRememberMe(enabled)
    }

    /**
     * Lấy ID người dùng từ session
     */
    fun getUserId(): Int {
        return sessionManager.getUserId()
    }

    /**
     * Lưu số điện thoại người dùng
     */
    fun saveUserPhone(phone: String) {
        sessionManager.saveUserPhone(phone)
    }

    /**
     * Lấy số điện thoại đã lưu
     */
    fun getUserPhone(): String {
        return sessionManager.getUserPhone()
    }

    /**
     * Kiểm tra số điện thoại đã tồn tại chưa
     */
    suspend fun isPhoneExists(phone: String): Boolean {
        return userDao.isPhoneExists(phone)
    }
}