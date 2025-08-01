package com.nhathuy.nextmeet.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.NoteImageDao
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.ShareResult
import com.nhathuy.nextmeet.utils.ImageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository cho Note với xử lý logic business
 *
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val noteImageDao: NoteImageDao,
    private val imageManager: ImageManager,
    private val context: Context
) {

    /**
     * Lay tất cả ghi chú với tùy chọn filter và search
     */
    fun getAllNotesWithFilter(
        userId: Int,
        searchQuery: String = "",
        noteType: NoteType? = null,
        showPinnedOnly: Boolean = false,
        showSharedOnly: Boolean = false
    ): Flow<List<Note>> {
        return flow {
            try {
                // lấy data từ dao dựa trên filter
                val notesFlow = when {
                    showPinnedOnly -> noteDao.getPinnedNotes(userId)
                    showSharedOnly -> noteDao.getSharedNotes(userId)
                    searchQuery.isNotBlank() -> noteDao.searchNotes(userId, searchQuery)
                    noteType != null -> noteDao.getNotesByType(userId, noteType)
                    else -> noteDao.getAllNotesByUser(userId)
                }

                notesFlow.collect { notes ->
                    var filteredNotes = notes

                    if (noteType != null && !showPinnedOnly && !showSharedOnly) {
                        filteredNotes = filteredNotes.filter {
                            it.noteType == noteType
                        }
                    }

                    if (searchQuery.isNotBlank() && !showPinnedOnly && !showSharedOnly) {
                        filteredNotes = filteredNotes.filter {
                            it.title.contains(
                                searchQuery,
                                ignoreCase = true || it.content.contains(
                                    searchQuery,
                                    ignoreCase = true
                                )
                            )
                        }
                    }

                    // sort : pinned đầu tiên, khi đã update thời gian
                    val sortedNotes = filteredNotes.sortedWith(
                        compareByDescending<Note> {
                            it.isPinned
                        }.thenByDescending { it.updatedAt }
                    )

                    emit(sortedNotes)
                }
            } catch (e: Exception) {
                emit(emptyList())
                throw e
            }
        }
    }

    /**
     * Lấy ghi chú theo Id với error handling
     */
    suspend fun getNoteById(noteId: Int): Result<Note?> {
        return try {
            val note = noteDao.getNoteById(noteId)
            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tạo ghi chú mới với validation
     */
    suspend fun createNote(
        userId: Int,
        title: String = "",
        content: String = "",
        noteType: NoteType = NoteType.TEXT,
        color: String = "color_white",
        pinned: Boolean = false,
        shared: Boolean = false,
        reminderTime: Long? = null,
        checkListItems: String? = null
    ): Result<Long> {
        return try {
            if (userId <= 0) {
                return Result.failure(IllegalArgumentException(context.getString(R.string.error_invalid_user_id)))
            }

            if (title.isBlank() && content.isBlank() && checkListItems.isNullOrEmpty()) {
                return Result.failure(IllegalArgumentException(context.getString(R.string.note_not_found)))
            }

            if (!isValidHexColor(color)) {
                return Result.failure(IllegalArgumentException(context.getString(R.string.invalid_color)))
            }

            val note = Note(
                userId = userId,
                title = title.trim(),
                content = content.trim(),
                noteType = noteType,
                color = color,
                isPinned = pinned,
                isShared = shared,
                checkListItems = checkListItems,
                reminderTime = reminderTime,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val noteId = noteDao.insertNote(note)
            Result.success(noteId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kiểm tra hex color format hoặc tên màu hợp lệ
     */
    private fun isValidHexColor(color: String): Boolean {
        // Chấp nhận tên màu resource hoặc mã hex
        val allowedColorNames = setOf(
            "color_white", "color_red", "color_orange", "color_yellow", "color_green",
            "color_teal", "color_blue", "color_dark_blue", "color_purple", "color_pink",
            "color_brown", "color_gray"
        )
        return color.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) || allowedColorNames.contains(
            color
        )
    }


     /**
     * Cập nhật ghi chú với validation
     */
    suspend fun updateNote(
        noteId: Int,
        title: String? = null,
        content: String? = null,
        noteType: NoteType? = null,
        color: String? = null,
        checkListItems: String? = null
    ): Result<Unit> {
        return try {
            val existingNote = noteDao.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException(context.getString(R.string.note_not_found)))

            // Kiểm tra nhập input
            color?.let {
                if (!isValidHexColor(it)) {
                    return Result.failure(IllegalArgumentException(context.getString(R.string.invalid_color)))
                }
            }

            val updatedNote = existingNote.copy(
                title = title?.trim() ?: existingNote.title,
                content = content?.trim() ?: existingNote.content,
                noteType = noteType ?: existingNote.noteType,
                color = color ?: existingNote.color,
                checkListItems = checkListItems ?: existingNote.checkListItems,
                updatedAt = System.currentTimeMillis()
            )

            // kiểm tra hợp lệ với note
            if (updatedNote.title.isBlank() && updatedNote.content.isBlank() &&
                updatedNote.checkListItems.isNullOrBlank()
            ) {
                return Result.failure(IllegalArgumentException(context.getString(R.string.error_note_empty)))
            }

            noteDao.updateNote(updatedNote)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa ghi chú
     */
    suspend fun deleteNote(noteId: Int): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // Xóa tất cả ảnh của note trước
                deleteImagesByNoteId(noteId)

                // Xóa note
                noteDao.deleteNoteById(noteId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pin/unpin với ghi chú
     */
    suspend fun togglePin(noteId: Int): Result<Boolean> {
        return try {
            val note = noteDao.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException(context.getString(R.string.note_not_found)))

            val newPinStatus = !note.isPinned
            noteDao.updatePinStatus(noteId, newPinStatus)
            Result.success(newPinStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share/Unshare ghi chú với validation
     */
    suspend fun toggleShare(noteId: Int): Result<ShareResult> {
        return try {
            val note = noteDao.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException(context.getString(R.string.note_not_found)))

            if (note.title.isBlank() && note.content.isBlank()) {
                return Result.failure(IllegalArgumentException(context.getString(R.string.cannot_share_blank_notes)))
            }

            val newShareStatus = !note.isShared
            noteDao.updateShareStatus(noteId, newShareStatus)

            val shareResult = ShareResult(
                isShared = newShareStatus,
                shareContent = if (newShareStatus) generateShareContent(note) else null
            )
            Result.success(shareResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Thêm nhiều ảnh cho 1 ghi chú
     */
    suspend fun insertImagesForNote(images: List<NoteImage>): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {

                val savedImages = mutableListOf<NoteImage>()
                for(image in images){
                    val finalPath = if(image.imagePath.startsWith("content://")){
                        val uri = Uri.parse(image.imagePath)
                        imageManager.saveImageFromUri(uri,image.noteId)
                    }
                    else {
                        image.imagePath
                    }

                    finalPath?.let {
                        path ->
                        val noteImage = image.copy(imagePath =  path)
                        savedImages.add(noteImage)
                        Log.d("NoteRepository", "Saved image path: $path for noteId: ${noteImage.noteId}")
                    }
                }
                if(savedImages.isNotEmpty()){
                    noteImageDao.insertImages(savedImages)
                    Log.d("NoteRepository", "Inserted ${savedImages.size} images into Room for noteId: ${savedImages.firstOrNull()?.noteId}")
                    savedImages.forEach {
                        Log.d("NoteRepository", "Room image: id=${it.id}, noteId=${it.noteId}, path=${it.imagePath}")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error inserting images for note: ${e.message}", e)
            if (e is kotlinx.coroutines.CancellationException) {
                Log.e("NoteRepository", "Coroutine was cancelled, likely due to lifecycle or scope.")
            }
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách ảnh của 1 ghi chú
     */
    suspend fun getImagesForNote(noteId: Int): Result<List<NoteImage>> {
        return try {
            val images = noteImageDao.getImagesForNote(noteId)
            Result.success(images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy flow danh sách ảnh của 1 ghi chú (liên tục)
     */
    fun getImagesFlowForNote(noteId: Int): Flow<List<NoteImage>> {
        return noteImageDao.getImagesFlowForNote(noteId)
    }

    /**
     * Xóa 1 ảnh khỏi ghi chú
     */
    suspend fun deleteImage(image: NoteImage): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                imageManager.deleteImage(image.imagePath)
                noteImageDao.deleteImage(image)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    /**
     * Xóa tất cả ảnh của 1 ghi chú
     */
    suspend fun deleteImagesByNoteId(noteId: Int): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // Lấy danh sách ảnh trước khi xóa
                val images = noteImageDao.getImagesForNote(noteId)

                // Xóa từng file ảnh
                images.forEach { image ->
                    imageManager.deleteImage(image.imagePath)
                }

                noteImageDao.deleteImagesByNoteId(noteId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tạo dupliate title
     */
    private fun generateShareContent(note: Note): String {
        return buildString {
            if (note.title.isNotBlank()) {
                appendLine(note.title)
                appendLine("---")
            }
            if (note.content.isNotBlank()) {
                appendLine(note.content)
            }

            if (!note.checkListItems.isNullOrBlank()) {
                appendLine()
                appendLine(context.getString(R.string.list))
                appendLine(note.checkListItems)
            }
            appendLine()
            appendLine(context.getString(R.string.shared_from_nextmeet))
        }
    }

    /**
     * Cập nhật màu sắc với validation
     */
    suspend fun updateNoteColor(noteId: Int, color: String): Result<Unit> {
        return try {
            if (!isValidHexColor(color)) {
                return Result.failure(IllegalArgumentException(context.getString(R.string.invalid_color)))
            }

            val note = noteDao.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException(context.getString(R.string.note_not_found)))

            noteDao.updateNoteColor(noteId, color)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Cập nhật reminder với validation
     */
    suspend fun updateReminder(noteId: Int, reminderTime: Long?): Result<Unit> {
        return try {
            val note = noteDao.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException(context.getString(R.string.note_not_found)))

            // Kiểm tra thời gian hẹn lịch
            reminderTime?.let {
                if (it <= System.currentTimeMillis()) {
                    return Result.failure(IllegalArgumentException(context.getString(R.string.reminder_time_must_be_in_the_future)))
                }
            }

            noteDao.updateReminder(noteId, reminderTime)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Duplicate ghi chú với logic xử lý
     */
    suspend fun duplicateNote(noteId: Int): Result<Long> {
        return try {
            val originalNote = noteDao.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException(context.getString(R.string.note_not_found)))

            val duplicatedNote = originalNote.copy(
                id = 0,
                title = generateDuplicateTitle(originalNote.title),
                isPinned = false, // Bản sao không được pin
                isShared = false, // Bản sao không được share
                reminderTime = null, // Bản sao không có reminder
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val newNoteId = noteDao.insertNote(duplicatedNote)
            Result.success(newNoteId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tạo bản sao title
     */
    private fun generateDuplicateTitle(originalTitle: String): String {
        val copyLabel = context.getString(R.string.copy_suffix)
        val copyWithNumber = context.getString(R.string.copy_suffix_with_number)

        return when {
            originalTitle.isBlank() -> context.getString(R.string.copy_only)
            originalTitle.contains(copyLabel) -> {
                val regex = Regex("""\(${Regex.escape(copyLabel.removeSurrounding("(", ")"))}( (\d+))?\)$""")
                val match = regex.find(originalTitle)
                if (match != null) {
                    val number = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 1
                    originalTitle.replace(regex, copyWithNumber.format(number + 1))
                } else {
                    "$originalTitle ${copyWithNumber.format(2)}"
                }
            }
            else -> "$originalTitle $copyLabel"
        }
    }

    /**
     * Xóa nhieu ghi chú
     */
    suspend fun deleteMultipleNotes(noteIds: List<Int>): Result<Int> {
        return try {
            withContext(Dispatchers.IO) {
                var deletedCount = 0

                noteIds.forEach { noteId ->
                    try {
                        // Xóa ảnh của từng note
                        deleteImagesByNoteId(noteId)

                        // Xóa note
                        noteDao.deleteNoteById(noteId)
                        deletedCount++
                    } catch (e: Exception) {
                        // Log error nhưng tiếp tục với các note khác
                        Log.e("NoteRepository", "Error deleting note $noteId", e)
                    }
                }

                Result.success(deletedCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pin nhiều ghi chú
     */
    suspend fun pinMultipleNotes(noteIds: List<Int>, isPinned: Boolean): Result<Int> {
        return try {
            var updatedCount = 0
            noteIds.forEach { noteId ->
                val note = noteDao.getNoteById(noteId)
                if (note != null) {
                    noteDao.updatePinStatus(noteId, isPinned)
                    updatedCount++
                }
            }
            Result.success(updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * lấy tat ca anh cua paths
     */
    fun getAllImagePaths(): Flow<List<String>> {
        return noteImageDao.getAllImagePaths()
    }
}
