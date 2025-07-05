package com.nhathuy.nextmeet.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.nhathuy.nextmeet.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreHelper {

    companion object {
        suspend  fun getImagesFromGallery(context: Context): List<Photo> = withContext(Dispatchers.IO){
            val photos = mutableListOf<Photo>()
            val contentResolver: ContentResolver = context.contentResolver

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE
            )

            val selector  = "${MediaStore.Images.Media.SIZE} > 0"
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            try {
                val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )
                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val mimeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val name = it.getString(nameColumn) ?: "Unknown"
                        val date = it.getLong(dateColumn)
                        val size = it.getLong(sizeColumn)
                        val mimeType = it.getString(mimeColumn) ?: "image/*"

                        if(size > 0){
                            val contentUri = Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                            photos.add(Photo(id, contentUri.toString(), name, date))
                        }
                    }
                }
            }
            catch (e: Exception){
                e.printStackTrace()
            }


            photos
        }

        suspend fun getImagesByAlbum(context: Context): Map<String, List<Photo>> = withContext(
            Dispatchers.IO) {
            val albumMap = mutableMapOf<String, MutableList<Photo>>()
            val contentResolver: ContentResolver = context.contentResolver

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.SIZE,
            )

            val selection = "${MediaStore.Images.Media.SIZE} > 0"
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            try {
                val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )

                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val bucketNameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val name = it.getString(nameColumn) ?: "Unknown"
                        val date = it.getLong(dateColumn)
                        val bucketName = it.getString(bucketNameColumn) ?: "Unknown Album"
                        val size = it.getLong(sizeColumn)

                        if (size > 0) {
                            val contentUri = Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                            val photo = Photo(id, contentUri.toString(), name, date)

                            albumMap.getOrPut(bucketName) { mutableListOf() }.add(photo)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            albumMap
        }
    }
}