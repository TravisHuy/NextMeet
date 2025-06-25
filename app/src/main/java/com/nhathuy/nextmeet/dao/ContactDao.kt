package com.nhathuy.nextmeet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.ContactNameId
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
    suspend fun getContactById(contactId: Int): Contact?

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
    @Query(
        """
        SELECT * FROM contacts
        WHERE user_id =:userId AND is_favorite =1
        ORDER BY updated_at DESC
    """
    )
    fun getFavoriteContacts(userId: Int): Flow<List<Contact>>

    /**
     * Cập nhật trạng thái favorite
     */
    @Query(
        """
        UPDATE contacts
        SET is_favorite = :isFavorited , updated_at =:updatedAt
        WHERE id = :contactId
    """
    )
    suspend fun updateFavoriteStatus(
        contactId: Int,
        isFavorited: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Xoa liên hệ theo Id
     */
    @Delete
    suspend fun deleteContact(contact: Contact)

    /**
     * Lấy danh sách tên và ID của tất cả liên hệ của một người dùng
     * @param userId ID của người dùng
     * @return Flow chứa danh sách các đối tượng ContactNameId
     */
    @Query(
        """
        SELECT id, name FROM contacts
        WHERE user_id = :userId
        ORDER BY name ASC
    """
    )
    fun getContactNamesAndIds(userId: Int): Flow<List<ContactNameId>>

    /**
     * Cập nhật thông tin contact
     */
    @Update
    suspend fun updateContact(contact: Contact): Int

    /**
     * Cap nhat lại contact
     */
    @Query("""
            UPDATE contacts
            SET name = :name,
            phone = :phone,
            email = :email,
            role = :role,
            address = :address,
            notes  = :notes,
            latitude = :latitude,
            latitude = :latitude, 
            longitude = :longitude, 
            is_favorite = :isFavorite, 
            updated_at = :updatedAt
            
            WHERE id = :contactId
    """)
    suspend fun updateContactDetail(
        contactId: Int,
        name: String,
        phone: String,
        email: String,
        role: String,
        address: String,
        notes: String,
        latitude: Double?,
        longitude: Double?,
        isFavorite: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    ) : Int

    /**
     * tìm kiếm liên hệ theo tên, số điện thoại, email, địa chỉ, vai trò và ghi chú
     */
    @Query(
        """
        SELECT * FROM contacts
        WHERE user_id = :userId
        AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'
            OR email LIKE '%' || :query || '%' 
             OR address LIKE '%' || :query || '%'
             OR role LIKE '%' || :query || '%'
             OR notes LIKE '%' || :query || '%')
        ORDER BY 
        CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END,
         CASE 
        WHEN name LIKE :query || '%' THEN 1
        WHEN name LIKE '%' || :query || '%' THEN 2
        ELSE 3 END,
        name ASC
        """
    )
    fun searchContacts(userId: Int, query: String): Flow<List<Contact>>

    /**
     * Lấy liên hệ đề xuất tối đa theo tên
     */
    @Query("SELECT DISTINCT name FROM contacts WHERE user_id = :userId AND name LIKE :query || '%' ORDER BY name LIMIT :limit")
    fun getNameSuggestions(userId: Int, query:String, limit : Int =  5): Flow<List<String>>

    /**
     * Lấy liên hệ đề xuất tối đa theo địa chỉ
     */
    @Query("SELECT DISTINCT address FROM contacts WHERE user_id = :userId AND address != '' AND address LIKE :query || '%' ORDER BY address LIMIT :limit")
    fun getAddressSuggestions(userId: Int, query: String, limit: Int = 5): Flow<List<String>>

    /**
     * Lấy liện hệ đề xuất tối đa theo vai trò
     */
    @Query("SELECT DISTINCT role FROM contacts WHERE user_id = :userId AND role != '' AND role LIKE :query || '%' ORDER BY role LIMIT :limit")
    fun getRoleSuggestions(userId: Int, query: String, limit: Int = 5): Flow<List<String>>


    /**
     * Lấy liên hệ có số điện thoại
     */
    @Query("""
        SELECT * FROM contacts
        WHERE user_id = :userId AND phone != '' AND phone IS NOT NULL
        ORDER BY is_favorite DESC, updated_at DESC
    """)
    fun getContactsWithPhone(userId: Int): Flow<List<Contact>>

    /**
     * Lấy liên hệ có email
     */
    @Query("""
        SELECT * FROM contacts
        WHERE user_id = :userId AND email != '' AND email IS NOT NULL
        ORDER BY is_favorite DESC, updated_at DESC
    """)
    fun getContactsWithEmail(userId: Int): Flow<List<Contact>>

    /**
     * Lấy liên hệ có địa chỉ
     */
    @Query("""
        SELECT * FROM contacts
        WHERE user_id = :userId AND address != '' AND address IS NOT NULL
        ORDER BY is_favorite DESC, updated_at DESC
    """)
    fun getContactsWithAddress(userId: Int): Flow<List<Contact>>

    /**
     * Lấy liên hệ gần đây (được tạo hoặc cập nhật gần đây)
     */
    @Query("""
        SELECT * FROM contacts
        WHERE user_id = :userId
        ORDER BY updated_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentContacts(userId: Int, limit: Int = 10): List<Contact>

    /**
     * Lấy liên hệ có sđt
     */
    @Query("SELECT * FROM contacts WHERE user_id = :userId AND phone = :phone LIMIT 1")
    suspend fun getContactByUserIdAndPhone(userId: Int, phone: String): Contact?

}
