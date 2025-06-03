package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemNoteLayoutBinding
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private var notes: MutableList<Note>,
    private var onNoteClick: (Note) -> Unit,
    private var onNoteLongClick: (Note) -> Unit,
    private var onPinClick: (Note) -> Unit,
    private var onMoreClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val reminderFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    inner class NoteViewHolder(val binding: ItemNoteLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
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
                    onNoteClick(note)
                }

                root.setOnLongClickListener {
                    onNoteLongClick(note)
                    true
                }

                ivPin.setOnClickListener { onPinClick(note) }

                btnMoreOptions.setOnClickListener { onMoreClick(note) }
            }
        }

        private fun setupNoteType(noteType:NoteType){
            with(binding){
                when(noteType){
                    NoteType.TEXT -> {
                        ivNoteType.setImageResource(R.drawable.ic_text_fields)
                        ivNoteType.contentDescription = "Text"
                    }
                    NoteType.CHECKLIST ->{
                        ivNoteType.setImageResource(R.drawable.ic_checklist)
                        ivNoteType.contentDescription = "Checklist"
                    }
                    NoteType.PHOTO, NoteType.VIDEO -> {
                        ivNoteType.setImageResource(R.drawable.ic_photo)
                        ivNoteType.contentDescription = "Image"
                    }
                }
            }
        }
        private fun setupContent(note:Note){
            with(binding){
                when(note.noteType){
                    NoteType.TEXT -> {
                        tvContent.visibility = View.VISIBLE
                        checklistPreview.visibility = View.GONE
                        tvContent.text = note.content
                    }
                    NoteType.CHECKLIST -> {
                        tvContent.visibility = View.GONE
                        checklistPreview.visibility = View.VISIBLE
                        setupCheckListPreview(note)
                    }
                    NoteType.PHOTO,NoteType.VIDEO -> {
                        tvContent.visibility = View.GONE
                        checklistPreview.visibility = View.GONE

                    }
                }
            }
        }

        private fun setupCheckListPreview(note: Note){
            with(binding) {
                // Show placeholder checklist items
                tvChecklistCount.text = "View checklist items"
            }
        }

        private fun setupReminder(reminderTime:Long?){
            with(binding){
                if(reminderTime != null && reminderTime > System.currentTimeMillis()){
                    reminderIndicator.visibility = View.VISIBLE

                    val reminderDate = Date(reminderTime)
                    val today = Calendar.getInstance()
                    val reminderCalendar = Calendar.getInstance().apply {
                        time = reminderDate
                    }

                    val reminderText = when {
                        isSameDay(today,reminderCalendar) -> "Today"
                        isTomorrowDay(today ,reminderCalendar) -> "Tomorrow"

                        else -> reminderFormatter.format(reminderDate)
                    }

                    tvReminderDate.text = reminderText
                }
            }
        }

        private fun isSameDay(cal1:Calendar, cal2:Calendar):Boolean{
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)  &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        private fun isTomorrowDay(today:Calendar,target:Calendar):Boolean{
            val tomorrow = today.clone() as Calendar
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            return isSameDay(tomorrow, target)
        }

        private fun setupBackgroundColor(colorName:String){
            val colorResId = getColorResourceId(colorName)
            val color = ContextCompat.getColor(binding.root.context,colorResId)

        }
        private fun setupColorIndicator(colorName: String){
            val colorResId = getColorResourceId(colorName)
            val color = ContextCompat.getColor(binding.root.context, colorResId)
            binding.colorIndicator.setBackgroundColor(color)
        }
        private fun getColorResourceId(colorName: String):Int{
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