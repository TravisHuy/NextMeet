package com.nhathuy.nextmeet.repository

import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.NoteImageDao
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.ShareResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository cho Note với xử lý logic business
 *
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val noteImageDao: NoteImageDao
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
                return Result.failure(IllegalArgumentException("User Id không hợp lệ"))
            }

            if (title.isBlank() && content.isBlank() && checkListItems.isNullOrEmpty()) {
                return Result.failure(IllegalArgumentException("Ghi chú không thể trống"))
            }

            if (!isValidHexColor(color)) {
                return Result.failure(IllegalArgumentException("Màu sắc không hợp lệ"))
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
                ?: return Result.failure(IllegalArgumentException("Ghi chú không tồn tại"))

            // Kiểm tra nhập input
            color?.let {
                if (!isValidHexColor(it)) {
                    return Result.failure(IllegalArgumentException("Màu sắc không hợp lệ"))
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
                return Result.failure(IllegalArgumentException("Ghi chú không thể trống"))
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
            val note = noteDao.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException("ghi chú không tồn tại"))
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
                ?: return Result.failure(IllegalArgumentException("Ghi chú không tồn taại"))

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
                ?: return Result.failure(IllegalArgumentException("Ghi chú không tồn taại"))

            if (note.title.isBlank() && note.content.isBlank()) {
                return Result.failure(IllegalArgumentException("Không thể chia sẽ ghi chú trống"))
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
            noteImageDao.insertImages(images)
            Result.success(Unit)
        } catch (e: Exception) {
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
     * Xóa 1 ảnh khỏi ghi chú
     */
    suspend fun deleteImage(image: NoteImage): Result<Unit> {
        return try {
            noteImageDao.deleteImage(image)
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
            noteImageDao.deleteImagesByNoteId(noteId)
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
                appendLine("Danh sách:")
                appendLine(note.checkListItems)
            }
            appendLine()
            appendLine("Được chia sẽ từ NextMeet")
        }
    }

    /**
     * Cập nhật màu sắc với validation
     */
    suspend fun updateNoteColor(noteId: Int, color: String): Result<Unit> {
        return try {
            if (!isValidHexColor(color)) {
                return Result.failure(IllegalArgumentException("Màu sắc không hợp lệ"))
            }

            val note = noteDao.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException("Ghi chú không tồn tại"))

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
                ?: return Result.failure(IllegalArgumentException("Ghi chú không tồn tại"))

            // Kiểm tra thời gian hẹn lịch
            reminderTime?.let {
                if (it <= System.currentTimeMillis()) {
                    return Result.failure(IllegalArgumentException("Thời gian nhắc nhở phải trong tương lai"))
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
                ?: return Result.failure(IllegalArgumentException("Ghi chú không tồn tại"))

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
        return when {
            originalTitle.isBlank() -> "bản sao"
            originalTitle.contains("(Bản sao)") -> {
                val regex = Regex("""\(Bản sao( \d+)?\)$""")
                val match = regex.find(originalTitle)
                if (match != null) {
                    val number = match.groupValues[1].trim().toIntOrNull() ?: 1
                    originalTitle.replace(regex, "(Bản sao ${number + 1})")
                } else {
                    "$originalTitle (Bản sao 2)"
                }
            }

            else -> "$originalTitle (bản sao)"
        }
    }

    /**
     * Xóa nhieu ghi chú
     */
    suspend fun deleteMultipleNotes(noteIds: List<Int>): Result<Int> {
        return try {
            var deletedCount = 0
            noteIds.forEach { noteId ->
                val note = noteDao.getNoteById(noteId)
                if (note != null) {
                    noteDao.deleteNote(note)
                    deletedCount++
                }
            }
            Result.success(deletedCount)
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
}

