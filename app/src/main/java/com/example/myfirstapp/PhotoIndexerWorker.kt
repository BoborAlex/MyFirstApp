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

        val newPhotos = mutableListOf<PhotoEntity>()
        
        // Забираем вообще все картинки из галереи без ограничений по дате
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )

        applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(pathColumn) ?: continue
                val dateAdded = cursor.getLong(dateColumn) * 1000

                // Читаем EXIF-данные напрямую из файла фотографии на диске
                try {
                    val file = File(filePath)
                    if (file.exists()) {
                        val exif = ExifInterface(file.absolutePath)
                        val latLong = FloatArray(2)
                        
                        if (exif.getLatLong(latLong)) {
                            val latitude = latLong[0].toDouble()
                            val longitude = latLong[1].toDouble()

                            if (latitude != 0.0 || longitude != 0.0) {
                                newPhotos.add(
                                    PhotoEntity(
                                                filePath = filePath,
                                        latitude = latitude,
                                        longitude = longitude,
                                        dateAdded = dateAdded
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Пропускаем поврежденные файлы
                }
            }
        }

        // Сохраняем найденное в базу
        if (newPhotos.isNotEmpty()) {
            dao.insertAll(newPhotos)
        }

        return Result.success()
    }
}
