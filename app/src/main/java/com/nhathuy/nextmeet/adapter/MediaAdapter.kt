package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhathuy.nextmeet.databinding.ItemMediaBinding
import com.nhathuy.nextmeet.model.NoteImage

/**
 * Adapter hiển thị danh sách ảnh trong AddNoteActivity
 * @author TravisHuy
 * @since 03/06/2025
 */
class MediaAdapter(
    private val images: MutableList<NoteImage>,
    private val onRemovedClick : (NoteImage)  -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(noteImage: NoteImage) {
            Glide.with(itemView.context)
                .load(noteImage.imagePath)
                .centerCrop()
                .into(binding.ivMedia)
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
            
            // Getting the RecyclerView instance
            val recyclerView = getRecyclerView()
            
            // Update span count based on new size
            if (recyclerView != null) {
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager
                layoutManager?.let {
                    it.spanCount = when {
                        images.size <= 1 -> 1
                        images.size <= 4 -> 2
                        else -> 3
                    }
                }
            }
            
            // Notify adapter of data change
            notifyDataSetChanged()
            
            // Force layout update
            recyclerView?.post {
                recyclerView.requestLayout()
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
            
            // Update span count if needed
            updateSpanCount()
            
            notifyItemInserted(images.size - 1)
        }
    }

    fun addMultipleImages(newImages: List<NoteImage>) {
        val uniqueImages = newImages.filter { newImage ->
            // Kiểm tra trùng lặp dựa trên imagePath
            !images.any { existingImage -> existingImage.imagePath == newImage.imagePath }
        }

        if (uniqueImages.isNotEmpty()) {
            images.addAll(uniqueImages)
            
            // Update span count based on new size
            updateSpanCount()
            
            notifyDataSetChanged()
            
            // Force layout update on the RecyclerView
            getRecyclerView()?.post {
                getRecyclerView()?.requestLayout()
            }
        }
    }
    
    private fun updateSpanCount() {
        val recyclerView = getRecyclerView() ?: return
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
        
        layoutManager.spanCount = when {
            images.size <= 1 -> 1
            images.size <= 4 -> 2
            else -> 3
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
