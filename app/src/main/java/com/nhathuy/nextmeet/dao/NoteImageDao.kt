package com.nhathuy.nextmeet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhathuy.nextmeet.model.NoteImage
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho thao tác với bảng NoteImage
 *
 *
 * @author TravisHuy (Ho Nhat Huy)
 * @since 03/06/2025
 * @version 2.0
 */
@Dao
interface NoteImageDao {
    // Thêm 1 hoặc nhiều ảnh cho note
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<NoteImage>)

    // Lấy tất cả ảnh của 1 note
    @Query("SELECT * FROM note_images WHERE note_id = :noteId")
    suspend fun getImagesForNote(noteId: Int): List<NoteImage>

    // Xóa 1 ảnh
    @Delete
    suspend fun deleteImage(image: NoteImage)

    // Xóa tất cả ảnh của 1 note
    @Query("DELETE FROM note_images WHERE note_id = :noteId")
    suspend fun deleteImagesByNoteId(noteId: Int)

    // Lấy tất cả cac ảnh
    @Query("SELECT image_path FROM note_images")
    fun getAllImagePaths() : Flow<List<String>>
}

