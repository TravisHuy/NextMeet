package com.nhathuy.nextmeet.utils

/**
 * Utility functions for handling image paths in Note entity
 * 
 * @author TravisHuy
 * @since 2025
 */
object ImagePathUtils {
    
    private const val PATH_DELIMITER = "||"
    
    /**
     * Convert List<String> of image paths to a delimited string for storage
     */
    fun imagePathsToString(imagePaths: List<String>): String? {
        return if (imagePaths.isEmpty()) {
            null
        } else {
            imagePaths.joinToString(PATH_DELIMITER)
        }
    }
    
    /**
     * Parse delimited string back to List<String> of image paths
     */
    fun parseImagePaths(imagePathsString: String?): List<String> {
        return if (imagePathsString.isNullOrBlank()) {
            emptyList()
        } else {
            imagePathsString.split(PATH_DELIMITER)
                .filter { it.isNotBlank() }
        }
    }
}