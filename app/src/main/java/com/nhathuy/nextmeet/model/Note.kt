package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho ghi chú những ghi chú trong ứng dụng
 *
 * @property id Id tự động tăng của id với note
 * @property userId của người dùng sở hữu cuộc hẹn này (khóa ngoại đến bảng User).
 * @property title Tiêu đề của ghi chú
 * @property content Nội dung của ghi chu
 * @property noteType Loại ghi chú ví dụ như văn bản, hình ảnh , video..
 * @property color Màu nền của ghi chú
 * @property isPinned Kiểm tra có pin không
 * @property isArchived Kiểm tra có lưu trữ không
 * @property reminderTime Thời gian nhắc nhỡ
 * @property checkListItems Danh sách công việc (lưu json)
 * @property imagePaths Đường dẫn các ảnh (lưu dạng chuỗi phân cách)
 * @property createdAt Thời gian tạo
 * @property updatedAt Thời gian update
 *
 * @version 2.0
 * @author TravisHuy (Ho Nhat Huy)
 * @since 27/05/2025
 */
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("user_id"),Index("is_pinned"),Index("is_shared")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    @ColumnInfo(name = "user_id")
    val userId : Int,
    @ColumnInfo(name = "title")
    val title : String = "",
    @ColumnInfo(name = "content")
    val content: String = "",
    @ColumnInfo(name = "note_type")
    val noteType: NoteType = NoteType.TEXT,
    @ColumnInfo(name = "color")
    val color: String = "color_white",
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "is_shared")
    val isShared: Boolean = false,
    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null,
    @ColumnInfo(name = "check_list_items")
    val checkListItems : String? = null,
    @ColumnInfo(name = "image_paths")
    val imagePaths: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
