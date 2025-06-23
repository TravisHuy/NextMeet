package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemContactLayoutBinding
import com.nhathuy.nextmeet.model.Contact

class ContactsAdapter(
    private val contacts: MutableList<Contact>,
    private val onContactClick: (Contact) -> Unit,
    private val onContactLongClick: (Contact, Int) -> Unit,
    private val onContactFavorite: (Contact) -> Unit,
    private val onContactPhone: (Contact) -> Unit,
    private val onContactAppointment: (Contact) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = { }
) : RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {

    private var multiSelectMode = false
    private val selectedContacts = mutableSetOf<Int>()

    fun setMultiSelectMode(enabled: Boolean) {
        multiSelectMode = enabled
        if (!enabled) {
            selectedContacts.clear()
            onSelectionChanged(0)
        }
        notifyDataSetChanged()
    }

    fun isMultiSelectMode(): Boolean = multiSelectMode

    fun isSelected(contactId: Int): Boolean = selectedContacts.contains(contactId)

    fun toggleSelection(contactId: Int) {
        if (selectedContacts.contains(contactId)) {
            selectedContacts.remove(contactId)
        } else {
            selectedContacts.add(contactId)
        }
        onSelectionChanged(selectedContacts.size)

        // Chỉ notify item thay đổi để tối ưu hiệu suất
        val position = contacts.indexOfFirst { it.id == contactId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun getSelectedContacts(): List<Contact> {
        return contacts.filter { selectedContacts.contains(it.id) }
    }

    fun getSelectedCount(): Int = selectedContacts.size

    fun clearSelection() {
        selectedContacts.clear()
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedContacts.clear()
        selectedContacts.addAll(contacts.map { it.id })
        onSelectionChanged(selectedContacts.size)
        notifyDataSetChanged()
    }

    inner class ContactsViewHolder(val binding: ItemContactLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.contactName.text = contact.name
            binding.contactPhone.text = contact.phone
            binding.contactRole.text = contact.role

            // Hiển thị sort name
            val initials = getInitials(contact.name)
            binding.nameShort.text = initials

            // Thay đổi màu favorite khi favorite và unfavorite
            if (contact.isFavorite) {
                binding.ivFavorite.setImageResource(R.drawable.ic_favorite_heart)
            } else {
                binding.ivFavorite.setImageResource(R.drawable.ic_favorite_gray)
            }

            binding.ivFavorite.visibility = View.VISIBLE

            // Set favorite click listener
            binding.ivFavorite.setOnClickListener {
                onContactFavorite(contact)
            }

            // Set button click listeners
            binding.btnCall.setOnClickListener {
                onContactPhone(contact)
            }

            binding.btnAppointment.setOnClickListener {
                onContactAppointment(contact)
            }


            if (multiSelectMode) {
                // Cập nhật trạng thái checkbox
                val isSelected = selectedContacts.contains(contact.id)
                if(isSelected){
                    binding.contactCard.apply {
                        strokeWidth = 6
                        strokeColor = ContextCompat.getColor(itemView.context, R.color.selection_border_color)
                        alpha = 0.8f
                    }
                }
                else{
                    binding.contactCard.apply{
                        strokeWidth = 1
                        strokeColor = ContextCompat.getColor(itemView.context, R.color.gray_light)
                        alpha = 1.0f
                    }
                }

            } else {
                binding.contactCard.apply{
                    strokeWidth = 1
                    strokeColor = ContextCompat.getColor(itemView.context, R.color.gray_light)
                    alpha = 1.0f
                }
            }
        }

        private fun getInitials(contactName: String): String {
            return contactName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifEmpty { "?" }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactsAdapter.ContactsViewHolder {
        val binding = ItemContactLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ContactsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ContactsAdapter.ContactsViewHolder,
        position: Int
    ) {
        val contact = contacts[position]
        holder.bind(contact)

        // Xóa listener cũ để tránh conflict
        holder.binding.root.setOnLongClickListener(null)
        holder.binding.root.setOnClickListener(null)

        // Set long click listener
        holder.binding.root.setOnLongClickListener {
            if (!multiSelectMode) {
                setMultiSelectMode(true)
            }
            toggleSelection(contact.id)
            onContactLongClick(contact, position)
            true
        }

        // Set click listener
        holder.binding.root.setOnClickListener {
            if (multiSelectMode) {
                toggleSelection(contact.id)
            } else {
                onContactClick(contact)
            }
        }
    }

    fun updateContacts(newContacts: List<Contact>) {
        val diffResult = DiffUtil.calculateDiff(
            ContactDiffCallback(newContacts, contacts)
        )
        contacts.clear()
        contacts.addAll(newContacts)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = contacts.size

    private class ContactDiffCallback(
        private val newContacts: List<Contact>,
        private val oldContacts: List<Contact>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldContacts.size

        override fun getNewListSize(): Int = newContacts.size

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            return oldContacts[oldItemPosition].id == newContacts[newItemPosition].id
        }

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            val oldContact = oldContacts[oldItemPosition]
            val newContact = newContacts[newItemPosition]

            return oldContact.name == newContact.name &&
                    oldContact.phone == newContact.phone &&
                    oldContact.address == newContact.address &&
                    oldContact.email == newContact.email &&
                    oldContact.role == newContact.role &&
                    oldContact.isFavorite == newContact.isFavorite &&
                    oldContact.updateAt == newContact.updateAt
        }
    }
}