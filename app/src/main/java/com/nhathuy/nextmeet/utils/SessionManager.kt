package com.nhathuy.nextmeet.utils

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quản lý cho xử lý dùng cho lưu cụ bộ với thông tin login
 *
 * Đây là lớp dùng để thông tin đăng nhap của người dùng khi click remeber me
 *
 * @author TravisHuy(Ho Nhat Huy)
 * @since 21.05.2025
 */
@Singleton
class SessionManager @Inject constructor(context: Context) {

    companion object {
        private const val TAG = "SessionManager"
        private const val PREF_NAME = "NextMeetPrefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_PHONE = "user_phone"
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * lưu thông người dùng vào session
     *
     * @param userId UserId của người dùng
     * @param rememberMe RememberMe có nên duy trì đăng nhập khi khởi động lại ứng dụng không
     *
     */
    fun createLoginSession(userId: Int, rememberMe: Boolean, phone: String = "") {
        with(sharedPreferences.edit()) {
            putInt(KEY_USER_ID, userId)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            putBoolean(KEY_IS_LOGGED_IN, true)

            if (rememberMe && phone.isNotEmpty()) {
                putString(KEY_USER_PHONE, phone)
            }
            apply()
        }
    }

    /**
     *  Xóa dữ liệu người dùng ra khỏi session khi đăng xuất
     */
    fun logout() {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, false)
            if (!isRememberMeEnable()) {
                remove(KEY_USER_ID)
                remove(KEY_USER_PHONE)
            }
            apply()
        }
    }

    /**
     * Kiểm tra người dùng có đăng nhập chưa
     *
     * @return true Nếu người dùng đã đăng nhập, false khi người dùng đăng xuất
     */
    fun isLoggedIn(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = sharedPreferences.getInt(KEY_USER_ID,-1)

        Log.d(TAG, "isLoggedIn: $isLoggedIn, userId: $userId")
        return isLoggedIn && userId > 0
    }

    /**
     * Kiểm tra id của user đã logged chưa
     */
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    /**
     * Kiểm tra có nhấn nút remember me ko
     */
    fun isRememberMeEnable(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }

    /**
     * Lưu lại khi nhấn nút remember me
     *
     * @param enabled Whether to enable Remember Me
     */
    fun setRememberMe(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply()
    }

    /**
     * Lưu số điện thoại của người dùng
     *
     * @param phone Số điện thoại người dùng
     */
    fun saveUserPhone(phone: String) {
        sharedPreferences.edit().putString(KEY_USER_PHONE, phone).apply()
    }

    /**
     * Lấy số điện thoại đã lưu
     *
     * @return Số điện thoại người dùng đã lưu
     */
    fun getUserPhone(): String {
        return sharedPreferences.getString(KEY_USER_PHONE, "") ?: ""
    }
}