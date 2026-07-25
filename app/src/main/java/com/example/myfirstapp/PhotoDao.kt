package com.example.myfirstapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Query("SELECT MAX(dateAdded) FROM photos")
    suspend fun getLastIndexedDate(): Long?

    // Быстрый поиск в радиусе (приблизительный прямоугольник для скорости, затем точный расчет)
    @Query("SELECT * FROM photos WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon")
    suspend fun getPhotosInBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<PhotoEntity>
}
