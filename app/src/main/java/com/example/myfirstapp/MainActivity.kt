package com.example.myfirstapp

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.squareup.picasso.Picasso
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvLocationResult: TextView
    private lateinit var ivLocationImage: ImageView

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Picasso.get().load(uri).into(ivLocationImage)

            try {
                // Пытаемся вытащить реальный путь к файлу из URI
                val filePath = getRealPathFromURI(uri)
                
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        // Читаем EXIF напрямую из файла на диске
                        val exif = ExifInterface(file.absolutePath)
                        val latLong = FloatArray(2)
                        
                        if (exif.getLatLong(latLong) && (latLong[0] != 0.0f || latLong[1] != 0.0f)) {
                            val latitude = latLong[0].toDouble()
                            val longitude = latLong[1].toDouble()

                            val geocoder = Geocoder(this, Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                            if (!addresses.isNullOrEmpty()) {
                                val address: Address = addresses[0]
                                val fullAddress = address.getAddressLine(0) ?: "Адрес не определен"
                                tvLocationResult.text = "УСПЕХ НАПРЯМУЮ!\nКоординаты: $latitude, $longitude\nАдрес: $fullAddress"
                            } else {
                                tvLocationResult.text = "Координаты найдены: $latitude, $longitude, но адрес не определился."
                            }
                        } else {
                            tvLocationResult.text = "Файл найден, но EXIF-теги GPS пусты или равны нулю."
                        }
                    } else {
                        tvLocationResult.text = "Файл по пути не найден."
                    }
                } else {
                    tvLocationResult.text = "Не удалось получить прямой путь к файлу из-за защиты Android."
                }

            } catch (e: Exception) {
                tvLocationResult.text = "Ошибка: ${e.localizedMessage}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLocationResult = findViewById(R.id.tvLocationResult)
        ivLocationImage = findViewById(R.id.ivLocationImage)
        val btnTestPhoto = findViewById<Button>(R.id.btnTestPhoto)

        // Проверяем и запрашиваем разрешение на доступ ко всем файлам для старых фото
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        }

        btnTestPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                path = cursor.getString(columnIndex)
            }
        }
        return path
    }
}
