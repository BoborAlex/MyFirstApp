package com.example.myfirstapp

import android.location.Address
import android.location.Geocoder
import android.media.ExifInterface
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvLocationResult: TextView
    private lateinit var ivLocationImage: ImageView

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Picasso.get().load(uri).into(ivLocationImage)

            try {
                // Читаем EXIF напрямую из системного потока Uri (без использования путей к файлам)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    val latLong = FloatArray(2)

                    if (exif.getLatLong(latLong)) {
                        val latitude = latLong[0].toDouble()
                        val longitude = latLong[1].toDouble()

                        val geocoder = Geocoder(this, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                        if (!addresses.isNullOrEmpty()) {
                            val address: Address = addresses[0]
                            val fullAddress = address.getAddressLine(0) ?: "Адрес не определен"
                            tvLocationResult.text = "УСПЕХ!\nКоординаты: $latitude, $longitude\nАдрес: $fullAddress"
                        } else {
                            tvLocationResult.text = "Координаты найдены: $latitude, $longitude, но адрес не определился."
                        }
                    } else {
                        tvLocationResult.text = "В выбранном фото нет GPS-меток (EXIF пуст)."
                    }
                }
            } catch (e: Exception) {
                tvLocationResult.text = "Ошибка чтения: ${e.localizedMessage}"
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
