package com.nhathuy.nextmeet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import kotlinx.coroutines.flow.Flow

/**
 * Dao cho note
 *
 * @version 2.0
 * @author TravisHuy (Ho Nhat Huy)
 * @since 29/05/2025
 */
@Dao
interface NoteDao {

    /**
     * Lấy tất cả ghi chú cua user, sắp xếp theo pinned và thời gian updated
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id =:userId
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun getAllNotesByUser(userId:Int) :Flow<List<Note>>

    /**
     * Lấy ghi chú theo ID
     */
    @Query("SELECT * FROM notes WHERE id =:noteId")
    suspend fun getNoteById(noteId: Int) : Note?

    /**
     * Lấy ghi chú đã pin
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id =:userId AND is_pinned =1
        ORDER BY updated_at DESC
    """)
    fun getPinnedNotes(userId:Int) : Flow<List<Note>>

    /**
     * Lấy ghi chú đã share
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id =:userId AND is_shared =1
        ORDER BY updated_at DESC
    """)
    fun getSharedNotes(userId:Int) : Flow<List<Note>>

    /**
     * Tìm kiếm ghi chú theo title hoặc content
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id = :userId
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun searchNotes(userId: Int,query:String):Flow<List<Note>>

    /**
     * Lấy ghi chu theo loại
     */
    @Query("""
        SELECT * FROM notes 
        WHERE user_id = :userId AND note_type = :noteType
        ORDER BY is_pinned DESC , updated_at DESC
    """)
    fun getNotesByType(userId: Int,noteType: NoteType):Flow<List<Note>>

    /**
     * Lấy ghi chú có reminder
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id =:userId AND reminder_time IS NOT NULL
        ORDER BY reminder_time ASC
    """)
    fun getNotesWithReminder(userId: Int) : Flow<List<Note>>

    /**
     * Thêm ghi chú mới
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note:Note) :Long

    /**
     * Cập nhật ghi chú
     */
    @Update
    suspend fun updateNote(note:Note)

    /**
     * Xóa ghi chú
     */
    @Delete
    suspend fun deleteNote(note:Note)

    /**
     * Xoa ghi chú theo Id
     */
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)

    /**
     * Cập nhật trạng thái pin
     */
    @Query("""
        UPDATE notes
        SET is_pinned = :isPinned , updated_at =:updatedAt
        WHERE id = :noteId
    """)
    suspend fun updatePinStatus(noteId:Int, isPinned:Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * Cập nhật trạng thái share
     */
    @Query("""
        UPDATE notes
        SET is_shared = :isShared , updated_at =:updatedAt
        WHERE id = :noteId
    """)
    suspend fun updateShareStatus(noteId:Int , isShared: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * Cập nhật màu sắc ghi chú
     */
    @Query("""
        UPDATE notes
        SET color =:color , updated_at = :updatedAt 
        WHERE id = :noteId
    """)
    suspend fun updateNoteColor(noteId:Int, color:String,updatedAt: Long = System.currentTimeMillis())

    /**
     * Cập nhật reminder
     */
    @Query("""
        UPDATE notes
        SET reminder_time = :reminderTime,
        updated_at =  :updatedAt
        WHERE id = :noteId
    """)
    suspend fun updateReminder(noteId: Int, reminderTime:Long? , updatedAt: Long = System.currentTimeMillis())

    /**
     * Xóa tất cả ghi chú của người dùng
     */
    @Query("DELETE FROM notes WHERE user_id = :userId")
    suspend fun deleteAllNotesByUser(userId: Int)

    /**
     * Đếm số lượng ghi chú của user
     */
    @Query("SELECT COUNT(*) FROM notes WHERE user_id = :userId")
    suspend fun getNoteCount(userId: Int) : Int

    /**
     * Lấy ghi chú được cập nhật gần đây
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id = :userId
        ORDER BY updated_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentNotes(userId: Int,limit:Int=10):List<Note>

    /**
     * Tìm kiếm ghi chú với ưu tiên kết quả
     * - Kết quả pin sẽ được ưu tiên hàng đầu
     * - Kết quả bắt đầu bằng từ khóa sẽ được ưu tiên hơn kết quả chứa từ khóa
     */
    @Query("""
        SELECT * FROM notes 
        WHERE user_id = :userId 
        AND (title LIKE '%' || :query || '%' 
             OR content LIKE '%' || :query || '%')
        ORDER BY 
            CASE WHEN is_pinned = 1 THEN 0 ELSE 1 END,
            CASE 
                WHEN title LIKE :query || '%' THEN 1
                WHEN title LIKE '%' || :query || '%' THEN 2
                ELSE 3
            END,
            updated_at DESC
    """)
    fun searchNotePlus(userId: Int, query: String): Flow<List<Note>>

    /**
     * Lấy danh sách gợi ý tiêu đề ghi chú dựa trên từ khóa
     * @param userId ID người dùng
     * @param query Từ khóa để tìm kiếm tiêu đề
     * @param limit Số lượng gợi ý tối đa (mặc định là 5)
     * @return Danh sách các tiêu đề ghi chú phù hợp
     */
    @Query("SELECT DISTINCT title FROM notes WHERE user_id = :userId AND title != '' AND title LIKE :query || '%' ORDER BY title LIMIT :limit")
    fun getTitleSuggestions(userId: Int, query: String, limit: Int = 5): Flow<List<String>>


    /**
     * Lấy ghi chú hôm nay
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id = :userId
        AND DATE(created_at/1000, 'unixepoch') = DATE('now')
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun getTodayNotes(userId: Int): Flow<List<Note>>

    /**
     * Lấy ghi chú là checklist
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id = :userId 
        AND note_type = "CHECKLIST"
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun getChecklistNotes(userId: Int): Flow<List<Note>>

    /**
     * Lấy ghi chú gần đây (Flow version)
     */
    @Query("""
        SELECT * FROM notes
        WHERE user_id = :userId
        ORDER BY updated_at DESC
        LIMIT :limit
    """)
    fun getRecentNotesFlow(userId: Int, limit: Int = 10): Flow<List<Note>>

}