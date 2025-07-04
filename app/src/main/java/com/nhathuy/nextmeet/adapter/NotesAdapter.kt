package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemNoteLayoutBinding
import com.nhathuy.nextmeet.model.ChecklistItem
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.model.NoteImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.material.imageview.ShapeableImageView
import com.nhathuy.nextmeet.utils.MediaGridLayoutManager

/**
* Adapter hiển thị danh sách ghi chú trong RecyclerView.
*
* Adapter này xử lý các loại ghi chú khác nhau (văn bản, checklist, ảnh/video)
* và cung cấp các listener cho các thao tác như mở ghi chú, ghim, hoặc truy cập tuỳ chọn khác.
*
* @property notes Danh sách mutable các đối tượng [Note] để hiển thị.
* @property onNoteClick Lambda được gọi khi một item ghi chú được nhấn.
* Nhận vào đối tượng [Note] vừa được nhấn.
* @property onNoteLongClick Lambda được gọi khi một item ghi chú bị nhấn giữ.
* Nhận vào đối tượng [Note] vừa được nhấn giữ.
* @property onPinClick Lambda được gọi khi icon ghim của ghi chú được nhấn.
* Nhận vào đối tượng [Note] tương ứng.
* @property onMoreClick Lambda được gọi khi nút "tuỳ chọn khác" của ghi chú được nhấn.
* Nhận vào đối tượng [Note] tương ứng.
 *
 * @version 2.0
 * @author TravisHuy (Ho Nhat Huy)
 * @since 04/06/2025
*/
class NotesAdapter(
    private var notes: MutableList<Note>,
    private var onNoteClick: (Note) -> Unit,
    private var onNoteLongClick: (Note) -> Unit,
    private var onPinClick: (Note) -> Unit,
    private var onMoreClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val reminderFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    private var noteImagesMap: MutableMap<Int, List<NoteImage>> = mutableMapOf()

    inner class NoteViewHolder(val binding: ItemNoteLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var checklistAdapter = ChecklistAdapter(emptyList<ChecklistItem>().toMutableList())
        private var mediaPreviewAdapter: MediaPreviewAdapter? = null
        private var isClickHandled = false
        private val clickHandler = Handler(Looper.getMainLooper())

        fun bind(note: Note) {
            with(binding) {
                tvTitle.text = if (note.title.isNotEmpty()) note.title else "Untitled"

                //set note type và icon
                setupNoteType(note.noteType)

                //set content với type
                setupContent(note)

                //set pin ẩn / hiện
                ivPin.visibility = if (note.isPinned) View.VISIBLE else View.GONE

                //cài đặt ngày
                tvCreatedDate.text = dateFormatter.format(Date(note.createdAt))

                //set reminder
                setupReminder(note.reminderTime)

                //set background color
                setupBackgroundColor(note.color)

                //set color indiator
                setupColorIndicator(note.color)


                // click listener
                root.setOnClickListener {
                    handleNoteClick(note)
                }

                root.setOnLongClickListener {
//                    btnMoreOptions.visibility = View.VISIBLE
                    onNoteLongClick(note)
                    true
                }

                ivPin.setOnClickListener { onPinClick(note) }

                btnMoreOptions.setOnClickListener { onMoreClick(note) }
            }
        }

        private fun handleNoteClick(note: Note) {
            if (!isClickHandled) {
                isClickHandled = true
                onNoteClick(note)

                // Reset click flag sau 300ms để tránh spam clicks
                clickHandler.postDelayed({
                    isClickHandled = false
                }, 300)
            }
        }

        private fun setupNoteType(noteType: NoteType) {
            with(binding) {
                when (noteType) {
                    NoteType.TEXT -> {
                        ivNoteType.setImageResource(R.drawable.ic_text_fields)
                        ivNoteType.contentDescription = root.context.getString(R.string.text)
                        tvNoteType.text = root.context.getString(R.string.text)
                    }

                    NoteType.CHECKLIST -> {
                        ivNoteType.setImageResource(R.drawable.ic_checklist)
                        ivNoteType.contentDescription = root.context.getString(R.string.checklist)
                        tvNoteType.text = root.context.getString(R.string.checklist)
                    }

                    NoteType.PHOTO, NoteType.VIDEO -> {
                        ivNoteType.setImageResource(R.drawable.ic_photo)
                        ivNoteType.contentDescription = root.context.getString(R.string.image)
                        tvNoteType.text = root.context.getString(R.string.image)
                    }
                }
            }
        }

        private fun setupContent(note: Note) {
            with(binding) {
                when (note.noteType) {
                    NoteType.TEXT -> {
                        tvContent.visibility = View.VISIBLE
                        rvChecklistPreview.visibility = View.GONE
                        tvChecklistCount.visibility = View.GONE
                        tvContent.text = note.content
                        rvMediaPreview.visibility = View.GONE
                    }

                    NoteType.CHECKLIST -> {
                        tvContent.visibility = View.GONE
                        rvChecklistPreview.visibility = View.VISIBLE
                        setupCheckListPreview(note)
                        rvMediaPreview.visibility = View.GONE
                    }

                    NoteType.PHOTO, NoteType.VIDEO -> {
                        tvContent.visibility = if (note.content.isNotEmpty()) View.VISIBLE else View.GONE
                        tvContent.text = note.content
                        rvChecklistPreview.visibility = View.GONE
                        tvChecklistCount.visibility = View.GONE
                        rvMediaPreview.visibility = View.VISIBLE

                        setupMediaPreview(note)
                    }
                }
            }
        }

        private fun setupCheckListPreview(note: Note) {
            with(binding) {
                val checklistItems = parseChecklistString(note.checkListItems ?: "")
                if (checklistItems.isNotEmpty()) {
                    if (rvChecklistPreview.layoutManager == null) {
                        rvChecklistPreview.layoutManager = LinearLayoutManager(binding.root.context)
                    }

                    rvChecklistPreview.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    rvChecklistPreview.isNestedScrollingEnabled = false


                    val maxItems = 3
                    val visibleItems = checklistItems.take(maxItems)

                    checklistAdapter = ChecklistAdapter(
                        visibleItems.toMutableList(),
                        isPreviewMode = true,
                        onNoteClick = {
                            handleNoteClick(note)
                        }
                    )
                    rvChecklistPreview.adapter = checklistAdapter

                    val remainingCount = checklistItems.size - maxItems
                    if (remainingCount > 0) {
                        tvChecklistCount.visibility = View.VISIBLE
                        tvChecklistCount.text = "+$remainingCount more"
                        tvChecklistCount.setOnClickListener {
                            handleNoteClick(note)
                        }
                    } else {
                        tvChecklistCount.visibility = View.GONE
                    }
                } else {
                    rvChecklistPreview.adapter = null
                    tvChecklistCount.visibility = View.GONE
                }
            }
        }

        // chuyển một chuỗi checklist thành danh sách ChecklistItem
        private fun parseChecklistString(checklistString: String): List<ChecklistItem> {
            if (checklistString.isBlank()) return emptyList()

            return checklistString.split("\n").mapNotNull { line ->
                when {
                    line.startsWith("- [ ]") -> ChecklistItem(
                        text = line.removePrefix("- [ ]").trim(), isChecked = false
                    )

                    line.startsWith("- [x]") -> ChecklistItem(
                        text = line.removePrefix("- [x]").trim(), isChecked = true
                    )

                    else -> null
                }
            }.filter { it.text.isNotBlank() }
        }

        private fun setupMediaPreview(note : Note) {
            with(binding){
                val images = noteImagesMap[note.id] ?: emptyList()
                if(images.isNotEmpty()){
                    Log.d("NoteAdapter","Setting up media preview for note ${note.id} with ${images.size} images")

                    val maxDisplayImages = 4
                    val displayImages = images.take(maxDisplayImages)
                        .toMutableList()

                    val layoutManager = MediaGridLayoutManager(root.context, displayImages.size)
                    rvMediaPreview.layoutManager = layoutManager


                    // Setup adapter
                    mediaPreviewAdapter = MediaPreviewAdapter(displayImages) { image, position ->
                        // Xử lý click vào ảnh - có thể mở gallery hoặc full screen
                        handleNoteClick(note)
                    }
                    rvMediaPreview.adapter = mediaPreviewAdapter


//                    if (images.size > maxDisplayImages && displayImages.size == maxDisplayImages) {
//                        setupMoreImagesOverlay(displayImages.last(), images.size - maxDisplayImages)
//                    }

                    rvMediaPreview.isNestedScrollingEnabled = false
                    rvMediaPreview.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                }
            }
        }

        // Method để setup overlay cho ảnh cuối khi có nhiều ảnh
        private fun setupMoreImagesOverlay(lastImage: NoteImage, remainingCount: Int) {

        }

        private fun setupReminder(reminderTime: Long?) {
            with(binding) {
                if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                    reminderIndicator.visibility = View.VISIBLE

                    val reminderDate = Date(reminderTime)
                    val today = Calendar.getInstance()
                    val reminderCalendar = Calendar.getInstance().apply {
                        time = reminderDate
                    }

                    val reminderText = when {
                        isSameDay(today, reminderCalendar) -> root.context.getString(R.string.today)
                        isTomorrowDay(today, reminderCalendar) -> root.context.getString(R.string.tomorrow)

                        else -> reminderFormatter.format(reminderDate)
                    }

                    tvReminderDate.text = reminderText
                }
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        private fun isTomorrowDay(today: Calendar, target: Calendar): Boolean {
            val tomorrow = today.clone() as Calendar
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            return isSameDay(tomorrow, target)
        }

        private fun setupBackgroundColor(colorName: String) {
            val colorResId = getColorResourceId(colorName)
            val color = ContextCompat.getColor(binding.root.context, colorResId)
            binding.cardNoteLayout.setCardBackgroundColor(color)
        }

        private fun setupColorIndicator(colorName: String) {
            val colorResId = getColorResourceId(colorName)
            val color = ContextCompat.getColor(binding.root.context, colorResId)
//            binding.colorIndicator.setBackgroundColor(color)
        }

        private fun getColorResourceId(colorName: String): Int {
            return when (colorName) {
                "color_white" -> R.color.color_white
                "color_red" -> R.color.color_red
                "color_orange" -> R.color.color_orange
                "color_yellow" -> R.color.color_yellow
                "color_green" -> R.color.color_green
                "color_teal" -> R.color.color_teal
                "color_blue" -> R.color.color_blue
                "color_dark_blue" -> R.color.color_dark_blue
                "color_purple" -> R.color.color_purple
                "color_pink" -> R.color.color_pink
                "color_brown" -> R.color.color_brown
                "color_gray" -> R.color.color_gray
                else -> R.color.color_white
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesAdapter.NoteViewHolder {
        val binding =
            ItemNoteLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotesAdapter.NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        notes.clear()
        notes.addAll(newNotes)
        diffResult.dispatchUpdatesTo(this)
    }

    // New: update notes and images map directly, no DB/DAO logic here
    fun updateNotesWithImages(newNotes: List<Note>, noteImagesMap: Map<Int, List<NoteImage>>) {
        notes.clear()
        notes.addAll(newNotes)
        this.noteImagesMap.clear()
        this.noteImagesMap.putAll(noteImagesMap)
        notifyDataSetChanged()
    }

    //lấy vị trí của note
    fun getPositionOfNote(noteId: Int): Int {
        return notes.indexOfFirst { it.id == noteId }
    }

    private class NotesDiffCallback(
        private val oldNotes: List<Note>,
        private val newNotes: List<Note>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldNotes.size

        override fun getNewListSize(): Int = newNotes.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldNotes[oldItemPosition].id == newNotes[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldNote = oldNotes[oldItemPosition]
            val newNote = newNotes[newItemPosition]

            return oldNote.title == newNote.title &&
                    oldNote.content == newNote.content &&
                    oldNote.isPinned == newNote.isPinned &&
                    oldNote.color == newNote.color &&
                    oldNote.reminderTime == newNote.reminderTime &&
                    oldNote.updatedAt == newNote.updatedAt
        }

    }
}

