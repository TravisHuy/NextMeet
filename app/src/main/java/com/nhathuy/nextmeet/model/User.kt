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
 * @property defaultAddress địa chỉ mặt đinh của người dùng
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
    @ColumnInfo(name = "default_address")
    val defaultAddress: String = "",
    @ColumnInfo(name = "default_latitude")
    val defaultLatitude: Double? = null,
    @ColumnInfo(name = "default_longitude")
    val defaultLongitude: Double? = null,
    @ColumnInfo(name = "notification_enabled")
    val notificationEnabled: Boolean = true,
    @ColumnInfo(name = "default_reminder_minutes")
    val defaultReminderMinutes : Int = 15,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updateAt: Long = System.currentTimeMillis()
)
