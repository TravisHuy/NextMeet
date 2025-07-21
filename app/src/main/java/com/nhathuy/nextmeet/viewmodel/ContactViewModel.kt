package com.nhathuy.nextmeet.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.ContactNameId
import com.nhathuy.nextmeet.repository.ContactRepository
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.resource.NoteUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    @ApplicationContext private val context: Context
) :
    ViewModel() {

    private val _contactUiState = MutableStateFlow<ContactUiState>(ContactUiState.Idle)
    val contactUiState: StateFlow<ContactUiState> = _contactUiState

    // Cập nhật StateFlow cho danh sách tên và ID của contacts
    private val _contactNamesAndIds = MutableStateFlow<List<ContactNameId>>(emptyList())
    val contactNamesAndIds: StateFlow<List<ContactNameId>> = _contactNamesAndIds

    /**
     * Tạo liên hệ mới
     */
    fun createContact(contact: Contact) {
        viewModelScope.launch {
            _contactUiState.value = ContactUiState.Loading
            try {
                val result = contactRepository.insertContact(
                    userId = contact.userId,
                    name = contact.name,
                    address = contact.address,
                    phone = contact.phone,
                    email = contact.email,
                    role = contact.role,
                    notes = contact.notes,
                    latitude = contact.latitude,
                    longitude = contact.longitude,
                    isFavorite = contact.isFavorite
                )
                if (result.isSuccess) {
                    _contactUiState.value = ContactUiState.ContactCreated(
                        result.getOrThrow(),
                        context.getString(R.string.contact_created_success)
                    )
                } else {
                    _contactUiState.value =
                        ContactUiState.Error(result.exceptionOrNull()?.message ?: context.getString(R.string.error_unknown))
                }
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: context.getString(R.string.error_creating_contact))
            }
        }
    }

    /**
     * Lấy tất cả liên hệ của người dùng
     */
    fun getAllContacts(userId: Int, searchQuery: String = "", showFavoriteOnly: Boolean = false) {
        viewModelScope.launch {
            _contactUiState.value = ContactUiState.Loading
            try {
                contactRepository.getAllContactsWithFilter(userId, searchQuery, showFavoriteOnly)
                    .collect { contacts ->
                        _contactUiState.value = ContactUiState.ContactsLoaded(contacts)
                    }
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: context.getString(R.string.error_loading_contacts))
            }
        }
    }

    /*  * Toggle pin
    */
    fun toggleFavorite(contactId: Int) {
        viewModelScope.launch {
            contactRepository.toggleFavorite(contactId)
                .onSuccess { isFavorited ->
                    val message =
                        if (isFavorited) context.getString(R.string.contact_favorited) else context.getString(R.string.contact_unfavorited)
                    _contactUiState.value = ContactUiState.FavoriteToggled(isFavorited, message)
                }
                .onFailure { error ->
                    _contactUiState.value =
                        ContactUiState.Error(error.message ?: context.getString(R.string.error_favorited_contact))
                }
        }
    }

    /**
     * Xóa lien hệ theo Id
     */
    fun deleteContact(contactId: Int) {
        viewModelScope.launch {
            _contactUiState.value = ContactUiState.Loading
            try {
                contactRepository.deleteContact(contactId)
                _contactUiState.value =
                    ContactUiState.ContactDeleted(context.getString(R.string.contact_deleted_success))
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: context.getString(R.string.error_deleting_contact))
            }
        }
    }

    /**
     * Lấy danh sách đơn giản của các liên hệ (chỉ ID và tên)
     * Thường dùng cho các dropdown selector
     *
     * @param userId ID của người dùng
     */
    fun getContactNamesAndIds(userId: Int) {
        viewModelScope.launch {
            try {
                contactRepository.getContactNamesAndIds(userId).collect { contacts ->
                    _contactNamesAndIds.value = contacts
                }
            } catch (e: Exception) {
                _contactUiState.value =
                    ContactUiState.Error(e.message ?: context.getString(R.string.error_loading_contact_names))
            }
        }
    }

    /**
     * Lay contact theo Id
     */
    fun getContactById(contactId: Int) {
        viewModelScope.launch {
            _contactUiState.value = ContactUiState.Loading
            try {
                contactRepository.getContactById(contactId).onSuccess { contact ->
                    _contactUiState.value = ContactUiState.ContactsLoaded(listOf(contact))
                }.onFailure { error ->
                    _contactUiState.value = ContactUiState.Error(
                        error.message ?: context.getString(R.string.error_loading_contacts)
                    )
                }
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: context.getString(R.string.error_loading_contacts))
            }
        }
    }

    /**
     * Cap nhat lai contact
     */
    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            _contactUiState.value = ContactUiState.Loading
            try {
                contactRepository.updateContact(contact)
                    .onSuccess {
                        _contactUiState.value =
                            ContactUiState.ContactUpdated(context.getString(R.string.contact_updated_success))
                    }
                    .onFailure { error ->
                        _contactUiState.value = ContactUiState.Error(
                            error.message ?: context.getString(R.string.error_updating_contact)
                        )
                    }
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: context.getString(R.string.error_updating_contact))
            }
        }
    }

    /**
     * Thêm liên hệ nhanh
     */
    suspend fun quickAddContact(
        userId: Int,
        name: String,
        phone: String,
        role: String
    ): Long? {
        return try {
            // Validate input
            if (name.trim().isEmpty()) {
                _contactUiState.value = ContactUiState.Error(context.getString(R.string.error_name_empty))
                return null
            }

            if (phone.trim().isEmpty()) {
                _contactUiState.value = ContactUiState.Error(context.getString(R.string.error_phone_empty))
                return null
            }

            if (role.trim().isEmpty()) {
                _contactUiState.value = ContactUiState.Error(context.getString(R.string.error_role_empty))
                return null
            }

            // Validate phone format (basic validation)
            val phonePattern = "^[+]?[0-9]{10,15}$".toRegex()
            val cleanPhone = phone.trim().replace("\\s".toRegex(), "")
            if (!cleanPhone.matches(phonePattern)) {
                _contactUiState.value = ContactUiState.Error(context.getString(R.string.error_phone_empty))
                return null
            }

            // Kiểm tra xem phone đã tồn tại chưa
            val existingContact = contactRepository.getContactByUserIdAndPhone(userId, cleanPhone)
            existingContact.getOrNull()?.let { existing ->
                _contactUiState.value =
                    ContactUiState.Error(context.getString(R.string.error_phone_exists_with_contact, existing.name))
                return null
            }

            val result =
                contactRepository.quickAddContact(userId, name.trim(), cleanPhone, role.trim())

            if (result.isSuccess) {
                val contactId = result.getOrThrow()

                // Lấy contact vừa tạo để emit state
                val createdContact = contactRepository.getContactById(contactId.toInt())
                if (createdContact.isSuccess) {
                    val createdContact = createdContact.getOrNull()
                    createdContact?.let {
                        _contactUiState.value = ContactUiState.ContactCreated(
                            it.id.toLong(),
                            context.getString(R.string.contact_created_success)
                        )
                    }
                }

                // Refresh contact names and IDs list
                getContactNamesAndIds(userId)

                // Return the contact ID
                contactId
            } else {
                val exception = result.exceptionOrNull()
                val errorMessage = when {
                    exception?.message?.contains("UNIQUE constraint failed") == true ->
                       context.getString(R.string.error_phone_exists)

                    else -> exception?.message ?: context.getString(R.string.error_creating_contact)
                }
                _contactUiState.value = ContactUiState.Error(errorMessage)
                null
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("UNIQUE constraint failed") == true ->
                    context.getString(R.string.error_phone_exists)

                else -> e.message ?: context.getString(R.string.unknown_error_adding_contact)
            }
            _contactUiState.value = ContactUiState.Error(errorMessage)
            null
        }
    }

    /**
     * Reset Ui state ve Idle
     */
    fun resetUiState() {
        _contactUiState.value = ContactUiState.Idle
    }
}