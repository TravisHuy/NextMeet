package com.nhathuy.nextmeet.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemSearchHeaderBinding
import com.nhathuy.nextmeet.databinding.ItemSearchResultBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.UniversalSearchResult
import com.nhathuy.nextmeet.resource.SearchResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchResultsAdapter(
    private val context: Context,
    private val onAppointmentClick: (AppointmentPlus) -> Unit,
    private val onContactClick: (Contact) -> Unit,
    private val onNoteClick: (Note) -> Unit
) : ListAdapter<SearchResultItem, RecyclerView.ViewHolder>(DiffCallback) {


    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResultItem.HeaderItem -> VIEW_TYPE_HEADER
            is SearchResultItem.AppointmentItem -> VIEW_TYPE_APPOINTMENT
            is SearchResultItem.ContactItem -> VIEW_TYPE_CONTACT
            is SearchResultItem.NoteItem -> VIEW_TYPE_NOTE
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemSearchHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }

            VIEW_TYPE_APPOINTMENT -> {
                val binding = ItemSearchResultBinding.inflate(inflater, parent, false)
                AppointmentViewHolder(binding)
            }

            VIEW_TYPE_CONTACT -> {
                val binding = ItemSearchResultBinding.inflate(inflater, parent, false)
                ContactViewHolder(binding)
            }

            VIEW_TYPE_NOTE -> {
                val binding = ItemSearchResultBinding.inflate(inflater, parent, false)
                NoteViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = getItem(position)) {
            is SearchResultItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is SearchResultItem.AppointmentItem -> (holder as AppointmentViewHolder).bind(item.appointment)
            is SearchResultItem.ContactItem -> (holder as ContactViewHolder).bind(item.contact)
            is SearchResultItem.NoteItem -> (holder as NoteViewHolder).bind(item.note)
        }
    }
    fun submitSearchResults(results: UniversalSearchResult) {
        val items = mutableListOf<SearchResultItem>()

        // Add appointments
        if (results.appointments.isNotEmpty()) {
            items.add(SearchResultItem.HeaderItem(context.getString(R.string.appointment), results.appointments.size))
            items.addAll(results.appointments.map { SearchResultItem.AppointmentItem(it) })
        }

        // Add contacts
        if (results.contacts.isNotEmpty()) {
            items.add(SearchResultItem.HeaderItem(context.getString(R.string.contact), results.contacts.size))
            items.addAll(results.contacts.map { SearchResultItem.ContactItem(it) })
        }

        // Add notes
        if (results.notes.isNotEmpty()) {
            items.add(SearchResultItem.HeaderItem(context.getString(R.string.notes), results.notes.size))
            items.addAll(results.notes.map { SearchResultItem.NoteItem(it) })
        }

        Log.d("SearchResultsAdapter", "Total items: ${items.size}")
        Log.d("SearchResultsAdapter", "Appointments: ${results.appointments.size}")
        Log.d("SearchResultsAdapter", "Contacts: ${results.contacts.size}")
        Log.d("SearchResultsAdapter", "Notes: ${results.notes.size}")

        submitList(items)
    }
    inner class HeaderViewHolder(private val binding: ItemSearchHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SearchResultItem.HeaderItem) {
            binding.tvHeaderTitle.text = item.title
            binding.tvHeaderCount.text = context.getString(R.string.search_result_count, item.count)
        }
    }

    inner class AppointmentViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: AppointmentPlus) {
            binding.apply {
                tvResultTitle.text = appointment.title
                tvResultSubtitle.text = appointment.description

                // Format date and time
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = Date(appointment.startDateTime)
                tvResultTime.text = "${dateFormat.format(date)} - ${timeFormat.format(date)}"
                tvResultTime.visibility = View.VISIBLE

                // Show location if available
                if (!appointment.location.isNullOrEmpty()) {
                    tvResultDescription.text = appointment.location
                    tvResultDescription.visibility = View.VISIBLE
                } else {
                    tvResultDescription.visibility = View.GONE
                }

                // Set appointment icon
                ivResultIcon.setImageResource(R.drawable.calendar_date)

                // Handle status
                when (appointment.status) {
                    AppointmentStatus.SCHEDULED-> {
                        tvResultStatus.text = context.getString(R.string.scheduled)
                        tvResultStatus.setTextColor(binding.root.context.getColor(R.color.primary_color))
                    }

                    AppointmentStatus.IN_PROGRESS-> {
                        tvResultStatus.text = context.getString(R.string.in_progress)
                        tvResultStatus.setTextColor(binding.root.context.getColor(R.color.primary_color))
                    }

                    AppointmentStatus.COMPLETED -> {
                        tvResultStatus.text = context.getString(R.string.completed)
                        tvResultStatus.setTextColor(binding.root.context.getColor(R.color.green))
                    }

                    AppointmentStatus.CANCELLED -> {
                        tvResultStatus.text = context.getString(R.string.cancelled)
                        tvResultStatus.setTextColor(binding.root.context.getColor(R.color.red))
                    }
                    else -> {}
                }
                tvResultStatus.visibility = View.VISIBLE

                root.setOnClickListener { onAppointmentClick(appointment) }
            }
        }
    }

    inner class ContactViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            binding.apply {
                tvResultTitle.text = contact.name
                tvResultSubtitle.text = contact.phone

                // Show email if available
                if (!contact.email.isNullOrEmpty()) {
                    tvResultDescription.text = contact.email
                    tvResultDescription.visibility = View.VISIBLE
                } else {
                    tvResultDescription.visibility = View.GONE
                }

                // Set contact icon
                ivResultIcon.setImageResource(R.drawable.ic_contact)

                // Hide time and status for contacts
                tvResultTime.visibility = View.GONE
                tvResultStatus.visibility = View.GONE

                root.setOnClickListener { onContactClick(contact) }
            }
        }
    }

    inner class NoteViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.apply {
                tvResultTitle.text = note.title
                tvResultSubtitle.text = note.content.take(100) + if (note.content.length > 100) "..." else ""

                // Format creation date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = Date(note.createdAt)
                tvResultTime.text =
                    context.getString(R.string.create_note_date, dateFormat.format(date))
                tvResultTime.visibility = View.VISIBLE

                // Hide description and status for notes
                tvResultDescription.visibility = View.GONE
                tvResultStatus.visibility = View.GONE

                // Set note icon
                ivResultIcon.setImageResource(R.drawable.ic_note)

                root.setOnClickListener { onNoteClick(note) }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SearchResultItem>() {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_APPOINTMENT = 1
        private const val VIEW_TYPE_CONTACT = 2
        private const val VIEW_TYPE_NOTE = 3

        override fun areItemsTheSame(
            oldItem: SearchResultItem,
            newItem: SearchResultItem
        ): Boolean {
            return when {
                oldItem is SearchResultItem.HeaderItem && newItem is SearchResultItem.HeaderItem ->
                    oldItem.title == newItem.title

                oldItem is SearchResultItem.AppointmentItem && newItem is SearchResultItem.AppointmentItem ->
                    oldItem.appointment.id == newItem.appointment.id

                oldItem is SearchResultItem.ContactItem && newItem is SearchResultItem.ContactItem ->
                    oldItem.contact.id == newItem.contact.id

                oldItem is SearchResultItem.NoteItem && newItem is SearchResultItem.NoteItem ->
                    oldItem.note.id == newItem.note.id

                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: SearchResultItem,
            newItem: SearchResultItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
