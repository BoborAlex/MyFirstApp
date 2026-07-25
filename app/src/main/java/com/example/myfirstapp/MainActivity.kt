package com.example.myfirstapp

import android.location.Geocoder
import android.os.Bundle
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

    private var tvLocationResult: TextView? = null
    private var ivLocationImage: ImageView? = null

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                ivLocationImage?.let { Picasso.get().load(uri).into(it) }
            } catch (e: Exception) {
                // Игнорируем ошибки загрузки картинки
            }

            try {
                val tempFile = File(cacheDir, "temp_photo.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val exif = ExifInterface(tempFile.absolutePath)
                val latLong = FloatArray(2)
                val hasLatLng = try { exif.getLatLong(latLong) } catch (e: Exception) { false }

                if (hasLatLng && (latLong[0] != 0.0f || latLong[1] != 0.0f)) {
                    val latitude = latLong[0].toDouble()
                    val longitude = latLong[1].toDouble()

                    var fullAddress = "Адрес не определен"
                    try {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            fullAddress = addresses[0].getAddressLine(0) ?: "Адрес не определен"
                        }
                    } catch (e: Exception) {
                        fullAddress = "Координаты: $latitude, $longitude (ошибка геокодера)"
                    }

                    tvLocationResult?.text = "УСПЕХ!\nКоординаты: $latitude, $longitude\nАдрес: $fullAddress"
                } else {
                    tvLocationResult?.text = "В этом файле EXIF-координаты отсутствуют или занулены системой."
                }

                if (tempFile.exists()) {
                    tempFile.delete()
                }

            } catch (e: Exception) {
                tvLocationResult?.text = "Ошибка обработки: ${e.localizedMessage}"
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
            try {
                pickPhotoLauncher.launch("image/*")
            } catch (e: Exception) {
                tvLocationResult?.text = "Ошибка запуска галереи: ${e.localizedMessage}"
            }
        }
    }
}
