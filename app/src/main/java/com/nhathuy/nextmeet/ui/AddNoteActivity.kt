package com.nhathuy.nextmeet.ui

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.ColorPickerAdapter
import com.nhathuy.nextmeet.adapter.ChecklistAdapter
import com.nhathuy.nextmeet.adapter.MediaAdapter
import com.nhathuy.nextmeet.databinding.ActivityAddNoteBinding
import com.nhathuy.nextmeet.fragment.PhotoAlbumBottomSheet
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.model.ChecklistItem
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.Photo
import com.nhathuy.nextmeet.resource.NoteUiState
import com.nhathuy.nextmeet.resource.NotificationUiState
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.viewmodel.NoteViewModel
import com.nhathuy.nextmeet.viewmodel.NotificationViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity để thêm mới hoặc chỉnh sửa ghi chú
 * Hỗ trợ các loại note: TEXT, PHOTO, CHECKLIST
 * Mode: Add (tạo mới) hoặc Edit (chỉnh sửa)
 */
@AndroidEntryPoint
class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private lateinit var colorAdapter: ColorPickerAdapter
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var mediaAdapter: MediaAdapter

    // Data collections
    private val checklistItems = mutableListOf<ChecklistItem>()
    private val imageList = mutableListOf<NoteImage>()

    // Note properties
    private var noteType: NoteType? = null
    private var reminderTime: Long? = null
    private var selectedColorName: String = "color_white"
    private var isPinned = false
    private var isShared = false
    private var currentUserId: Int? = null

    // ViewModels
    private val noteViewModel: NoteViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val notificationViewModel : NotificationViewModel by viewModels()

    // Edit mode variables
    private var noteId: Int = -1
    private var isEditMode = false
    private var currentNote: Note? = null


    private val originalImageList = mutableListOf<NoteImage>()
    private val imagesToDelete = mutableListOf<NoteImage>()
    private val imagesToAdd =  mutableListOf<NoteImage>()
    private var hasUnsavedChanges = false
    private var isNotificationScheduled = false

    // Thêm biến này để kiểm soát việc chờ lưu ảnh
    private var pendingImageInsert = false

    // Image picker launcher
