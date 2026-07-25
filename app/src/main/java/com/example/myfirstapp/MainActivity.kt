package com.example.myfirstapp

import android.media.ExifInterface
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvLocationResult: TextView
    private lateinit var ivLocationImage: ImageView

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Picasso.get().load(uri).into(ivLocationImage)

            try {
                val tempFile = File(cacheDir, "temp_photo.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val exif = ExifInterface(tempFile.absolutePath)
                
                // Проверяем основные теги координат напрямую
                val lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                val latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
                val lon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                val lonRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)

                val latLongArray = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLongArray)

                tvLocationResult.text = """
                    Сырые данные EXIF:
                    Lat: $lat ($latRef)
                    Lon: $lon ($lonRef)
                    Метод getLatLong: $hasLatLong
                    Значения: ${latLongArray[0]}, ${latLongArray[1]}
                """.trimIndent()

                tempFile.delete()

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

        btnTestPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }
    }
}
