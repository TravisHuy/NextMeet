package com.nhathuy.nextmeet.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.model.ChecklistItem
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.resource.NoteUiState
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.viewmodel.NoteViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private lateinit var colorAdapter: ColorPickerAdapter
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var mediaAdapter: MediaAdapter
    private val checklistItems = mutableListOf<ChecklistItem>()
    private val imageList = mutableListOf<NoteImage>()
    private var noteType: NoteType? = null
    private var reminderTime: Long? = null
    private var selectedColorName: String = "color_white"
    private var isPinned = false
    private var isShared = false
    private var currentUserId: Int? = null
    private val noteViewModel: NoteViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()



    private val pickMultipleImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            handleMultipleImageSelection(uris)
        }
    }

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteType = intent.getSerializableExtra(Constant.EXTRA_NOTE_TYPE) as NoteType?

        setupUI()
        setupObservers()
        setupClickListeners()
        setupMediaSection()
    }

    // khởi tạo giao diện
    private fun setupUI() {
        setupColorPicker()
        setupChecklist()
        showContentBasedOnType(noteType!!)
    }

    //khởi tạo observer
    private fun setupObservers() {
        lifecycleScope.launch {
            noteViewModel.uiState.collect { state ->
                when (state) {
                    is NoteUiState.Loading -> {
                        // Show loading indicator if needed
                    }

                    is NoteUiState.NoteCreated -> {
                        if(noteType == NoteType.PHOTO && imageList.isNotEmpty()){
                            val noteId = state.noteId.toInt()
                            val imagesToSave = imageList.map { it.copy(noteId = noteId) }
                            noteViewModel.insertImagesForNote(imagesToSave)
                            // Show success and finish immediately after saving images
                            showSuccessAndFinish(state.message)
                        } else {
                            showSuccessAndFinish(state.message)
                        }
                    }

                    is NoteUiState.Error -> {
                        Toast.makeText(this@AddNoteActivity, state.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                    else -> {
                    }
                }
            }
        }
        
        userViewModel.getCurrentUser().observe(this) { user ->
            user?.let {
                currentUserId = user.id
            }
        }
    }

    // Hiệu ứng đơn giản, hiệu quả khi tạo note xong
    private fun showSuccessAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    // hàm sử lý click
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
        }
        binding.switchShare.setOnCheckedChangeListener { _, isChecked ->
            isShared = isChecked
        }
    }

    private fun setupChecklist() {
        checklistAdapter = ChecklistAdapter(checklistItems, onItemChanged = {
            // Optional: handle changes if needed
        }, onRequestFocus = { position ->
            binding.rvChecklistItems.post {
                val holder = binding.rvChecklistItems.findViewHolderForAdapterPosition(position)
                if (holder is ChecklistAdapter.ChecklistViewHolder) {
                    holder.binding.etCheckItem.requestFocus()
                }
            }
        })
        binding.rvChecklistItems.adapter = checklistAdapter
        binding.rvChecklistItems.layoutManager = LinearLayoutManager(this)

        binding.btnAddChecklistItem.setOnClickListener {
            checklistAdapter.addItem()
            binding.rvChecklistItems.smoothScrollToPosition(checklistItems.size - 1)
        }
        // Add one empty item by default
        if (noteType == NoteType.CHECKLIST && checklistItems.isEmpty()) checklistAdapter.addItem()
    }

    // hàm show layout với notetype tương ứng
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

            NoteType.VIDEO, NoteType.PHOTO -> {
                binding.textInputContent.visibility = View.GONE
                binding.checklistContainer.visibility = View.GONE
                binding.layoutMedia.visibility = View.VISIBLE
            }
        }
    }

    // xử lý chọn color cho note
    private fun setupColorPicker() {
        colorAdapter = ColorPickerAdapter(listColor, colorSourceNames) { colorResId, colorName ->
            val color = ContextCompat.getColor(this, colorResId)
            binding.layoutAddNote.setBackgroundColor(color)
            selectedColorName = colorName
        }

        binding.rvColorPicker.adapter = colorAdapter
        binding.rvColorPicker.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    // hiển thị dialog thoát
    private fun showDialogBack() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Do you want to cancel creating this note?")
            .setMessage("Your note will not be saved.")
            .setIcon(R.drawable.ic_cancel)
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton("Yes") { _, _ ->
                onBackPressedDispatcher.onBackPressed()
            }
            .show()
    }

    //khởi tạo recyclerview và nút thêm ảnh
    private fun setupMediaSection() {
        mediaAdapter = MediaAdapter(imageList) { noteImage ->
            mediaAdapter.removeImage(noteImage)
        }
        
        val gridLayoutManager = GridLayoutManager(this, calculateSpanCount(imageList.size))
        
        binding.rvMediaItems.apply {
            layoutManager = gridLayoutManager
            adapter = mediaAdapter
            setHasFixedSize(false)
            itemAnimator?.changeDuration = 0
        }
        
        binding.btnAddImage.setOnClickListener {
            pickMultipleImagesLauncher.launch("image/*")
        }
    }

    // Calculate appropriate span count based on the number of items
    private fun calculateSpanCount(itemCount: Int): Int {
        return when {
            itemCount <= 1 -> 1    // Single column for 0-1 items
            itemCount <= 4 -> 2    // Two columns for 2-4 items
            else -> 3              // Three columns for 5+ items
        }
    }

    // Chon nhieu anh
    private fun handleMultipleImageSelection(uris: List<Uri>) {
        try {
            val noteImages = uris.map { uri ->
                NoteImage(
                    noteId = 0, // Will be set when note is created
                    imagePath = uri.toString()
                )
            }
            // Add to data source
            imageList.addAll(noteImages)
            // Update span count based on new size before updating the adapter
            val layoutManager = binding.rvMediaItems.layoutManager as GridLayoutManager
            layoutManager.spanCount = calculateSpanCount(imageList.size)
            // Update UI with the new images
            mediaAdapter.addMultipleImages(noteImages)
            // Force layout update
            binding.rvMediaItems.post { binding.rvMediaItems.requestLayout() }
            // Show success message
            val count = noteImages.size
            Log.d("AddNoteActivity","$count images added successfully")
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to add images: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AddNoteActivity", "Error adding multiple images", e)
        }
    }

    //hàm lưu note
    private fun saveNote() {
        val title = binding.textEditTitle.text?.toString()?.trim() ?: ""

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
                noteViewModel.createNote(note)
            }

            NoteType.PHOTO, NoteType.VIDEO -> {
                if (noteType == NoteType.PHOTO && imageList.isEmpty()) {
                    Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show()
                    return
                }
                
                val note = Note(
                    userId = currentUserId!!,
                    title = title,
                    noteType = noteType!!,
                    color = selectedColorName,
                    isPinned = isPinned,
                    isShared = isShared,
                    reminderTime = reminderTime
                )
                noteViewModel.createNote(note)
                // Note: Images will be saved in the observer when note creation is successful
            }

            NoteType.CHECKLIST -> {
                val items = checklistAdapter.getItems()
                val checklistString =
                    items.joinToString("\n") { (if (it.isChecked) "- [x] " else "- [ ] ") + it.text }
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
                noteViewModel.createNote(note)
            }

            else -> {}
        }
    }

    // hiển thị ra ngày ,tháng năm
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

    // hiển thị thời gian chọn
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
                    (selectedHour == now.get(Calendar.HOUR_OF_DAY)) && selectedMinute <= now.get(
                        Calendar.MINUTE
                    )
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

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateReminderDisplay() {
        val formatter = SimpleDateFormat("dd MM yyyy, HH:mm", Locale.getDefault())
        val formattedDate = formatter.format(Date(reminderTime!!))
        binding.tvReminderTime.text = formattedDate
    }


}
