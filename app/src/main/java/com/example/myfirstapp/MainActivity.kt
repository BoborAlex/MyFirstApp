package com.example.myfirstapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.squareup.picasso.Picasso
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLocationResult: TextView
    private lateinit var ivLocationImage: ImageView
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLocationResult = findViewById(R.id.tvLocationResult)
        ivLocationImage = findViewById(R.id.ivLocationImage)
        val btnGetLocation = findViewById<Button>(R.id.btnGetLocation)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnGetLocation.setOnClickListener {
            checkLocationPermissionAndGet()
        }
    }

    private fun checkLocationPermissionAndGet() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val addressText = address.getAddressLine(0) ?: "Адрес не найден"
                        val cityName = address.locality ?: address.countryName ?: "landscape"
                        
                        tvLocationResult.text = "Я здесь:\n$addressText"
                        
                        // Загружаем картинку места из интернета по названию города/локации
                        loadPlaceImage(cityName)
                    } else {
                        tvLocationResult.text = "Широта: ${location.latitude}\nДолгота: ${location.longitude}"
                    }
                } else {
                    tvLocationResult.text = "Не удалось определить местоположение."
                }
            }
        } catch (e: SecurityException) {
            tvLocationResult.text = "Ошибка доступа к геолокации"
        }
    }

    private fun loadPlaceImage(query: String) {
        // Используем сервис Lorem Picsum для демонстрации красивых картинок по запросу
        // (в реальных проектах сюда можно подставить поиск через Unsplash API или Google Images)
        val imageUrl = "https://picsum.photos/seed/${query.hashCode()}/600/400"
        
        Picasso.get()
            .load(imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery) // картинка-заглушка во время загрузки
            .error(android.R.drawable.ic_dialog_alert) // если не удалось загрузить
            .into(ivLocationImage)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            } else {
                tvLocationResult.text = "Нужно разрешение на геопозицию!"
            }
        }
    }
}
