package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho lịch sử các địa điểm mà người dùng đã ghé thăm.
 *
 * Mỗi bản ghi tương ứng với một địa điểm cụ thể kèm theo thông tin người dùng,
 * số lần ghé thăm và thời gian ghé thăm gần nhất.
 *
 * @property id ID tự động tăng, là khóa chính của bảng.
 * @property userId ID của người dùng (khóa ngoại liên kết đến bảng User).
 * @property locationName Tên địa điểm mà người dùng đã ghé thăm.
 * @property address Địa chỉ cụ thể của địa điểm.
 * @property latitude Vĩ độ của địa điểm.
 * @property longitude Kinh độ của địa điểm.
 * @property visitCount Số lần người dùng đã ghé thăm địa điểm này (mặc định là 1).
 * @property lastVisited Thời gian lần gần nhất người dùng ghé thăm địa điểm (tính bằng milliseconds).
 * @property createdAt Thời điểm bản ghi được tạo (tính bằng milliseconds).
 */
@Entity(
    tableName = "location_history",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId"), Index("locationName")]
)
data class LocationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "location_name")
    val locationName: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "visit_count")
    val visitCount: Int = 1,

    @ColumnInfo(name = "last_visited")
    val lastVisited: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
