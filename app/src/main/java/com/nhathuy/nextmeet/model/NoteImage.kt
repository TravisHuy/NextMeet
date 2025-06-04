package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho ảnh của ghi chú (1 ghi chú có thể có nhiều ảnh)
 * @property id Id tự động tăng của ảnh
 * @property noteId Id của ghi chú (khóa ngoại đến bảng Note)
 * @property imagePath Đường dẫn hoặc uri của ảnh
 *
 * @since 03/06/2025
 * @author TravisHuy (Ho Nhat Huy)
 * @version 2.0
 */
@Entity(
    tableName = "note_images",
    foreignKeys = [ForeignKey(
        entity = Note::class,
        parentColumns = ["id"],
        childColumns = ["note_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("note_id")]
)
data class NoteImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "note_id")
    val noteId: Int,
    @ColumnInfo(name = "image_path")
    val imagePath: String
)

