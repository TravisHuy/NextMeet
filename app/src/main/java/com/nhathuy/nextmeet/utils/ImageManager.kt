package com.nhathuy.nextmeet.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ImageManager @Inject constructor(private val context:Context) {

    companion object {
        private const val IMAGES_FOLDER = "note_images"
        private const val MAX_IMAGES_SIZE = "1024*1024"
        private const val JPEG_QUALITY = 85
    }

    /**
     * Copy va compress anh tu Uri vao internal storage
     * tra ve path cua file da luu
     */
    suspend fun saveImageFromUri(uri: Uri, noteId: Int):String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if(inputStream == null) return@withContext null

            // tao thu muc luu anh neu chua co
            val imagesDir = File(context.filesDir,IMAGES_FOLDER)
            if(!imagesDir.exists()){
                imagesDir.mkdirs()
            }

            // tao ten file
            val fileName = "note_${noteId}_${System.currentTimeMillis()}.jpg"
            val outputFile = File(imagesDir, fileName)

            // doc va compress anh
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if(bitmap == null) return@withContext null

            // resize neu anh qua lon
            val resizedBitmap = resizeBitmap(bitmap)

            val outputStream = FileOutputStream(outputFile)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY,outputStream)
            outputStream.close()

            // Giải phóng memory
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            bitmap.recycle()

            return@withContext outputFile.absolutePath
        }
        catch (e: Exception){
            Log.e("ImageManager", "Error saving image", e)
            return@withContext null
        }
    }

    /**
     * resize bitmap neu qua lon
     */
    private fun resizeBitmap(bitmap: Bitmap) : Bitmap {
        val maxDimension = 1920
        val width = bitmap.width
        val height = bitmap.height

        if(width <= maxDimension && height <= maxDimension){
            return bitmap
        }

        val ratio = if(width > height){
            maxDimension.toFloat() / width
        }
        else {
            maxDimension.toFloat() / height
        }

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap,newWidth, newHeight,true)
    }

    /**
     * Xoa file anh
     */
    fun deleteImage(imagePath: String) : Boolean {
        return try {
            val file = File(imagePath)
            if(file.exists()){
                file.delete()
            }
            else {
                true
            }
        }
        catch (e: Exception){
            Log.e("ImageManager","Error deleting image",e)
            false
        }
    }

    /**
     * Xoa tat ca anh cua mot note
     */
    fun deleteImagesForNote(noteId: Int){
        try {
            val imagesDir = File(context.filesDir,IMAGES_FOLDER)
            if(imagesDir.exists()){
                imagesDir.listFiles()?.forEach {
                    file ->
                    if(file.name.startsWith("note_${noteId}_")){
                        file.delete()
                    }
                }
            }

        }
        catch (e: Exception){
            Log.e("ImageManger","Error deleting images for note",e)
        }
    }


    /*
     * Lay file object tu path de load anh
     */
    fun getImageFile(imagePath: String): File? {
        return try {
            val file = File(imagePath)
            if(file.exists()) file else null
        }
        catch (e: Exception){
            null
        }
    }

    /**
     * Kiem tra file anh co ton tai khong
     */
    fun imageExists(imagePath: String):Boolean {
        return try {
            File(imagePath).exists()
        }
        catch (e: Exception){
            false
        }
    }
}