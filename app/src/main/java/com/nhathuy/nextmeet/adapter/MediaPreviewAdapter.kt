package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemMediaPreviewBinding
import com.nhathuy.nextmeet.model.NoteImage

class MediaPreviewAdapter(
    private var images: List<NoteImage>,
    private val totalImageCount: Int = 0,
    private val onImageClick: (NoteImage, Int) -> Unit
) : RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: ItemMediaPreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(image: NoteImage, position: Int) {
            with(binding) {
                Glide.with(ivMediaPreview.context)
                    .load(image.imagePath)
                    .centerCrop()
                    .placeholder(R.drawable.ic_photo)
                    .into(ivMediaPreview)

                val isLastItem = position == images.size - 1
                val hasMoreImages = totalImageCount > images.size

                if (isLastItem && hasMoreImages) {
                    val remainingCount = totalImageCount - images.size
                    overlayMore.visibility = View.VISIBLE
                    tvMoreCount.visibility = View.VISIBLE
                    tvMoreCount.text = "+$remainingCount"
                } else {
                    overlayMore.visibility = View.GONE
                    tvMoreCount.visibility = View.GONE
                }

                root.setOnClickListener {
                    onImageClick(image, position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaPreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size

    fun updateImages(newImages: List<NoteImage>) {
        images = newImages
        notifyDataSetChanged()
    }
}