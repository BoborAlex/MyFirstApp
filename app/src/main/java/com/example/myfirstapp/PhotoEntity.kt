package com.example.myfirstapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val filePath: String,
    val latitude: Double,
    val longitude: Double,
    val dateAdded: Long
)