//    private val pickMultipleImagesLauncher =
//        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
//            if (uris.isNotEmpty()) {
//                handleMultipleImageSelection(uris)
//            }
//        }

    companion object {
        val listColor = listOf(
            R.color.color_white,
            R.color.color_red,
            R.color.color_orange,
            R.color.color_yellow,
            R.color.color_green,
            R.color.color_teal,
            R.color.color_blue,
            R.color.color_dark_blue,
            R.color.color_purple,
            R.color.color_pink,
            R.color.color_brown,
            R.color.color_gray
        )

        val colorSourceNames = mapOf(
            R.color.color_white to "color_white",
            R.color.color_red to "color_red",
            R.color.color_orange to "color_orange",
            R.color.color_yellow to "color_yellow",
            R.color.color_green to "color_green",
            R.color.color_teal to "color_teal",
            R.color.color_blue to "color_blue",
            R.color.color_dark_blue to "color_dark_blue",
            R.color.color_purple to "color_purple",
            R.color.color_pink to "color_pink",
            R.color.color_brown to "color_brown",
            R.color.color_gray to "color_gray"
        )

        // Reverse mapping để tìm color resource từ name
        val colorNamesToRes = colorSourceNames.entries.associate { (res, name) -> name to res }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        initializeFromIntent()
        setupUI()
        setupObservers()
        setupClickListeners()
        setupMediaSection()
        setupTextChangeListeners()

        // Load note data nếu ở edit mode
        if (isEditMode) {
            noteViewModel.getNoteById(noteId)
        }

        // Ẩn loading ban đầu
        binding.progressBarAddNote?.visibility = View.GONE
    }

    /**
     * Khởi tạo dữ liệu từ Intent
     */
    private fun initializeFromIntent() {
        // Kiểm tra xem có noteId không (edit mode)
        noteId = intent.getIntExtra(Constant.EXTRA_NOTE_ID, -1)
        isEditMode = noteId != -1

        // Chỉ lấy noteType từ intent nếu không phải edit mode
        if (!isEditMode) {
            noteType = intent.getSerializableExtra(Constant.EXTRA_NOTE_TYPE) as NoteType?
        }

        Log.d("AddNoteActivity", "Mode: ${if (isEditMode) "Edit" else "Add"}, NoteId: $noteId, NoteType: $noteType")
    }

    /**
     * Khởi tạo giao diện
     */
    private fun setupUI() {
        setupColorPicker()
        setupChecklist()
        updateUIForMode()

        // Chỉ show content based on type nếu không phải edit mode
        // Nếu là edit mode, sẽ show sau khi load note
        if (!isEditMode && noteType != null) {
            showContentBasedOnType(noteType!!)
        }
    }

    /**
     * Cập nhật UI dựa trên mode (Add/Edit)
     */
    private fun updateUIForMode() {
        if (isEditMode) {
            binding.noteTitle.text = getString(R.string.edit_note)
        } else {
            binding.noteTitle.text = getString(R.string.add_note)
        }
    }

    /**
     * Khởi tạo observers cho ViewModel
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            noteViewModel.uiState.collect { state ->
                when (state) {
                    is NoteUiState.Loading -> {
                        // Show loading indicator
                        binding.progressBarAddNote?.visibility = View.VISIBLE
                        binding.btnSave.isEnabled = false
                    }

                    is NoteUiState.NoteLoaded -> {
                        binding.progressBarAddNote?.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Log.d("AddNoteActivity", "Note loaded: ${state.note}")
                        populateNoteData(state.note)
                    }

                    is NoteUiState.NoteCreated -> {
                        Log.d("AddNoteActivity", "Note created with ID: ${state.noteId}")
                        if (noteType == NoteType.PHOTO && imageList.isNotEmpty()) {
                            val noteId = state.noteId.toInt()
                            val imagesToSave = imageList.map { it.copy(noteId = noteId) }
                            pendingImageInsert = true
                            noteViewModel.insertImagesForNote(imagesToSave)
                        } else {
                            showSuccessAndFinish(state.message)
                        }
                    }

                    is NoteUiState.ImagesInserted -> {
                        Log.d("AddNoteActivity", "Images inserted successfully")
                        if (pendingImageInsert) {
                            pendingImageInsert = false
                            showSuccessAndFinish("Đã lưu ghi chú với ảnh")
                        }
                    }

                    is NoteUiState.NoteUpdated -> {
                        binding.progressBarAddNote?.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Log.d("AddNoteActivity", "Note updated successfully")
                        showSuccessAndFinish(state.message)
                    }

                    is NoteUiState.Error -> {
                        binding.progressBarAddNote.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Log.e("AddNoteActivity", "Error: ${state.message}")
                        Toast.makeText(this@AddNoteActivity, state.message, Toast.LENGTH_SHORT).show()
                        pendingImageInsert = false
                    }

                    else -> {
                        binding.progressBarAddNote.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                    }
                }
            }
        }

        userViewModel.getCurrentUser().observe(this) { user ->
            user?.let {
                currentUserId = user.id
                notificationViewModel.setCurrentUserId(user.id)
                Log.d("AddNoteActivity", "Current user ID: ${user.id}")
            }
        }


        // Add notification observation
        lifecycleScope.launch {
            notificationViewModel.notificationUiState.collect { state ->
                handleNotificationUiState(state)
            }
        }

    }

    private fun handleNotificationUiState(state: NotificationUiState) {
        when (state) {
            is NotificationUiState.NotificationScheduled -> {
                isNotificationScheduled = true
                Log.d("AddAppointment", "Notification scheduled: ${state.message}")
            }
            is NotificationUiState.Error -> {
                isNotificationScheduled = false
                Log.e("AddAppointment", "Notification error: ${state.message}")
                // Don't show error to user as notification is secondary feature
            }
            else -> {}
        }
    }
    /**
     * Populate UI với dữ liệu note đã load (cho edit mode)
     */
    private fun populateNoteData(note: Note) {
        currentNote = note
        noteType = note.noteType

        // Set basic fields
        binding.textEditTitle.setText(note.title)
        selectedColorName = note.color
        isPinned = note.isPinned
        isShared = note.isShared
        reminderTime = note.reminderTime

        // Update UI controls
        binding.switchPin.isChecked = isPinned
        binding.switchShare.isChecked = isShared

        // Set color và background
        colorAdapter.setSelectedColors(note.color)
        val colorRes = colorNamesToRes[note.color] ?: R.color.color_white
        val color = ContextCompat.getColor(this, colorRes)
        binding.layoutAddNote.setBackgroundColor(color)

        // Set reminder display
        reminderTime?.let {
            updateReminderDisplay()
        }

        // Show content dựa trên note type
        showContentBasedOnType(note.noteType)

        // Populate content dựa trên note type
        when (note.noteType) {
            NoteType.TEXT -> {
                binding.textEdContent.setText(note.content)
            }

            NoteType.CHECKLIST -> {
                populateChecklistItems(note.checkListItems ?: "")
            }

            NoteType.PHOTO -> {
                binding.textEdPhotoContent.setText(note.content)
                loadExistingImages(note.id)
            }

            else -> {
                Log.w("AddNoteActivity", "Unsupported note type: ${note.noteType}")
            }
        }

        Log.d("AddNoteActivity", "Note data populated successfully")
    }

    /**
     * Populate checklist items từ string
     */
    private fun populateChecklistItems(checklistString: String) {
        checklistItems.clear()
        if (checklistString.isNotEmpty()) {
            val lines = checklistString.split("\n")
            for (line in lines) {
                if (line.trim().isNotEmpty()) {
                    val isChecked = line.contains("[x]")
                    val text = line.replace("- [x] ", "").replace("- [ ] ", "").trim()
                    if (text.isNotEmpty()) {
                        checklistItems.add(ChecklistItem(text, isChecked))
                    }
                }
            }
        }

        // Add empty item nếu list rỗng
        if (checklistItems.isEmpty()) {
            checklistItems.add(ChecklistItem("", false))
        }

        checklistAdapter.notifyDataSetChanged()
        Log.d("AddNoteActivity", "Populated ${checklistItems.size} checklist items")
    }

    /**
     * Load existing images cho PHOTO note
     */
    private fun loadExistingImages(noteId: Int) {
        noteViewModel.getImagesForNote(noteId) { images ->

            // lưu danh sách goc
            originalImageList.clear()
            originalImageList.addAll(images)

            // hiển thị ảnh hien tại
            imageList.clear()
            imageList.addAll(images)

            // Update layout manager span count
            val layoutManager = binding.rvMediaItems.layoutManager as GridLayoutManager
            layoutManager.spanCount = calculateSpanCount(imageList.size)

            // Update adapter
            mediaAdapter.notifyDataSetChanged()
            binding.rvMediaItems.post {
                binding.rvMediaItems.requestLayout()
            }

            Log.d("AddNoteActivity", "Loaded ${images.size} existing images")
        }
    }

    /**
     * Hiển thị thông báo thành công và đóng activity
     */
    private fun showSuccessAndFinish(message: String) {
        binding.progressBarAddNote.visibility = View.GONE
        binding.btnSave.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)

        if (!pendingImageInsert) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            showDialogBack()
        }

        binding.btnSave.setOnClickListener {
            saveNote()
        }

        binding.layoutReminder.setOnClickListener {
            showDateTimePicker()
        }

        binding.switchPin.setOnCheckedChangeListener { _, isChecked ->
            isPinned = isChecked
            hasUnsavedChanges = true
        }

        binding.switchShare.setOnCheckedChangeListener { _, isChecked ->
            isShared = isChecked
            hasUnsavedChanges = true
        }
    }

    /**
     * Setup checklist adapter và RecyclerView
     */
    private fun setupChecklist() {
        checklistAdapter = ChecklistAdapter(
            checklistItems,
            onItemChanged = {
                // Optional: handle changes if needed
            },
            onRequestFocus = { position ->
                binding.rvChecklistItems.post {
                    val holder = binding.rvChecklistItems.findViewHolderForAdapterPosition(position)
                    if (holder is ChecklistAdapter.ChecklistViewHolder) {
                        holder.binding.etCheckItem.requestFocus()
                    }
                }
            }
        )

        binding.rvChecklistItems.adapter = checklistAdapter
        binding.rvChecklistItems.layoutManager = LinearLayoutManager(this)

        binding.btnAddChecklistItem.setOnClickListener {
            checklistAdapter.addItem()
            binding.rvChecklistItems.smoothScrollToPosition(checklistItems.size - 1)
        }

        // Chỉ thêm empty item mặc định nếu không phải edit mode
        if (!isEditMode && noteType == NoteType.CHECKLIST && checklistItems.isEmpty()) {
            checklistAdapter.addItem()
        }
    }

    /**
     * Hiển thị layout content dựa trên note type
     */
    private fun showContentBasedOnType(noteType: NoteType) {
        when (noteType) {
            NoteType.TEXT -> {
                binding.textInputContent.visibility = View.VISIBLE
                binding.checklistContainer.visibility = View.GONE
                binding.layoutMedia.visibility = View.GONE
            }

            NoteType.CHECKLIST -> {
                binding.textInputContent.visibility = View.GONE
                binding.checklistContainer.visibility = View.VISIBLE
                binding.layoutMedia.visibility = View.GONE
            }

            NoteType.PHOTO, NoteType.VIDEO -> {
                binding.textInputContent.visibility = View.GONE
                binding.checklistContainer.visibility = View.GONE
                binding.layoutMedia.visibility = View.VISIBLE
            }
        }
        Log.d("AddNoteActivity", "Content layout set for type: $noteType")
    }

    /**
     * Setup color picker
     */
    private fun setupColorPicker() {
        colorAdapter = ColorPickerAdapter(listColor, colorSourceNames) { colorResId, colorName ->
            val color = ContextCompat.getColor(this, colorResId)
            binding.layoutAddNote.setBackgroundColor(color)
            selectedColorName = colorName
            Log.d("AddNoteActivity", "Color selected: $colorName")
        }

        binding.rvColorPicker.adapter = colorAdapter
        binding.rvColorPicker.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    /**
     * Kiểm tra có thay đổi chưa lưu - New method
     */
    private fun hasUnsavedChanges(): Boolean {
        if (!isEditMode) {
            // Trong add mode, có content là có thay đổi
            val title = binding.textEditTitle.text?.toString()?.trim() ?: ""
            if (title.isNotEmpty()) return true

            when (noteType) {
                NoteType.TEXT -> {
                    val content = binding.textEdContent.text?.toString()?.trim() ?: ""
                    return content.isNotEmpty()
                }
                NoteType.CHECKLIST -> {
                    return checklistItems.any { it.text.isNotEmpty() }
                }
                NoteType.PHOTO -> {
                    val content = binding.textEdPhotoContent.text?.toString()?.trim() ?: ""
                    return imageList.isNotEmpty() && content.isNotEmpty()
                }
                else -> return false
            }
        } else {
            // Trong edit mode, kiểm tra thay đổi so với dữ liệu g��c
            currentNote?.let { note ->
                val title = binding.textEditTitle.text?.toString()?.trim() ?: ""
                if (title != note.title) return true
                if (selectedColorName != note.color) return true
                if (isPinned != note.isPinned) return true
                if (reminderTime != note.reminderTime) return true

                when (noteType) {
                    NoteType.TEXT -> {
                        val content = binding.textEdContent.text?.toString()?.trim() ?: ""
                        return content != (note.content ?: "")
                    }
                    NoteType.CHECKLIST -> {
                        val items = checklistAdapter.getItems()
                        val checklistString = items.joinToString("\n") {
                            (if (it.isChecked) "- [x] " else "- [ ] ") + it.text
                        }
                        return checklistString != (note.checkListItems ?: "")
                    }
                    NoteType.PHOTO -> {
                        return hasUnsavedImageChanges(note)
                    }
                    else -> return false
                }
            }
        }
        return hasUnsavedChanges
    }

    /**
     * Kiểm tra có thay đổi chưa lưu - New method
     */
    private fun hasUnsavedImageChanges(note: Note): Boolean {
        val content = binding.textEdPhotoContent.text?.toString()?.trim() ?: ""
        return imagesToDelete.isNotEmpty() || imagesToAdd.isNotEmpty() || content != (note.content ?: "")
    }

    /**
     * Áp dụng các thay đổi ảnh vào database - New method
     */
    private fun applyImageChanges() {
        if (!isEditMode) return

        // Xóa ảnh đã đánh dấu xóa
        if (imagesToDelete.isNotEmpty()) {
            imagesToDelete.forEach { image ->
                noteViewModel.deleteImage(image)
            }
            Log.d("AddNoteActivity", "Deleted ${imagesToDelete.size} images")
        }

        // Thêm ảnh mới
        if (imagesToAdd.isNotEmpty()) {
            val imagesToSave = imagesToAdd.map { it.copy(noteId = noteId) }
            noteViewModel.insertImagesForNote(imagesToSave)
            Log.d("AddNoteActivity", "Added ${imagesToAdd.size} new images")
        }

        // Clear pending changes
        imagesToDelete.clear()
        imagesToAdd.clear()
    }
    /**
     * Hủy bỏ các thay đổi ảnh - New method
     */
    private fun discardImageChanges() {
        // Restore original image list
        imageList.clear()
        imageList.addAll(originalImageList)

        // Clear pending changes
        imagesToDelete.clear()
        imagesToAdd.clear()

        // Update UI
        val layoutManager = binding.rvMediaItems.layoutManager as GridLayoutManager
        layoutManager.spanCount = calculateSpanCount(imageList.size)
        mediaAdapter.notifyDataSetChanged()
        binding.rvMediaItems.post { binding.rvMediaItems.requestLayout() }

        Log.d("AddNoteActivity", "Image changes discarded")
    }

    /**
     * Hiển thị dialog xác nhận thoát
     */
    private fun showDialogBack() {
        if (!hasUnsavedChanges()) {
            finish()
            return
        }

        val title = if (isEditMode) {
            "Discard changes?"
        } else {
            "Discard note?"
        }

        val message = if (isEditMode) {
            "You have unsaved changes. Are you sure you want to discard them?"
        } else {
            "Your note will not be saved. Are you sure you want to discard it?"
        }

        MaterialAlertDialogBuilder(this).setTitle(title)
            .setMessage(message)
            .setIcon(R.drawable.ic_cancel)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Discard") { _, _ ->
                if (isEditMode && noteType == NoteType.PHOTO) {
                    // Restore image changes if discarding
                    discardImageChanges()
                }
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            .show()
    }

    /**
     * Setup media section (RecyclerView cho images)
     */
    private fun setupMediaSection() {
        mediaAdapter = MediaAdapter(imageList) { noteImage ->
            handleImageRemoval(noteImage)
        }

        val gridLayoutManager = GridLayoutManager(this, calculateSpanCount(imageList.size))

        binding.rvMediaItems.apply {
            layoutManager = gridLayoutManager
            adapter = mediaAdapter
            setHasFixedSize(false)
            itemAnimator?.changeDuration = 0
        }

        binding.btnAddImage.setOnClickListener {
            showPhotoAlbumBottomSheet()
        }
    }
    private fun showPhotoAlbumBottomSheet() {
        val bottomSheet = PhotoAlbumBottomSheet.newInstance { selectedPhotos ->
            handleSelectedPhotos(selectedPhotos)
        }
        bottomSheet.show(supportFragmentManager, "PhotoAlbumBottomSheet")
    }

    /**
     * Handle photos selected from bottom sheet
     */
    private fun handleSelectedPhotos(selectedPhotos: List<Photo>) {
        try {
            hasUnsavedChanges = true

            val noteImages = selectedPhotos.map { photo ->
                NoteImage(
                    noteId = if (isEditMode) noteId else 0,
                    imagePath = photo.uri
                )
            }

            // Add to data source
            imageList.addAll(noteImages)

            // Add to pending list for edit mode
            imagesToAdd.addAll(noteImages)

            // Update span count based on new size
            val layoutManager = binding.rvMediaItems.layoutManager as GridLayoutManager
            layoutManager.spanCount = calculateSpanCount(imageList.size)

            // Update adapter
            mediaAdapter.addMultipleImages(noteImages)

            // Force layout update
            binding.rvMediaItems.post { binding.rvMediaItems.requestLayout() }

            val count = noteImages.size
            Toast.makeText(this, "$count photos added", Toast.LENGTH_SHORT).show()
            Log.d("AddNoteActivity", "$count photos selected from gallery")

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to add photos: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AddNoteActivity", "Error handling selected photos", e)
        }
    }

    /**
     * Xu ly xoa anh
     */
    private fun handleImageRemoval(noteImage: NoteImage){
        hasUnsavedChanges = true

        if (isEditMode && noteImage.id != 0) {
            // Nếu là ảnh đã tồn tại trong database
            imagesToDelete.add(noteImage)
        } else {
            // Nếu là ảnh mới thêm vào (chưa save)
            imagesToAdd.remove(noteImage)
        }

        // Remove từ danh sách hiển thị
        mediaAdapter.removeImage(noteImage)

        // Update layout after removal
        val layoutManager = binding.rvMediaItems.layoutManager as GridLayoutManager
        layoutManager.spanCount = calculateSpanCount(imageList.size)
        binding.rvMediaItems.post { binding.rvMediaItems.requestLayout() }

        Log.d("AddNoteActivity", "Image marked for removal: ${noteImage.imagePath}")
    }

    /**
     * Tính span count dựa trên số lượng items
     */
    private fun calculateSpanCount(itemCount: Int): Int {
        return when {
            itemCount <= 1 -> 1    // Single column for 0-1 items
            itemCount <= 4 -> 2    // Two columns for 2-4 items
            else -> 3              // Three columns for 5+ items
        }
    }

    /**
     * Xử lý chọn nhiều ảnh
     */
//    private fun handleMultipleImageSelection(uris: List<Uri>) {
//        try {
//            hasUnsavedChanges = true
//
//            val noteImages = uris.map { uri ->
//                NoteImage(
//                    noteId = if (isEditMode) noteId else 0,
//                    imagePath = uri.toString()
//                )
//            }
//
//            // Add to data source
//            imageList.addAll(noteImages)
//
//            // Thêm vào danh sách ảnh cần thêm (chưa save vào database)
//            imagesToAdd.addAll(noteImages)
//
//            // Update span count based on new size
//            val layoutManager = binding.rvMediaItems.layoutManager as GridLayoutManager
//            layoutManager.spanCount = calculateSpanCount(imageList.size)
//
//            // Update UI with the new images
//            mediaAdapter.addMultipleImages(noteImages)
//
//            // Force layout update
//            binding.rvMediaItems.post { binding.rvMediaItems.requestLayout() }
//
//            val count = noteImages.size
//            Log.d("AddNoteActivity", "$count images added to pending list")
//
//        } catch (e: Exception) {
//            Toast.makeText(this, "Failed to add images: ${e.message}", Toast.LENGTH_SHORT).show()
//            Log.e("AddNoteActivity", "Error adding multiple images", e)
//        }
//    }

    /**
     * Lưu note (tạo mới hoặc cập nhật)
     */
    private fun saveNote() {
        val title = binding.textEditTitle.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditMode) {
            updateNote(title)
        } else {
            createNote(title)
        }
    }

    /**
     * Tạo note mới
     */
    private fun createNote(title: String) {
        when (noteType) {
            NoteType.TEXT -> {
                val content = binding.textEdContent.text?.toString()?.trim() ?: ""

                val note = Note(
                    userId = currentUserId!!,
                    title = title,
                    content = content,
                    noteType = noteType!!,
                    color = selectedColorName,
                    isPinned = isPinned,
                    isShared = isShared,
                    reminderTime = reminderTime
                )
                if(reminderTime != null){
                    noteViewModel.createNote(note,true)
                }
                else{
                    noteViewModel.createNote(note,false)
                }
            }

            NoteType.PHOTO -> {
                val content = binding.textEdPhotoContent.text?.toString()?.trim() ?: ""

                if (imageList.isEmpty()) {
                    Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show()
                    return
                }

                val note = Note(
                    userId = currentUserId!!,
                    title = title,
                    content = content,
                    noteType = noteType!!,
                    color = selectedColorName,
                    isPinned = isPinned,
                    isShared = isShared,
                    reminderTime = reminderTime
                )
                if(reminderTime != null){
                    noteViewModel.createNote(note,true)
                }
                else{
                    noteViewModel.createNote(note,false)
                }
            }

            NoteType.CHECKLIST -> {
                val items = checklistAdapter.getItems()
                if (items.all { it.text.isEmpty() }) {
                    Toast.makeText(this, "Please add at least one checklist item", Toast.LENGTH_SHORT).show()
                    return
                }

                val checklistString = items.joinToString("\n") {
                    (if (it.isChecked) "- [x] " else "- [ ] ") + it.text
                }

                val note = Note(
                    userId = currentUserId!!,
                    title = title,
                    noteType = noteType!!,
                    color = selectedColorName,
                    isPinned = isPinned,
                    isShared = isShared,
                    reminderTime = reminderTime,
                    checkListItems = checklistString
                )
                if(reminderTime != null){
                    noteViewModel.createNote(note,true)
                }
                else{
                    noteViewModel.createNote(note,false)
                }
            }

            else -> {
                Toast.makeText(this, "Unsupported note type", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Cập nhật note hiện có
     */
    private fun updateNote(title: String) {
        when (noteType) {
            NoteType.TEXT -> {
                val content = binding.textEdContent.text?.toString()?.trim() ?: ""
                noteViewModel.updateNote(
                    noteId = noteId,
                    title = title,
                    content = content,
                    color = selectedColorName
                )
            }

            NoteType.CHECKLIST -> {
                val items = checklistAdapter.getItems()
                if (items.all { it.text.isEmpty() }) {
                    Toast.makeText(this, "Please add at least one checklist item", Toast.LENGTH_SHORT).show()
                    return
                }

                val checklistString = items.joinToString("\n") {
                    (if (it.isChecked) "- [x] " else "- [ ] ") + it.text
                }

                noteViewModel.updateNote(
                    noteId = noteId,
                    title = title,
                    color = selectedColorName,
                    checkListItems = checklistString
                )
            }

            NoteType.PHOTO -> {

                val content = binding.textEdPhotoContent.text?.toString()?.trim() ?: ""

                if (imageList.isEmpty()) {
                    Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show()
                    return
                }

                noteViewModel.updateNote(
                    noteId = noteId,
                    title = title,
                    content = content,
                    color = selectedColorName
                )
                // Images are handled separately when added/removed
                applyImageChanges()
            }

            else -> {
                Toast.makeText(this, "Unsupported note type", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Update các properties khác nếu thay đổi
        currentNote?.let { note ->
            if (note.isPinned != isPinned) {
                noteViewModel.togglePin(noteId)
            }
            if (note.reminderTime != reminderTime) {
                noteViewModel.updateReminder(noteId, reminderTime)
                lifecycleScope.launch {
                    // Bước 1: Hủy notification cũ nếu có
                    if (note.reminderTime != null) {
                        noteViewModel.cancelNoteNotification(note.id)
                    }

                    // Bước 2: Cập nhật reminder time trong database
                    noteViewModel.updateReminder(noteId, reminderTime)

                    // Bước 3: Tạo notification mới nếu có reminder time
                    if (reminderTime != null) {
                        val updatedNote = note.copy(
                            reminderTime = reminderTime,
                            updatedAt = System.currentTimeMillis()
                        )
                        noteViewModel.scheduleNoteNotification(updatedNote)
                    }
                }
            }
        }
    }

    /**
     * Hiển thị date time picker cho reminder
     */
    private fun showDateTimePicker() {
        val constraintBuilder =
            CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            showTimePicker(calendar)
        }

        datePicker.show(supportFragmentManager, "date_picker")
    }

    /**
     * Hiển thị time picker
     */
    private fun showTimePicker(calendar: Calendar) {
        val now = Calendar.getInstance()

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Select time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val selectedHour = timePicker.hour
            val selectedMinute = timePicker.minute

            if (isToday(calendar)) {
                if (selectedHour < now.get(Calendar.HOUR_OF_DAY) ||
                    (selectedHour == now.get(Calendar.HOUR_OF_DAY)) && selectedMinute <= now.get(Calendar.MINUTE)
                ) {
                    Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
                    return@addOnPositiveButtonClickListener
                }
            }

            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            calendar.set(Calendar.SECOND, 0)

            reminderTime = calendar.timeInMillis
            updateReminderDisplay()
        }

        timePicker.show(supportFragmentManager, "time_picker")
    }

    /**
     * Kiểm tra xem có phải ngày hôm nay không
     */
    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Cập nhật hiển thị reminder time
     */
    private fun updateReminderDisplay() {
        if (reminderTime != null) {
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val formattedDate = formatter.format(Date(reminderTime!!))
            binding.tvReminderTime.text = formattedDate
            binding.tvReminderTime.visibility = View.VISIBLE
            Log.d("AddNoteActivity", "Reminder set for: $formattedDate")
        } else {
            // Clear text khi không có reminder
            binding.tvReminderTime.text = ""
            binding.tvReminderTime.visibility = View.GONE
            Log.d("AddNoteActivity", "No reminder set")
        }
    }

    private fun setupTextChangeListeners() {
        binding.textEditTitle.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                hasUnsavedChanges = true
            }
        })

        if (noteType == NoteType.TEXT) {
            binding.textEdContent.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    hasUnsavedChanges = true
                }
            })
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        showDialogBack()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AddNoteActivity", "Activity destroyed")
    }
}