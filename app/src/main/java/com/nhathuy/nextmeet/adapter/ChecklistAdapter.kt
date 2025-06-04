package com.nhathuy.nextmeet.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemChecklistBinding
import com.nhathuy.nextmeet.model.ChecklistItem

class ChecklistAdapter(
    private val items: MutableList<ChecklistItem>,
    private val onItemChanged: (() -> Unit)? = null,
    private val onRequestFocus: ((position: Int) -> Unit)? = null,
    private val isPreviewMode: Boolean = false
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(val binding: ItemChecklistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChecklistItem) {
            binding.etCheckItem.setText(item.text)
            binding.etCheckItem.paint.isStrikeThruText = item.isChecked
            binding.ivCheck.setImageResource(if (item.isChecked) R.drawable.ic_check else R.drawable.ic_check_box_outline)
            binding.ivCheck.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    if (item.isChecked) R.color.green else R.color.gray
                )
            )

            if (isPreviewMode) {
                binding.ivDelete.visibility = View.GONE
                binding.ivCheck.isEnabled = false
                binding.etCheckItem.isEnabled = false
                binding.etCheckItem.clearFocus()
            } else {
                binding.ivDelete.visibility = View.VISIBLE
                binding.ivCheck.isEnabled = true
                binding.etCheckItem.isEnabled = true
            }

            // Focus vào EditText nếu là item cuối cùng (mới thêm)
            if (adapterPosition == items.size - 1 && shouldRequestFocus && !isPreviewMode) {
                binding.etCheckItem.requestFocus()
                shouldRequestFocus = false
            }

            binding.ivCheck.setOnClickListener {
                if (!isPreviewMode) {
                    item.isChecked = !item.isChecked
                    notifyItemChanged(position)
                    onItemChanged?.invoke()
                }
            }

            binding.ivDelete.setOnClickListener {
                if (!isPreviewMode) {
                    items.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, items.size)
                    onItemChanged?.invoke()
                }
            }

            binding.etCheckItem.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (!isPreviewMode) {
                        item.text = s?.toString() ?: ""
                        onItemChanged?.invoke()
                    }
                }

                override fun afterTextChanged(p0: Editable?) {}
            })
        }
    }

    private var shouldRequestFocus = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChecklistAdapter.ChecklistViewHolder {
        val binding =
            ItemChecklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChecklistViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ChecklistAdapter.ChecklistViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addItem() {
        items.add(ChecklistItem("", false))
        notifyItemInserted(items.size - 1)
        shouldRequestFocus = true
        onItemChanged?.invoke()
        onRequestFocus?.invoke(items.size - 1)
    }

    fun getItems(): List<ChecklistItem> {
        return items.filter { it.text.isNotBlank() }
    }
}

