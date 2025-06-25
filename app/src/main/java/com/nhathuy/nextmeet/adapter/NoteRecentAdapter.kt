package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.databinding.ItemNoteRecentsBinding
import com.nhathuy.nextmeet.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NoteRecentAdapter() :
    ListAdapter<Note, NoteRecentAdapter.NoteRecentViewHolder>(DiffCallback) {

    inner class NoteRecentViewHolder(val binding: ItemNoteRecentsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            with(binding) {
                tvNoteRecentTitle.text = note.title
                tvNoteRecentContent.text = note.content
                tvNoteRecentTime.text = formatTime(note.createdAt)
            }
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
        parent: ViewGroup,
        viewType: Int
    ): NoteRecentViewHolder {
        val binding =
            ItemNoteRecentsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteRecentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NoteRecentViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }


    companion object DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(
            oldItem: Note,
            newItem: Note
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Note,
            newItem: Note
        ): Boolean {
            return oldItem == newItem
        }

    }
}