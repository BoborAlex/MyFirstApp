package com.example.myfirstapp

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.provider.MediaStore
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
                var latitude = 0.0
                var longitude = 0.0

                // Запрашиваем координаты напрямую из системной базы MediaStore по Uri выбранного фото
                val projection = arrayOf(
                    MediaStore.Images.Media.LATITUDE,
                    MediaStore.Images.Media.LONGITUDE
                )

                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val latCol = cursor.getColumnIndex(MediaStore.Images.Media.LATITUDE)
                    val lonCol = cursor.getColumnIndex(MediaStore.Images.Media.LONGITUDE)

                    if (cursor.moveToFirst() && latCol != -1 && lonCol != -1) {
                        latitude = cursor.getDouble(latCol)
                        longitude = cursor.getDouble(lonCol)
                    }
                }

                if (latitude != 0.0 || longitude != 0.0) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val address: Address = addresses[0]
                        val fullAddress = address.getAddressLine(0) ?: "Адрес не определен"
                        tvLocationResult.text = "УСПЕХ из MediaStore!\nКоординаты: $latitude, $longitude\nАдрес: $fullAddress"
                    } else {
                        tvLocationResult.text = "Координаты из MediaStore: $latitude, $longitude, но адрес не определился."
                    }
                } else {
                    tvLocationResult.text = "В системной базе MediaStore для этого фото нет координат (нуки)."
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

        btnTestPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }
    }
}
