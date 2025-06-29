package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemPhotoBinding
import com.nhathuy.nextmeet.model.Photo

class PhotoAdapter(
    private val onPhotoClick: (Photo) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private var photos = mutableListOf<Photo>()
    private var selectedPhotos = mutableListOf<Photo>()

    fun updatePhotos(newPhotos: List<Photo>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    fun updateSelectedPhotos(selected: List<Photo>) {
        selectedPhotos.clear()
        selectedPhotos.addAll(selected)
        notifyDataSetChanged()
    }
    fun getCurrentPhotos(): List<Photo> = photos.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount() = photos.size

    inner class PhotoViewHolder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Photo) {
            Glide.with(itemView.context)
                .load(photo.uri)
                .centerCrop()
                .placeholder(R.drawable.ic_photo)
                .error(R.drawable.ic_photo)
                .into(binding.imageView)

            val isSelected = selectedPhotos.any { it.id == photo.id }
            binding.checkBox.isChecked = isSelected
            binding.overlayView.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onPhotoClick(photo)
            }

            binding.checkBox.setOnClickListener {
                onPhotoClick(photo)
            }
        }
    }
}