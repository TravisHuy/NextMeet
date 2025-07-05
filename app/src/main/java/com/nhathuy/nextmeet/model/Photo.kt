package com.nhathuy.nextmeet.model

data class Photo(
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateAdded: Long,
    val size : Long = 0 ,
    val mimeType : String = "image/*"
){
    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass) return false
        other as Photo
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
