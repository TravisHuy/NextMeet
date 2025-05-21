package com.nhathuy.nextmeet.utils

import android.content.Context
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
        private const val PREF_NAME = "NextMeetPrefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE)

    /**
     * lưu thông người dùng vào session
     *
     * @param userId UserId của người dùng
     * @param rememberMe RememberMe có nên duy trì đăng nhập khi khởi động lại ứng dụng không
     *
     */
    fun createLoginSession(userId:Int, rememberMe:Boolean){
        with(sharedPreferences.edit()){
            putInt(KEY_USER_ID,userId)
            putBoolean(KEY_REMEMBER_ME,rememberMe)
            putBoolean(KEY_IS_LOGGED_IN,true)

            apply()
        }
    }
    /**
     *  Xóa dữ liệu người dùng ra khỏi session khi đăng xuất
     */
    fun logout(){
        with(sharedPreferences.edit()){
            putBoolean(KEY_IS_LOGGED_IN,false)
            if(!isRememberMeEnable()){
                remove(KEY_USER_ID)
            }
            apply()
        }
    }
    /**
     * Kiểm tra người dùng có đăng nhập chưa
     *
     * @return true Nếu người dùng đã đăng nhập, false khi người dùng đăng xuất
     */
    fun isLoggedIn():Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN,false)
    }

    /**
     * Kiểm tra id của user đã logged chưa
     */
    fun getUserId():Int{
        return sharedPreferences.getInt(KEY_USER_ID,-1)
    }

    /**
     * Kiểm tra có nhấn nút remember me ko
     */
    fun isRememberMeEnable():Boolean{
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME,false)
    }

    /**
     * Lưu lại khi nhấn nút remember me
     *
     * @param enabled Whether to enable Remember Me
     */
    fun setRememberMe(enabled:Boolean){
        sharedPreferences.edit().putBoolean(KEY_REMEMBER_ME,enabled).apply()
    }
}