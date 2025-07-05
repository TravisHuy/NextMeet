package com.nhathuy.nextmeet.fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.NotesAdapter
import com.nhathuy.nextmeet.databinding.BottomSheetNoteOptionsBinding
import com.nhathuy.nextmeet.databinding.FragmentNotesBinding
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.resource.NoteUiState
import com.nhathuy.nextmeet.ui.AddNoteActivity
import com.nhathuy.nextmeet.ui.EditNoteActivity
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.utils.NavigationCallback
import com.nhathuy.nextmeet.viewmodel.NoteViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * NoteFragment fragment - Simplified version without appbar and chip filters
 * Hiển thị tất cả notes với FAB menu animation
 */
@AndroidEntryPoint
class NotesFragment : Fragment(), NavigationCallback {
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private var isFabMenuOpen = false
    private var currentUserId: Int = 0

    private lateinit var notesAdapter: NotesAdapter
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var userViewModel: UserViewModel

    // Map lưu trữ danh sách ảnh cho mỗi note
    private val noteImagesMap = mutableMapOf<Int, List<NoteImage>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        setupUserInfo()
        setupViews()
        setupRecyclerView()
        setupFabMenu()
        setupObservers()
    }

    private fun setupUserInfo() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                startObservingNotes()
            }
        }
    }

    private fun setupViews() {
        // Initial state
        updateEmptyState()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                noteViewModel.uiState.collect { state ->
                    when (state) {
                        is NoteUiState.Loading -> {
                            // Có thể thêm loading indicator nếu cần
                        }
                        is NoteUiState.NotesLoaded -> {
                            handleNotesLoaded(state.notes)
                        }
                        is NoteUiState.Error -> {
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                        is NoteUiState.NotePinToggled -> {
                            val message = if (state.isPinned) "Đã ghim ghi chú" else "Đã bỏ ghim ghi chú"
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                        }
                        is NoteUiState.NoteDeleted -> {
                            Snackbar.make(binding.root, "Đã xóa ghi chú thành công", Snackbar.LENGTH_SHORT).show()
                        }
                        is NoteUiState.NoteDuplicated -> {
                            Snackbar.make(binding.root, "Đã tạo bản sao ghi chú", Snackbar.LENGTH_SHORT).show()
                        }
                        is NoteUiState.NoteShared -> {
                            val shareContent = state.shareResult.shareContent
                            if (!shareContent.isNullOrBlank()) {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareContent)
                                }
                                startActivity(Intent.createChooser(shareIntent, "Share Note"))
                            } else {
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                        is NoteUiState.ReminderUpdated -> {
                            val message = state.message ?: "Đã cập nhật lời nhắc"
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Handle other states if needed
                        }
                    }
                }
            }
        }
    }

    private fun startObservingNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            noteViewModel.getAllNotes(currentUserId).collect { notes ->
                // The notes are already handled in setupObservers() via NotesLoaded state
                // This collect is just to trigger the Flow
            }
        }
    }

    private fun handleNotesLoaded(notes: List<Note>) {
        Log.d("NotesFragment", "Handling ${notes.size} notes")

        // Clear previous images map
        noteImagesMap.clear()

        // Load images for photo notes
        loadImagesForPhotoNotes(notes)
    }

    private fun loadImagesForPhotoNotes(notes: List<Note>) {
        val photoNotes = notes.filter { it.noteType == NoteType.PHOTO }

        if (photoNotes.isEmpty()) {
            // Nếu không có photo notes, update UI ngay
            displayNotes(notes)
            return
        }

        var loadedCount = 0
        val totalPhotoNotes = photoNotes.size

        // Load images for each photo note
        photoNotes.forEach { note ->
            noteViewModel.getImagesForNote(note.id) { images ->
                Log.d("NotesFragment", "Loaded ${images.size} images for note ID: ${note.id}")
                noteImagesMap[note.id] = images
                loadedCount++

                // Update UI when all images are loaded
                if (loadedCount == totalPhotoNotes) {
                    displayNotes(notes)
                }
            }
        }
    }

    /**
     * Hiển thị tất cả notes với pinned notes ở trên
     */
    private fun displayNotes(notes: List<Note>) {
        val combinedNotes = mutableListOf<Note>()

        // Thêm pinned notes trước
        val pinnedNotes = notes.filter { it.isPinned }
        combinedNotes.addAll(pinnedNotes)

        // Thêm unpinned notes sau
        val unpinnedNotes = notes.filter { !it.isPinned }
        combinedNotes.addAll(unpinnedNotes)

        // Update adapter với notes và images
        notesAdapter.updateNotesWithImages(combinedNotes, noteImagesMap)
        updateEmptyState(combinedNotes.isEmpty())
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            notes = mutableListOf(),
            onNoteClick = { note -> openNoteForEdit(note) },
            onNoteLongClick = { note -> handleNoteLongClick(note) },
            onPinClick = { note -> togglePin(note) },
            onMoreClick = { note -> showNoteOptions(note) }
        )

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS

        binding.rvNotes.apply {
            this.layoutManager = layoutManager
            adapter = notesAdapter
            setHasFixedSize(false)
        }
    }

    private fun openNoteForEdit(note: Note) {
        val intent = Intent(requireContext(), AddNoteActivity::class.java)
        intent.putExtra(Constant.EXTRA_NOTE_ID, note.id)
        startActivity(intent)
    }

    private fun handleNoteLongClick(note: Note) {
        showNoteOptions(note)
    }

    private fun togglePin(note: Note) {
        noteViewModel.togglePin(note.id)
    }

    private fun showNoteOptions(note: Note) {
        val dialogBinding = BottomSheetNoteOptionsBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvNoteTitle.text = note.title

        dialogBinding.llPin.setOnClickListener {
            togglePin(note)
            dialog.dismiss()
        }
        dialogBinding.llShare.setOnClickListener {
            noteViewModel.toggleShare(note.id)
            dialog.dismiss()
        }
        dialogBinding.llReminder.setOnClickListener {
            dialog.dismiss()
            showReminderDialog(note)
        }
        dialogBinding.llDuplicate.setOnClickListener {
            duplicateNote(note)
            dialog.dismiss()
        }
        dialogBinding.llDelete.setOnClickListener {
            showDeleteConfirmation(note)
            dialog.dismiss()
        }

        dialogBinding.root.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(R.drawable.rounded_background)
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.BOTTOM)
        }
    }

    private fun showReminderDialog(note: Note) {
        val constraintBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày nhắc nhở")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Chọn giờ nhắc nhở")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)
                calendar.set(Calendar.SECOND, 0)
                val reminderTime = calendar.timeInMillis

                if (reminderTime <= System.currentTimeMillis()) {
                    Snackbar.make(binding.root, "Vui lòng chọn thời gian trong tương lai", Snackbar.LENGTH_SHORT).show()
                } else {
                    noteViewModel.updateReminder(note.id, reminderTime)
                }
            }
            timePicker.show(parentFragmentManager, "time_picker")
        }
        datePicker.show(parentFragmentManager, "date_picker")
    }

    private fun showDeleteConfirmation(note: Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                deleteNote(note)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteNote(note: Note) {
        if (note.id != 0) {
            if (note.noteType == NoteType.PHOTO) {
                noteViewModel.deleteImagesByNoteId(note.id)
            }
            noteViewModel.deleteNote(note.id)
        } else {
            Snackbar.make(binding.root, "Không thể xóa ghi chú này", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun duplicateNote(note: Note) {
        noteViewModel.duplicateNote(note.id)
    }

    private fun updateEmptyState(isEmpty: Boolean = true) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setupFabMenu() {
        binding.fabAddNote.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabMenuOverlay.setOnClickListener {
            closeFabMenu()
        }

        binding.fabTextNote.setOnClickListener {
            closeFabMenu()
            openAddNote(NoteType.TEXT)
        }

        binding.fabImageNote.setOnClickListener {
            closeFabMenu()
            openAddNote(NoteType.PHOTO)
        }

        binding.fabChecklistNote.setOnClickListener {
            closeFabMenu()
            openAddNote(NoteType.CHECKLIST)
        }
    }

    private fun openAddNote(noteType: NoteType) {
        val intent = Intent(requireContext(), AddNoteActivity::class.java)
        intent.putExtra(Constant.EXTRA_NOTE_TYPE, noteType)
        startActivity(intent)
    }

    private fun toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }

    private fun openFabMenu() {
        isFabMenuOpen = true

        binding.fabMenuOverlay.visibility = View.VISIBLE
        binding.fabMenuContainer.visibility = View.VISIBLE

        binding.fabMenuOverlay.alpha = 0f
        binding.fabMenuOverlay.animate()
            .alpha(1f)
            .setDuration(250)
            .start()

        binding.fabAddNote.animate()
            .rotation(45f)
            .setDuration(250)
            .start()

        animateSubFabIn(binding.fabTextNote, binding.tvTextNote, 0)
        animateSubFabIn(binding.fabImageNote, binding.tvCheckListNote, 90)
        animateSubFabIn(binding.fabChecklistNote, binding.tvImageNote, 180)
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false

        binding.fabMenuOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fabMenuOverlay.visibility = View.GONE
            }
            .start()

        binding.fabAddNote.animate()
            .rotation(0f)
            .setDuration(200)
            .start()

        animateSubFabOut(binding.fabChecklistNote, binding.tvImageNote, 0)
        animateSubFabOut(binding.fabImageNote, binding.tvCheckListNote, 70)
        animateSubFabOut(binding.fabTextNote, binding.tvTextNote, 140) {
            binding.fabMenuContainer.visibility = View.GONE
        }
    }

    private fun animateSubFabIn(fab: View, label: View, delay: Long) {
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.alpha = 0f
        fab.translationY = 50f

        label.scaleX = 0f
        label.scaleY = 0f
        label.alpha = 0f
        label.translationY = 50f

        fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        label.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .setStartDelay(delay + 50)
            .setInterpolator(OvershootInterpolator(1.1f))
            .start()
    }

    private fun animateSubFabOut(
        fab: View,
        label: View,
        delay: Long,
        endAction: (() -> Unit)? = null
    ) {
        label.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .translationY(50f)
            .setDuration(150)
            .setStartDelay(delay)
            .start()

        fab.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .translationY(50f)
            .setDuration(150)
            .setStartDelay(delay + 30)
            .withEndAction { endAction?.invoke() }
            .start()
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId != 0) {
            refreshNotes()
        }
    }

    private fun refreshNotes() {
        // Clear cache cũ
        noteImagesMap.clear()

        // Trigger refresh by starting observation again
        startObservingNotes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun triggerAddAction() {
        openAddNote(NoteType.TEXT)
    }
}