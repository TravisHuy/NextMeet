package com.nhathuy.nextmeet.adapter

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemNoteRecentsBinding
import com.nhathuy.nextmeet.model.ChecklistItem
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.NoteType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NoteRecentAdapter(private var notes: MutableList<Note>) :
    RecyclerView.Adapter< NoteRecentAdapter.NoteRecentViewHolder>() {
    private var noteImagesMap: MutableMap<Int, List<NoteImage>> = mutableMapOf()

    inner class NoteRecentViewHolder(val binding: ItemNoteRecentsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var checklistAdapter = ChecklistAdapter(emptyList<ChecklistItem>().toMutableList())
        fun bind(note: Note) {
            with(binding) {
                tvNoteRecentTitle.text = note.title
                setupContent(note)
                tvNoteRecentTime.text = formatTime(note.createdAt)
            }
        }

        // In your NoteRecentAdapter, replace the setupContent method with this fixed version:

        private fun setupContent(note: Note) {
            with(binding) {
                when(note.noteType) {
                    NoteType.TEXT -> {
                        tvNoteRecentContent.text = note.content
                        tvNoteRecentContent.visibility = View.VISIBLE
                        ivNoteRecent.visibility = View.GONE
                        rvChecklistNoteRecent.visibility = View.GONE
                    }
                    NoteType.CHECKLIST -> {
                        setupCheckListPreview(note)
                        tvNoteRecentContent.visibility = View.GONE
                        ivNoteRecent.visibility = View.GONE
                        rvChecklistNoteRecent.visibility = View.VISIBLE
                    }
                    NoteType.VIDEO, NoteType.PHOTO -> {
                        tvNoteRecentContent.visibility = View.GONE
                        rvChecklistNoteRecent.visibility = View.GONE

                        val mediaView = binding.ivNoteRecent
                        val images = noteImagesMap[note.id] ?: emptyList()

                        if (images.isNotEmpty()) {
                            Log.d("NotesAdapter", "Loading image: ${images[0].imagePath}")
                            mediaView.visibility = View.VISIBLE

                            Glide.with(mediaView.context)
                                .load(images[0].imagePath) // Use the path directly
                                .centerCrop()
                                .placeholder(R.drawable.ic_photo)
                                .error(R.drawable.ic_photo) // Add error placeholder
                                .into(mediaView)
                        } else {
                            Log.d("NotesAdapter", "No images found for note ${note.id}")
                            mediaView.visibility = View.GONE
                        }
                    }
                }
            }
        }

        private fun setupCheckListPreview(note: Note) {
            with(binding) {
                val checklistItems = parseChecklistString(note.checkListItems ?: "")
                if (checklistItems.isNotEmpty()) {
                    if (rvChecklistNoteRecent.layoutManager == null) {
                        rvChecklistNoteRecent.layoutManager = LinearLayoutManager(binding.root.context)
                    }
                    val maxItems = 3
                    val visibleItems = checklistItems.take(maxItems)

                    checklistAdapter = ChecklistAdapter(visibleItems.toMutableList(), isPreviewMode = true)
                    rvChecklistNoteRecent.adapter = checklistAdapter
                }
                else{
                    rvChecklistNoteRecent.adapter = null
                }
            }
        }

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

    }

    fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"

            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "${minutes}m ago"
            }

            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "${hours}h ago"
            }

            diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "${days}d ago"
            }

            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): NoteRecentViewHolder {
        val binding =
            ItemNoteRecentsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteRecentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NoteRecentViewHolder, position: Int
    ) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int  = notes.size

    fun updateNotesWithImages(newNotes: List<Note>, noteImagesMap: Map<Int, List<NoteImage>>) {
        notes.clear()
        notes.addAll(newNotes)
        this.noteImagesMap.clear()
        this.noteImagesMap.putAll(noteImagesMap)
        notifyDataSetChanged()
    }
}