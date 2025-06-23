package com.nhathuy.nextmeet.repository

import com.nhathuy.nextmeet.dao.ContactDao
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.ContactNameId
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.utils.ValidationUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository quản lý danh bạ.
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 2025-06-06
 */
@Singleton
class ContactRepository @Inject constructor(private val contactDao: ContactDao) {

    /**
     * Lấy danh bạ theo userId, cho phép tìm kiếm và lọc theo yêu thích.
     * Ưu tiên liên hệ yêu thích, sắp xếp theo thời gian cập nhật mới nhất.
     * @param userId ID người dùng
     * @param searchQuery Từ khóa tìm kiếm (tên/số điện thoại)
     * @param showFavoriteOnly Chỉ lấy liên hệ yêu thích
     * @return Flow danh sách liên hệ đã lọc và sắp xếp
     */

    fun getAllContactsWithFilter(
        userId: Int,
        searchQuery: String = "",
        showFavoriteOnly: Boolean = false
    ): Flow<List<Contact>> {
        return flow {
            try {
                val contactsFlow = when {
                    showFavoriteOnly -> contactDao.getFavoriteContacts(userId)
                    searchQuery.isNotEmpty() -> contactDao.getAllContactsByUser(userId)
                        .map {
                            it.filter { contact ->
                                contact.name.contains(
                                    searchQuery,
                                    ignoreCase = true
                                )
                            }
                        }

                    else -> contactDao.getAllContactsByUser(userId)
                }

                contactsFlow.collect { contacts ->
                    var filterContacts = contacts

                    if (searchQuery.isNotBlank() && !showFavoriteOnly) {
                        filterContacts = filterContacts.filter {
                            it.name.contains(
                                searchQuery,
                                ignoreCase = true || it.phone.contains(
                                    searchQuery,
                                    ignoreCase = true
                                )
                            )
                        }
                    }

                    var sortedContacts = filterContacts.sortedWith(
                        compareByDescending<Contact> {
                            it.isFavorite
                        }.thenByDescending { it.updateAt }
                    )

                    emit(sortedContacts)
                }
            } catch (e: Exception) {
                emit(emptyList())
                throw e
            }
        }
    }


    /**
     * Thêm một liên hệ mới vào cơ sở dữ liệu.
     *
     * @param userId ID của người dùng liên kết với liên hệ.
     * @param name Tên của liên hệ.
     * @param address Địa chỉ của liên hệ.
     * @param phone Số điện thoại của liên hệ.
     * @param email Địa chỉ email của liên hệ.
     * @param role Vai trò của liên hệ.
     * @param notes Ghi chú liên quan đến liên hệ.
     * @param latitude Vĩ độ vị trí của liên hệ.
     * @param longitude Kinh độ vị trí của liên hệ.
     * @param isFavorite Liên hệ có phải là yêu thích không.
     * @return Đối tượng [Result] chứa ID của liên hệ vừa được thêm thành công,
     * hoặc một ngoại lệ nếu thất bại.
     * Ngoại lệ có thể xảy ra:
     *  - [IllegalArgumentException] nếu userId không hợp lệ.
     *  - [IllegalArgumentException] nếu các trường nhập (name, address, phone, email) không hợp lệ.
     */
    suspend fun insertContact(
        userId: Int,
        name: String = "",
        address: String = "",
        phone: String = "",
        email: String = "",
        role: String = "",
        notes: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        isFavorite: Boolean = false
    ): Result<Long> {
        if (userId <= 0) {
            return Result.failure(IllegalArgumentException("User Id không hợp lệ"))
        }

        val validationResult = validateContactInputs(name, address, phone, email)
        if (validationResult.isFailure) {
            return Result.failure(
                IllegalArgumentException(
                    validationResult.exceptionOrNull()?.message ?: "Invalid contact inputs"
                )
            )
        }
        val contact = Contact(
            userId = userId,
            name = name,
            address = address,
            phone = phone,
            email = email,
            role = role,
            notes = notes,
            latitude = latitude,
            longitude = longitude,
            isFavorite = isFavorite,
            createAt = System.currentTimeMillis(),
            updateAt = System.currentTimeMillis()
        )

        val contactId = contactDao.insertContact(contact)
        return Result.success(contactId)
    }

    /**
     * Xác thực các trường đầu vào của liên hệ.
     * @param name Tên liên hệ.
     * @param address Địa chỉ liên hệ.
     * @param phone Số điện thoại liên hệ.
     * @param email Email liên hệ.
     * @return Kết quả xác thực, trả về lỗi nếu có.
     */
    private fun validateContactInputs(
        name: String,
        address: String,
        phone: String,
        email: String
    ): Result<Unit> {
        val validators = listOf(
            ValidationUtils.validateName(name),
            ValidationUtils.validateAddress(address),
            ValidationUtils.validatePhone(phone),
            ValidationUtils.validateEmail(email)
        )

        val error = validators.firstOrNull { !it.isValid }
        return if (error != null) {
            Result.failure(IllegalArgumentException(error.errorMessage ?: "Invalid input"))
        } else {
            Result.success(Unit)
        }
    }

    /**
     * favorite/unfavorite với liên hệ
     */
    suspend fun toggleFavorite(contactId: Int): Result<Boolean> {
        return try {
            val contact = contactDao.getContactById(contactId)
                ?: return Result.failure(IllegalArgumentException("Ghi chú không tồn taại"))

            val newFavoriteStatus = !contact.isFavorite
            contactDao.updateFavoriteStatus(contactId, newFavoriteStatus)
            Result.success(newFavoriteStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa liên hệ theo ID.
     */
    suspend fun deleteContact(contactId: Int): Result<Unit> {
        return try {
            val contact = contactDao.getContactById(contactId) ?: return Result.failure(
                IllegalArgumentException("Liên hệ không tồn tại")
            )

            contactDao.deleteContact(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách đơn giản của liên hệ chỉ gồm ID và tên
     * Hữu ích cho các dropdown selector
     *
     * @param userId ID người dùng
     * @return Flow danh sách các đối tượng ContactNameId
     */
    fun getContactNamesAndIds(userId: Int): Flow<List<ContactNameId>> {
        return contactDao.getContactNamesAndIds(userId)
            .catch { e ->
                emit(emptyList())
                throw e
            }
    }

    /**
     * Cập nhật lại liên hệ
     */
    suspend fun updateContact(contact: Contact): Result<Unit> {
        return try {
            // validate truoc khi input
            val validationResult = validateContactInputs(contact.name,contact.address,contact.phone,contact.email)

            if(validationResult.isFailure){
                return Result.failure(
                    IllegalArgumentException(
                        validationResult.exceptionOrNull()?.message ?: "Invalid contact inputs"
                    )
                )
            }

            // update time
            val updatedContact = contact.copy(updateAt = System.currentTimeMillis())
            val result = contactDao.updateContact(updatedContact)

            if (result > 0) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Contact not found"))
            }
        }
        catch (e:Exception){
            Result.failure(e)
        }
    }

    /**
     * Lấy contact theo ID
     */
    suspend fun getContactById(contactId: Int): Result<Contact> {
        return try {
            val contact = contactDao.getContactById(contactId)
            if (contact != null) {
                Result.success(contact)
            } else {
                Result.failure(IllegalArgumentException("Contact not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
