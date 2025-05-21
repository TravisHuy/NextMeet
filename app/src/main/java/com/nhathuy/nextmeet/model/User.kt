package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity đại điện cho thông tin của user
 *
 * @property id Khóa chính tự động tăng của người dùng
 * @property name Tên của người dùng
 * @property email Email của người dùng
 * @property phone Số điện thoại của người dùng
 * @property password mật khẩu của nguời dùng
 * @property isLoggedIn kiểm tra người dùng có đăng nhập chưa
 * @property defaultLatitude vĩ độ ban đầu của người dùng
 * @property defaultLongitude kinh độ ban đầu của nguoi dùng.
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 16.05.2025
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String = "",
    @ColumnInfo(name = "email")
    val email: String = "",
    @ColumnInfo(name = "phone")
    val phone: String = "",
    @ColumnInfo(name = "password")
    val password: String = "",
    @ColumnInfo(name = "default_latitude")
    val defaultLatitude: Double? = null,
    @ColumnInfo(name = "default_longitude")
    val defaultLongitude: Double? = null,
    @ColumnInfo(name = "is_logged_in")
    var isLoggedIn: Int = -1,
)
