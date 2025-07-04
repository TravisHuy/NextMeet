package com.nhathuy.nextmeet.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nhathuy.nextmeet.model.Note


fun Note.getImagePaths(): List<String> {
    return if (imagePaths.isNullOrEmpty()) {
        emptyList()
    } else {
        try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(imagePaths,type) ?:  emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

fun List<String>.toImagePathsJson(): String? {
    return if(isEmpty()) null else Gson().toJson(this)
}


fun Note.hasImage() : Boolean  = getImagePaths().isNotEmpty()

fun Note.addImagePath(imagePath: String): Note {
    val currentPaths = getImagePaths().toMutableList()
    currentPaths.add(imagePath)
    return copy(imagePaths = currentPaths.toImagePathsJson())
}

fun Note.removeImagePath(imagePath: String): Note {
    val currentPaths = getImagePaths().toMutableList()
    currentPaths.remove(imagePath)
    return copy(imagePaths = currentPaths.toImagePathsJson())
}
