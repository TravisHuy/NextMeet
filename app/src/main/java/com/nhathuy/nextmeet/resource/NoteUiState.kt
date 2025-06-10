package com.nhathuy.nextmeet.resource

import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.ShareResult

sealed class NoteUiState{
    object  Idle: NoteUiState()
    object Loading: NoteUiState()

    data class NotesLoaded(val notes: List<Note>):NoteUiState()
    data class NoteLoaded(val note:Note) : NoteUiState()

    data class NoteCreated(val noteId:Long, val message:String): NoteUiState()
    data class NoteUpdated(val message:String) :NoteUiState()
    data class NoteDeleted(val message: String):NoteUiState()

    data class ImagesInserted(val message:String): NoteUiState()

    data class MultipleNotesDeleted(val deletedCount: Int, val message:String):NoteUiState()
    data class NotePinToggled(val isPinned:Boolean , val message:String) : NoteUiState()

    data class MultipleNotesPinned(val updatedCount: Int, val isPinned: Boolean, val message: String) : NoteUiState()

    data class NoteShared(val shareResult: ShareResult, val message: String) : NoteUiState()
    data class NoteColorUpdated(val color: String, val message: String) : NoteUiState()
    data class ReminderUpdated(val reminderTime: Long?, val message: String) : NoteUiState()
    data class NoteDuplicated(val newNoteId: Long, val message: String) : NoteUiState()

    data class Error(val message: String) : NoteUiState()
}
