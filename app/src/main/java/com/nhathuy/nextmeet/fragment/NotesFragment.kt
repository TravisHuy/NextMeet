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
 * NoteFragment fragment thể hiện logic với FAB menu animation
 */
@AndroidEntryPoint
class NotesFragment : Fragment() , NavigationCallback{
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private var isFabMenuOpen = false

    private var currentUserId: Int = 0

    private lateinit var notesAdapter: NotesAdapter

    private lateinit var noteViewModel: NoteViewModel

    private lateinit var userViewModel: UserViewModel

    private var allNotes = listOf<Note>()

    private var pinnedNotes = listOf<Note>()

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
        setObserver()
        setupViews()
        setupRecyclerView()
        setupChipFilters()
        setupFabMenu()

    }

    private fun setupUserInfo() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                setupObserverNotes()
            }
        }
    }

    private fun setupViews() {
        //cài đặt chip filters nếu đã check
        binding.chipAll.isChecked = true
        // initial state
        updateEmptyState()
    }

    private fun setObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                noteViewModel.uiState.collect { state ->
                    when (state) {
                        is NoteUiState.Loading -> {

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
                            // Nếu chia sẻ thành công, lấy nội dung và thực hiện Intent share
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

                        }
                    }
                }
            }
        }
    }

    private fun setupObserverNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                noteViewModel.getAllNotes(currentUserId).collect { notes ->
                    allNotes = notes
                    pinnedNotes = notes.filter { it.isPinned }

                    // Tải ảnh cho tất cả note kiểu PHOTO
                    loadImagesForPhotoNotes(notes)

                    when {
                        binding.chipAll.isChecked -> showAllNotes()
                        binding.chipPinned.isChecked -> showPinnedNotes()
                        binding.chipReminder.isChecked -> showReminderNotes()
                    }
                }
            }
        }
    }

    // Hàm mới để tải ảnh cho tất cả note kiểu PHOTO
    private fun loadImagesForPhotoNotes(notes: List<Note>) {
        val photoNotes = notes.filter { it.noteType == NoteType.PHOTO }
        if (photoNotes.isEmpty()) return

        var loadedCount = 0
        photoNotes.forEach { note ->
            noteViewModel.getImagesForNote(note.id) { images ->
                Log.d("NotesFragment", "Loaded ${images.size} images for note ID: ${note.id}")
                noteImagesMap[note.id] = images
                loadedCount++

                // Chỉ cập nhật khi tất cả ảnh đã được load
                if (loadedCount == photoNotes.size) {
                    when {
                        binding.chipAll.isChecked -> showAllNotes()
                        binding.chipPinned.isChecked -> showPinnedNotes()
                        binding.chipReminder.isChecked -> showReminderNotes()
                    }
                }
            }
        }
    }

    // hiển thị tất cả note
    private fun showAllNotes() {
        val combinedNotes = mutableListOf<Note>()
        combinedNotes.addAll(pinnedNotes)

        val unpinnedNotes = allNotes.filter {
            !it.isPinned
        }
        combinedNotes.addAll(unpinnedNotes)

        // Sử dụng hàm updateNotesWithImages thay vì updateNotes
        notesAdapter.updateNotesWithImages(combinedNotes, noteImagesMap)
        updateEmptyState(combinedNotes.isEmpty())
    }

    // hiển thị note pin
    private fun showPinnedNotes() {
        // Sử dụng hàm updateNotesWithImages thay vì updateNotes
        notesAdapter.updateNotesWithImages(pinnedNotes, noteImagesMap)
        updateEmptyState(pinnedNotes.isEmpty())
    }

    //hiển thị note đã pin
    private fun showReminderNotes() {
        val reminderNotes = allNotes.filter {
            it.reminderTime != null && it.reminderTime > System.currentTimeMillis()
        }

        // Sử dụng hàm updateNotesWithImages thay vì updateNotes
        notesAdapter.updateNotesWithImages(reminderNotes, noteImagesMap)
        updateEmptyState(reminderNotes.isEmpty())
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

    // xử lý khi onclick vao item chuyển sao edit
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
            // Gọi toggleShare thay vì shareNote trực tiếp
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

        val constraintBuilder =
            CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now())


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

    // show dialog delete note
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
            if(note.noteType == NoteType.PHOTO) {
                // Xóa ảnh liên quan đến note nếu là note kiểu PHOTO
                noteViewModel.deleteImagesByNoteId(note.id)
            }
            // xóa
            noteViewModel.deleteNote(note.id)
        }
        else{
            Snackbar.make(binding.root,"Không thể xóa ghi chú này", Snackbar.LENGTH_SHORT).show()
        }
    }

    // tao ban sao cho note
    private fun duplicateNote(note:Note){
        noteViewModel.duplicateNote(note.id)
    }


    private fun setupChipFilters() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                clearOtherChips(binding.chipAll.id)
                showAllNotes()
            }
        }
        binding.chipPinned.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                clearOtherChips(binding.chipPinned.id)
                showPinnedNotes()
            }
        }
        binding.chipReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                clearOtherChips(binding.chipReminder.id)
                showReminderNotes()
            }
        }
    }

    private fun clearOtherChips(checkedChipId: Int) {
        when (checkedChipId) {
            binding.chipAll.id -> {
                binding.chipPinned.isChecked = false
                binding.chipReminder.isChecked = false
            }

            binding.chipPinned.id -> {
                binding.chipAll.isChecked = false
                binding.chipReminder.isChecked = false
            }

            binding.chipReminder.id -> {
                binding.chipPinned.isChecked = false
                binding.chipAll.isChecked = false
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean = allNotes.isEmpty()) {
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

        // Hiển thị lớp phủ và vùng chứa menu
        binding.fabMenuOverlay.visibility = View.VISIBLE
        binding.fabMenuContainer.visibility = View.VISIBLE

        // Animate overlay mờ dần
        binding.fabMenuOverlay.alpha = 0f
        binding.fabMenuOverlay.animate()
            .alpha(1f)
            .setDuration(250)
            .start()

        // Chuyển main FAB
        binding.fabAddNote.animate()
            .rotation(45f)
            .setDuration(250)
            .start()

        // Animate các FAB phụ với thời gian
        animateSubFabIn(binding.fabTextNote, binding.tvTextNote, 0)
        animateSubFabIn(binding.fabImageNote, binding.tvCheckListNote, 90)
        animateSubFabIn(binding.fabChecklistNote, binding.tvImageNote, 180)

    }

    private fun closeFabMenu() {
        isFabMenuOpen = false

        // làm mờ fab menu overlay
        binding.fabMenuOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fabMenuOverlay.visibility = View.GONE
            }
            .start()

        // Rotate main FAB back
        binding.fabAddNote.animate()
            .rotation(0f)
            .setDuration(200)
            .start()

        // Animate sub FABs
        animateSubFabOut(binding.fabChecklistNote, binding.tvImageNote, 0)
        animateSubFabOut(binding.fabImageNote, binding.tvCheckListNote, 70)
        animateSubFabOut(binding.fabTextNote, binding.tvTextNote, 140) {
            binding.fabMenuContainer.visibility = View.GONE
        }
    }

    private fun animateSubFabIn(fab: View, label: View, delay: Long) {
        // Khởi tạo trạng thái ban đầu
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.alpha = 0f
        fab.translationY = 50f

        label.scaleX = 0f
        label.scaleY = 0f
        label.alpha = 0f
        label.translationY = 50f

        // Animate FAB
        fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        // Animate label với delay nhỏ hơn
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
        // Animate label trước
        label.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .translationY(50f)
            .setDuration(150)
            .setStartDelay(delay)
            .start()

        // Animate FAB
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun triggerAddAction() {
        openAddNote(NoteType.TEXT)
    }
}

