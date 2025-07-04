package com.nhathuy.nextmeet.model

/**
 * Simple data class to represent an image item with just the path
 * Used for MediaAdapter instead of the complex NoteImage entity
 * 
 * @property imagePath The path to the image file
 * 
 * @author TravisHuy
 * @since 2025
 */
data class ImageItem(
    val imagePath: String
)