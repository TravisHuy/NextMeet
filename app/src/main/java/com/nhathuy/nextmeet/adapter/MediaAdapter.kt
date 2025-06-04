package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemMediaBinding
import com.nhathuy.nextmeet.model.NoteImage

/**
 * Adapter hiển thị danh sách ảnh trong AddNoteActivity (giống Google Keep)
 * @author TravisHuy
 * @since 03/06/2025
 */
class MediaAdapter(
    private val images: MutableList<NoteImage>,
    private val onRemovedClick : (NoteImage) -> Unit,
    private val isEditMode: Boolean = true // Mặc định là true (chế độ chỉnh sửa - hiển thị nút xóa)
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(noteImage: NoteImage) {
            Glide.with(itemView.context)
                .load(noteImage.imagePath)
                .centerCrop()
                .into(binding.ivMedia)

            // Chỉ hiển thị nút xóa trong chế độ chỉnh sửa
            binding.btnRemoveImage.visibility = if (isEditMode) View.VISIBLE else View.GONE

            binding.btnRemoveImage.setOnClickListener {
                onRemovedClick(noteImage)
            }
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MediaViewHolder,
        position: Int
    ) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    fun removeImage(noteImage: NoteImage){
        val index = images.indexOf(noteImage)
        if(index != -1){
            images.removeAt(index)
            notifyItemRemoved(index)
            // Notify range changed to update positions
            if (index < images.size) {
                notifyItemRangeChanged(index, images.size - index)
            }
        }
    }

    // Helper method to get the RecyclerView this adapter is attached to
    private fun getRecyclerView(): RecyclerView? {
        return try {
            val field = RecyclerView.Adapter::class.java.getDeclaredField("mRecyclerView")
            field.isAccessible = true
            field.get(this) as? RecyclerView
        } catch (e: Exception) {
            null
        }
    }

    fun addImages(noteImage: NoteImage){
        // Kiểm tra xem ảnh đã tồn tại chưa
        if (!images.any { it.imagePath == noteImage.imagePath }) {
            images.add(noteImage)
            notifyItemInserted(images.size - 1)
        }
    }

    fun addMultipleImages(newImages: List<NoteImage>) {
        val uniqueImages = newImages.filter { newImage ->
            !images.any { existingImage -> existingImage.imagePath == newImage.imagePath }
        }

        if (uniqueImages.isNotEmpty()) {
            val startPosition = images.size
            images.addAll(uniqueImages)
            notifyItemRangeInserted(startPosition, uniqueImages.size)
        }
    }

    fun setImages(newImages : List<NoteImage>){
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    fun replaceAllImages(newImages: List<NoteImage>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }
}
