package com.nhathuy.nextmeet.resource

import com.nhathuy.nextmeet.model.Contact

sealed class ContactUiState {
    object Idle: ContactUiState()
    object Loading: ContactUiState()
    data class ContactCreated(val contactId: Long, val message: String): ContactUiState()
    data class ContactsLoaded(val contacts: List<Contact>): ContactUiState()
    data class FavoriteToggled(val isFavorite:Boolean , val message:String) : ContactUiState()
    data class ContactDeleted(val message: String): ContactUiState()
    data class Error(val message:String): ContactUiState()
}

