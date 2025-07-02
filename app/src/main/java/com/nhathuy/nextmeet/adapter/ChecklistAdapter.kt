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
    private val isPreviewMode: Boolean = false,
    private val onNoteClick: (() -> Unit)? = null
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(val binding: ItemChecklistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChecklistItem) {
            // Set text và strikethrough
            binding.etCheckItem.setText(item.text)
            binding.etCheckItem.paint.isStrikeThruText = item.isChecked

            // Set checkbox icon và color
            binding.ivCheck.setImageResource(
                if (item.isChecked) R.drawable.ic_check else R.drawable.ic_check_box_outline
            )
            binding.ivCheck.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    if (item.isChecked) R.color.green else R.color.gray
                )
            )

            if (isPreviewMode) {
                // GOOGLE KEEP STYLE: Preview mode - chỉ hiển thị, không cho tương tác
                setupPreviewMode()
            } else {
                // Edit mode - cho phép tương tác đầy đủ
                setupEditMode(item)
            }
        }

        private fun setupPreviewMode() {
            with(binding) {
                // Ẩn nút delete
                ivDelete.visibility = View.GONE

                // Vô hiệu hóa tất cả tương tác
                ivCheck.isEnabled = false
                ivCheck.isClickable = false
                ivCheck.isFocusable = false

                etCheckItem.isEnabled = false
//                etCheckItem.isClickable = false
                etCheckItem.isFocusable = false
                etCheckItem.clearFocus()


                root.setOnClickListener {
                    onNoteClick?.invoke()
                }

                etCheckItem.setOnClickListener {
                    onNoteClick?.invoke()
                }

                ivCheck.setOnClickListener {
                    onNoteClick?.invoke()
                }

                // Làm cho toàn bộ item không thể focus
                root.isFocusable = true
                root.isClickable = true

                // Set text color nhạt hơn để thể hiện trạng thái preview
                etCheckItem.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        if (items[adapterPosition].isChecked) R.color.gray else R.color.black
                    )
                )
            }
        }

        private fun setupEditMode(item: ChecklistItem) {
            with(binding) {
                // Hiển thị nút delete
                ivDelete.visibility = View.VISIBLE

                // Kích hoạt tương tác
                ivCheck.isEnabled = true
                ivCheck.isClickable = true
                etCheckItem.isEnabled = true
                etCheckItem.isClickable = true


                root.setOnClickListener(null)
                etCheckItem.setOnClickListener(null)

                // Focus vào EditText nếu là item cuối cùng (mới thêm)
                if (adapterPosition == items.size - 1 && shouldRequestFocus) {
                    etCheckItem.requestFocus()
                    shouldRequestFocus = false
                }

                // Set click listeners
                ivCheck.setOnClickListener {
                    item.isChecked = !item.isChecked
                    notifyItemChanged(adapterPosition)
                    onItemChanged?.invoke()
                }

                ivDelete.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        items.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, items.size)
                        onItemChanged?.invoke()
                    }
                }

                // Text change listener
                etCheckItem.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        item.text = s?.toString() ?: ""
                        onItemChanged?.invoke()
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        }
    }

    private var shouldRequestFocus = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val binding = ItemChecklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChecklistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addItem() {
        if (!isPreviewMode) {
            items.add(ChecklistItem("", false))
            notifyItemInserted(items.size - 1)
            shouldRequestFocus = true
            onItemChanged?.invoke()
            onRequestFocus?.invoke(items.size - 1)
        }
    }

    fun getItems(): List<ChecklistItem> {
        return items.filter { it.text.isNotBlank() }
    }
}