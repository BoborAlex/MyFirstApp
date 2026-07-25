package com.example.myfirstapp

import android.content.Context
import android.media.ExifInterface
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class PhotoIndexerWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.photoDao()
        val lastDate = dao.getLastIndexedDate() ?: 0L

        val newPhotos = mutableListOf<PhotoEntity>()
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val selectionArgs = arrayOf((lastDate / 1000).toString())

        applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} ASC"
        )?.use { cursor ->
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(pathColumn)
                val dateAdded = cursor.getLong(dateColumn) * 1000

                // Достаем EXIF координаты
                try {
                    val exif = ExifInterface(filePath)
                    val latLong = FloatArray(2)
                    if (exif.getLatLong(latLong)) {
                        newPhotos.add(
                            PhotoEntity(
                                filePath = filePath,
                                latitude = latLong[0].toDouble(),
                                longitude = latLong[1].toDouble(),
                                dateAdded = dateAdded
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Файл поврежден или недоступен
                }
            }
        }

        if (newPhotos.isNotEmpty()) {
            dao.insertAll(newPhotos)
        }

        return Result.success()
    }
}
