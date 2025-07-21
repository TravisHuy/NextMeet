package com.nhathuy.nextmeet.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.repository.NoteRepository
import com.nhathuy.nextmeet.resource.FilterState
import com.nhathuy.nextmeet.resource.NoteUiState
import com.nhathuy.nextmeet.utils.NotificationManagerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel cho note
 *
 * @version 2.0
 * @since 29/05/2025
 * @author TravisHuy(Ho Nhat Huy)
 */
@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val notificationManagerService: NotificationManagerService,
    @ApplicationContext private val context : Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteUiState>(NoteUiState.Idle)
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    /**
     * Lấy tất cả ghi chú với filter
     */
    fun getAllNotes(userId: Int): Flow<List<Note>> {
        return combine(
            _filterState,
            noteRepository.getAllNotesWithFilter(
                userId = userId,
                searchQuery = _filterState.value.searchQuery,
                noteType = _filterState.value.selectedNoteType,
                showPinnedOnly = _filterState.value.showPinnedOnly,
                showSharedOnly = _filterState.value.showSharedOnly
            )
        ) { _, notes ->
            _uiState.value = NoteUiState.NotesLoaded(notes)
            notes
        }.catch { error ->
            _uiState.value = NoteUiState.Error(error.message ?: context.getString(R.string.error_unknown))
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
                        NoteUiState.Error(context.getString(R.string.note_not_found))
                    }
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: context.getString(R.string.error_loading_note))
                }
        }
    }

    /**
     * Tạo ghi chú mới
     */

    fun createNote(
        note: Note,
        shouldSetReminder: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading

            noteRepository.createNote(
                note.userId,
                note.title,
                note.content,
                note.noteType,
                note.color,
                note.isPinned,
                note.isShared,
                note.reminderTime,
                note.checkListItems
            )
                .onSuccess { noteId ->
                    if (shouldSetReminder && note.reminderTime != null) {
                        val noteWithId = note.copy(id = noteId.toInt())
                        scheduleNoteNotification(noteWithId)
                    }
                    _uiState.value =
                        NoteUiState.NoteCreated(noteId, context.getString(R.string.note_created_success))
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: context.getString(R.string.error_creating_note))
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
                    _uiState.value = NoteUiState.NoteUpdated(context.getString(R.string.note_updated_success))
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: context.getString(R.string.error_updating_note))
                }
        }
    }


    /**
     * Xử lý notification khi update note
     */
    private suspend fun handleNotificationUpdate(
        currentNote: Note,
        newReminderTime: Long?,
        shouldUpdateReminder: Boolean,
        updatedTitle: String?,
        updatedContent: String?
    ) {
        if (shouldUpdateReminder) {
            // Luôn cancel notification cũ trước
            cancelNoteNotification(currentNote.id)

            // Nếu có reminder time mới, tạo notification mới
            newReminderTime?.let { reminderTime ->
                val noteForNotification = currentNote.copy(
                    title = updatedTitle ?: currentNote.title,
                    content = updatedContent ?: currentNote.content,
                    reminderTime = reminderTime,
                    updatedAt = System.currentTimeMillis()
                )
                scheduleNoteNotification(noteForNotification)
            }
        }
    }

    /**
     * Hủy thông báo cho cuộc hẹn
     */
    suspend fun cancelNoteNotification(noteId: Int) {
        try {
            // Xóa tất cả notification liên quan đến appointment này
            notificationManagerService.cancelNotificationsByRelatedId(
                noteId,
                NotificationType.NOTE_REMINDER
            )
        } catch (e: Exception) {
            Log.e("AppointmentViewModel", "Lỗi khi hủy notification", e)
        }
    }

    /**
     * Lên thông báo hẹn cho note
     */
    fun scheduleNoteNotification(note: Note) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()

                note.reminderTime?.let { reminderTime ->
                    if (reminderTime > now) {
                        val notificationContent = when (note.noteType) {
                            NoteType.CHECKLIST -> {
                                if (!note.checkListItems.isNullOrEmpty()) {
                                    formatChecklistForNotification(note.checkListItems)
                                } else {
                                    note.content.ifEmpty { context.getString(R.string.checklist_empty) }
                                }
                            }

                            else -> {
                                note.content.ifEmpty { context.getString(R.string.note_empty) }
                            }
                        }
                        val success = notificationManagerService.scheduleNoteNotification(
                            userId = note.userId,
                            noteId = note.id,
                            title = note.title,
                            content = notificationContent,
                            noteTime = reminderTime,
                        )
                        if (!success) {
                            // Log lỗi nhưng không fail toàn bộ process tạo appointment
                            Log.w(
                                "NoteViewModel",
                                "Không thể tạo notification cho ghi chú ${note.id}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NoteViewModel", "Lỗi không tạo ghi chú", e)
            }
        }
    }

    private fun formatChecklistForNotification(checkListItems: String): String {
        return try {
            if (checkListItems.startsWith("[") && checkListItems.endsWith("]")) {
                // Parse JSON đơn giản (có thể cần thư viện JSON)
                val items = checkListItems
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .take(3)

                val preview = items.joinToString("\n") { "• $it" }
                if (checkListItems.split(",").size > 3) {
                    "$preview\n..."
                } else {
                    preview
                }
            } else {
                // Nếu là format đơn giản ngăn cách bằng |
                val items = checkListItems.split("|").take(3)
                val preview = items.joinToString("\n") { "• $it" }
                if (checkListItems.split("|").size > 3) {
                    "$preview\n..."
                } else {
                    preview
                }
            }
        } catch (e: Exception) {
            Log.e("NoteViewModel", "Lỗi format checklist", e)
            "Checklist: ${checkListItems.take(50)}..."
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
                    cancelNoteNotification(noteId)
                    _uiState.value = NoteUiState.NoteDeleted(context.getString(R.string.note_deleted_success))
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: context.getString(R.string.error_deleting_note))
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
                    val message = if (isPinned) context.getString(R.string.note_pinned) else context.getString(R.string.note_unpinned)
                    _uiState.value = NoteUiState.NotePinToggled(isPinned, message)
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: context.getString(R.string.error_pin_note))
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
                    _uiState.value =
                        NoteUiState.MultipleNotesPinned(updatedCount, isPinned, message)
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
                    val message =
                        if (shareResult.isShared) context.getString(R.string.note_shared_success) else context.getString(R.string.note_unshared_success)
                    _uiState.value = NoteUiState.NoteShared(shareResult, message)
                }
                .onFailure { error ->
                    _uiState.value =
                        NoteUiState.Error(error.message ?: context.getString(R.string.error_sharing_note))
                }
        }
    }

    /**
     * Chia sẻ note với ứng dụng khác
     */
    fun shareNoteWithOtherApps(noteId: Int){
        if (noteId <= 0) {
            _uiState.value = NoteUiState.Error(context.getString(R.string.error_invalid_note_id))
            return
        }
        viewModelScope.launch {
            noteRepository.getNoteById(noteId)
                .onSuccess { note ->
                    note?.let {
                        val shareContent = buildShareContent(note)
                        _uiState.value = NoteUiState.ShareWithOtherApps(shareContent)
                    }
                }
                .onFailure { error ->
                    _uiState.value =
                        NoteUiState.Error(error.message ?: context.getString(R.string.error_sharing_note))
                }
        }
    }

    /**
     * Build formatted content for sharing
     */
    private fun buildShareContent(note: Note): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return buildString {
            append(context.getString(R.string.share_note_title, note.title))

            when(note.noteType){
                NoteType.TEXT -> {
                    append("${note.content}\n\n")
                }
                NoteType.CHECKLIST -> {
                    append(context.getString(R.string.share_note_checklist_title))
                    note.checkListItems?.let { json ->
                        val items = formatChecklistForNotification(json)
                        append("$items\n")
                    } ?: append(context.getString(R.string.share_note_no_checklist))
                }
                NoteType.PHOTO, NoteType.VIDEO -> {
                    append(context.getString(R.string.share_note_description),note.content)
                }
            }

            append(context.getString(R.string.share_note_time),formatter.format(Date()))
            append(context.getString(R.string.share_note_footer))
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
                    _uiState.value =
                        NoteUiState.Error(error.message ?: "Lỗi khi cập nhật màu sắc")
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
                        context.getString(R.string.note_reminder_set)
                    } else {
                        context.getString(R.string.note_reminder_removed)
                    }
                    _uiState.value = NoteUiState.ReminderUpdated(reminderTime, message)
                }
                .onFailure { error ->
                    _uiState.value =
                        NoteUiState.Error(error.message ?: context.getString(R.string.error_updating_reminder))
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
                    _uiState.value =
                        NoteUiState.NoteDuplicated(newNoteId, context.getString(R.string.note_duplicated))
                }
                .onFailure { error ->
                    _uiState.value = NoteUiState.Error(error.message ?: context.getString(R.string.error_duplicating_note))
                }
        }
    }

    /**
     * Thêm nhiều ảnh cho 1 ghi chú
     */
    fun insertImagesForNote(images: List<NoteImage>) {
        viewModelScope.launch {
            try {
                noteRepository.insertImagesForNote(images)
                _uiState.value = NoteUiState.ImagesInserted(context.getString(R.string.note_images_saved))
            } catch (e: Exception) {
                _uiState.value = NoteUiState.Error(e.message ?: context.getString(R.string.error_saving_images))
            }
        }
    }

    /**
     * Lấy danh sách ảnh của 1 ghi chú
     */
    fun getImagesForNote(noteId: Int, onResult: (List<NoteImage>) -> Unit) {
        viewModelScope.launch {
            val result = noteRepository.getImagesForNote(noteId)
            if (result.isSuccess) {
                onResult(result.getOrDefault(emptyList()))
            } else {
                onResult(emptyList())
            }
        }
    }


    /**
     * Lấy flow danh sách ảnh của 1 ghi chú (liên tục)
     */
    fun getImagesFlowForNote(noteId: Int): Flow<List<NoteImage>> {
        return noteRepository.getImagesFlowForNote(noteId)
    }

    /**
     * Xóa 1 ảnh khỏi ghi chú
     */
    fun deleteImage(image: NoteImage) {
        viewModelScope.launch {
            noteRepository.deleteImage(image)
        }
    }

    /**
     * Xóa tất cả ảnh của 1 ghi chú
     */
    fun deleteImagesByNoteId(noteId: Int) {
        viewModelScope.launch {
            noteRepository.deleteImagesByNoteId(noteId)
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

