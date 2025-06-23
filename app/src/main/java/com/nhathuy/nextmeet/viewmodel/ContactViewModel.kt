package com.nhathuy.nextmeet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.ContactNameId
import com.nhathuy.nextmeet.repository.ContactRepository
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.resource.NoteUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(private val contactRepository: ContactRepository) :
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
                        "Contact created successfully"
                    )
                } else {
                    _contactUiState.value =
                        ContactUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: "Error creating contact")
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
                _contactUiState.value = ContactUiState.Error(e.message ?: "Error loading contacts")
            }
        }
    }

    /*  * Toggle pin
    */
    fun toggleFavorite(contactId: Int) {
        viewModelScope.launch {
            contactRepository.toggleFavorite(contactId)
                .onSuccess { isFavorited ->
                    val message = if (isFavorited) "Đã pin ghi chú" else "Đã bỏ pin ghi chú"
                    _contactUiState.value = ContactUiState.FavoriteToggled(isFavorited, message)
                }
                .onFailure { error ->
                    _contactUiState.value =
                        ContactUiState.Error(error.message ?: "Lỗi khi pin ghi chú")
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
                    ContactUiState.ContactDeleted("Contact deleted successfully")
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: "Error deleting contact")
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
                    ContactUiState.Error(e.message ?: "Error loading contact names")
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
                        error.message ?: "Error loading contact"
                    )
                }
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: "Error loading contact")
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
                            ContactUiState.ContactUpdated("Contact updated successfully")
                    }
                    .onFailure { error ->
                        _contactUiState.value = ContactUiState.Error(
                            error.message ?: "Error updating contact"
                        )
                    }
            } catch (e: Exception) {
                _contactUiState.value = ContactUiState.Error(e.message ?: "Error updating contact")
            }
        }
    }

    /**
     * Reset Ui state ve Idle
     */
    fun resetUiState() {
        _contactUiState.value = ContactUiState.Idle
    }
}
