package com.example.myfirstapp

import android.location.Address
import android.location.Geocoder
import android.media.MediaMetadataRetriever
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
                // Используем MediaMetadataRetriever, он надежнее читает GPS из MediaStore
                val retriever = MediaMetadataRetriever()
                
                // В новых версиях Android для чтения из URI лучше использовать этот метод
                retriever.setDataSource(this, uri)

                // Получаем GPS-строку из метаданных (формат: "+59.0123+030.0123/")
                val gpsData = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)

                if (gpsData != null) {
                    // Парсим строку, чтобы получить широту и долготу
                    // Регулярное выражение ищет числа, включая знак "+" или "-"
                    val regex = "([+-]?\\d+\\.\\d+)".toRegex()
                    val matches = regex.findAll(gpsData)
                    val coordinates = matches.map { it.value.toDouble() }.toList()

                    if (coordinates.size >= 2) {
                        val latitude = coordinates[0]
                        val longitude = coordinates[1]

                        // Превращаем координаты в адрес через Geocoder
                        val geocoder = Geocoder(this, Locale.getDefault())
                        // В старых версиях Android метод устарел, но на новых работает норм
                        @Suppress("DEPRECATION") 
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                        if (!addresses.isNullOrEmpty()) {
                            val address: Address = addresses[0]
                            val fullAddress = addresses[0].getAddressLine(0) ?: "Адрес не определен"
                            val city = address.locality ?: address.adminArea ?: ""
                            val street = address.thoroughfare ?: ""
                            val house = address.subThoroughfare ?: ""
                            
                            // Формируем аккуратный адрес для отображения
                            val prettyAddress = listOf(city, street, house).filter { it.isNotBlank() }.joinToString(", ")
                            
                            tvLocationResult.text = """
                                УСПЕХ!
                                Координаты: $latitude, $longitude
                                Город/Улица: $prettyAddress
                                Полный адрес: $fullAddress
                            """.trimIndent()
                        } else {
                            tvLocationResult.text = "Координаты найдены: $latitude, $longitude, но адрес не определился."
                        }
                    } else {
                        tvLocationResult.text = "GPS данные найдены, но формат не распознан: $gpsData"
                    }
                } else {
                    tvLocationResult.text = "В метаданных файла не найдена информация о локации (METADATA_KEY_LOCATION is null)."
                }
                retriever.release()

            } catch (e: Exception) {
                tvLocationResult.text = "Ошибка чтения метаданных: ${e.localizedMessage}"
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
