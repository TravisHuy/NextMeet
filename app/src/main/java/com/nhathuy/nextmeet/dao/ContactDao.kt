package com.nhathuy.nextmeet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    /**
     * Thêm liên hệ mới vào cơ sơ dữ liệu.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    /**
     * Lấy liên hệ theo ID
     */
    @Query("SELECT * FROM contacts WHERE id =:contactId")
    suspend fun getContactById(contactId: Int) : Contact?

    /**
     * Lấy tất cả liên hệ cua người dùng, sắp xếp theo thời gian và yêu thích.
     */
    @Query(
        """
        SELECT * FROM contacts
        WHERE user_id = :userId
        ORDER BY is_favorite DESC, updated_at DESC
    """
    )
    fun getAllContactsByUser(userId: Int): Flow<List<Contact>>

    /**
     * Lấy contact yeu thich
     */
    @Query("""
        SELECT * FROM contacts
        WHERE user_id =:userId AND is_favorite =1
        ORDER BY updated_at DESC
    """)
    fun getFavoriteContacts(userId: Int): Flow<List<Contact>>

    /**
     * Cập nhật trạng thái favorite
     */
    @Query("""
        UPDATE contacts
        SET is_favorite = :isFavorited , updated_at =:updatedAt
        WHERE id = :contactId
    """)
    suspend fun updateFavoriteStatus(contactId:Int, isFavorited:Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * Xoa liên hệ theo Id
     */
    @Delete
    suspend fun deleteContact(contact: Contact)
}

