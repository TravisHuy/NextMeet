package com.nhathuy.nextmeet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.repository.NoteRepository
import com.nhathuy.nextmeet.resource.FilterState
import com.nhathuy.nextmeet.resource.NoteUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho note
 *
 * @version 2.0
 * @since 29/05/2025
 * @author TravisHuy(Ho Nhat Huy)
 */
@HiltViewModel
class NoteViewModel @Inject constructor(private val noteRepository: NoteRepository) : ViewModel(){

    private val _uiState = MutableStateFlow<NoteUiState>(NoteUiState.Idle)
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState : StateFlow<FilterState> = _filterState.asStateFlow()

    /**
     * Lấy tất cả ghi chú với filter
     */
    fun getAllNotes(userId:Int) : Flow<List<Note>>{
        return combine(
            _filterState,
            noteRepository.getAllNotesWithFilter(
                userId = userId,
                searchQuery = _filterState.value.searchQuery,
                noteType = _filterState.value.selectedNoteType,
                showPinnedOnly = _filterState.value.showPinnedOnly,
                showSharedOnly = _filterState.value.showSharedOnly
            )
        ){
            _,notes ->
            _uiState.value = NoteUiState.NotesLoaded(notes)
            notes
        }.catch {
            error ->
            _uiState.value = NoteUiState.Error(error.message ?: "Unknow error")
            emit(emptyList())
        }
    }


    /**
     * Lấy ghi chú theo ID
     */
    fun getNoteById(noteId: Int) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading

            noteRepository.getNoteById(noteId)
                .onSuccess { note ->
                    _uiState.value = if (note != null) {
                        NoteUiState.NoteLoaded(note)
                    } else {
                        NoteUiState.Error("Ghi chú không tồn tại")
                    }
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi tải ghi chú")
                }
        }
    }

    /**
     * Tạo ghi chú mới
     */

    fun createNote(
        userId: Int,
        title: String = "",
        content: String ="",
        noteType: NoteType = NoteType.TEXT,
        color :String = "#ffffff",
        checkListItems : String? = null
    ){
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading

            noteRepository.createNote(userId, title, content, noteType, color, checkListItems)
                .onSuccess { noteId ->
                    _uiState.value = NoteUiState.NoteCreated(noteId, "Ghi chú đã được tạo thành công")
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi tạo ghi chú")
                }
        }
    }

    /**
     * Cập nhật ghi chú
     */
    fun updateNote(
        noteId: Int,
        title: String? = null,
        content: String? = null,
        noteType: NoteType? = null,
        color: String? = null,
        checkListItems: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading

            noteRepository.updateNote(noteId, title, content, noteType, color, checkListItems)
                .onSuccess {
                    _uiState.value = NoteUiState.NoteUpdated("Ghi chú đã được cập nhật")
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi cập nhật ghi chú")
                }
        }
    }

    /**
    * Xóa ghi chú
    */
    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading

            noteRepository.deleteNote(noteId)
                .onSuccess {
                    _uiState.value = NoteUiState.NoteDeleted("Ghi chú đã được xóa")
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi xóa ghi chú")
                }
        }
    }

    /**
     * Xóa nhiều ghi chú
     */
    fun deleteMultipleNotes(noteIds: List<Int>) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading

            noteRepository.deleteMultipleNotes(noteIds)
                .onSuccess { deletedCount ->
                    _uiState.value = NoteUiState.MultipleNotesDeleted(
                        deletedCount,
                        "Đã xóa $deletedCount ghi chú"
                    )
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi xóa ghi chú")
                }
        }
    }
    /*  * Toggle pin
     */
    fun togglePin(noteId: Int) {
        viewModelScope.launch {
            noteRepository.togglePin(noteId)
                .onSuccess { isPinned ->
                    val message = if (isPinned) "Đã pin ghi chú" else "Đã bỏ pin ghi chú"
                    _uiState.value = NoteUiState.NotePinToggled(isPinned, message)
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi pin ghi chú")
                }
        }
    }

    /**
     * Pin nhiều ghi chú
     */
    fun pinMultipleNotes(noteIds: List<Int>, isPinned: Boolean) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading

            noteRepository.pinMultipleNotes(noteIds, isPinned)
                .onSuccess { updatedCount ->
                    val message = if (isPinned) {
                        "Đã pin $updatedCount ghi chú"
                    } else {
                        "Đã bỏ pin $updatedCount ghi chú"
                    }
                    _uiState.value = NoteUiState.MultipleNotesPinned(updatedCount, isPinned, message)
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi pin ghi chú")
                }
        }
    }

    /**
     * Toggle share
     */
    fun toggleShare(noteId: Int) {
        viewModelScope.launch {
            noteRepository.toggleShare(noteId)
                .onSuccess { shareResult ->
                    val message = if (shareResult.isShared) "Đã chia sẻ ghi chú" else "Đã hủy chia sẻ"
                    _uiState.value = NoteUiState.NoteShared(shareResult, message)
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi chia sẻ ghi chú")
                }
        }
    }

    /**
     * Cập nhật màu sắc ghi chú
     */
    fun updateNoteColor(noteId: Int, color: String) {
        viewModelScope.launch {
            noteRepository.updateNoteColor(noteId, color)
                .onSuccess {
                    _uiState.value = NoteUiState.NoteColorUpdated(color, "Đã cập nhật màu sắc")
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi cập nhật màu sắc")
                }
        }
    }

    /**
     * Cập nhật reminder
     */
    fun updateReminder(noteId: Int, reminderTime: Long?) {
        viewModelScope.launch {
            noteRepository.updateReminder(noteId, reminderTime)
                .onSuccess {
                    val message = if (reminderTime != null) {
                        "Đã đặt lời nhắc"
                    } else {
                        "Đã xóa lời nhắc"
                    }
                    _uiState.value = NoteUiState.ReminderUpdated(reminderTime, message)
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi cập nhật lời nhắc")
                }
        }
    }

    /**
     * Duplicate ghi chú
     */
    fun duplicateNote(noteId: Int) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading

            noteRepository.duplicateNote(noteId)
                .onSuccess { newNoteId ->
                    _uiState.value = NoteUiState.NoteDuplicated(newNoteId, "Đã tạo bản sao ghi chú")
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: "Lỗi khi tạo bản sao")
                }
        }
    }

    /**
     * Cập nhật filter
     */
    fun updateFilter(
        searchQuery: String? = null,
        noteType: NoteType? = null,
        showPinnedOnly: Boolean? = null,
        showSharedOnly: Boolean? = null
    ) {
        _filterState.value = _filterState.value.copy(
            searchQuery = searchQuery ?: _filterState.value.searchQuery,
            selectedNoteType = noteType,
            showPinnedOnly = showPinnedOnly ?: _filterState.value.showPinnedOnly,
            showSharedOnly = showSharedOnly ?: _filterState.value.showSharedOnly
        )
    }

    /**
     * Clear filter
     */
    fun clearFilter() {
        _filterState.value = FilterState()
    }

    /**
     * Reset UI state về Idle
     */
    fun resetUiState() {
        _uiState.value = NoteUiState.Idle
    }
}