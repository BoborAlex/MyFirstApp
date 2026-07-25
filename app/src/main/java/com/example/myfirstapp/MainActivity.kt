package com.example.myfirstapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLocationResult: TextView
    private lateinit var ivLocationImage: ImageView
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLocationResult = findViewById(R.id.tvLocationResult)
        ivLocationImage = findViewById(R.id.ivLocationImage)
        val btnGetLocation = findViewById<Button>(R.id.btnGetLocation)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Запускаем фоновый периодический индексатор галереи
        startBackgroundIndexer()

        btnGetLocation.setOnClickListener {
            checkPermissionsAndRun()
        }
    }

    private fun startBackgroundIndexer() {
        val request = PeriodicWorkRequestBuilder<PhotoIndexerWorker>(4, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PhotoIndexerWork",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun checkPermissionsAndRun() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val readStorage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (fineLocation != PackageManager.PERMISSION_GRANTED || readStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_MEDIA_IMAGES
                    else
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            fetchLocationAndFindPhotos()
        }
    }

    private fun fetchLocationAndFindPhotos() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Показываем панораму улицы по текущим координатам
                    loadStreetView(location.latitude, location.longitude)
                    
                    // Ищем фото в радиусе 1 км в локальной базе данных
                    findNearbyPhoto(location.latitude, location.longitude)
                } else {
                    tvLocationResult.text = "Не удалось определить местоположение."
                }
            }
        } catch (e: SecurityException) {
            tvLocationResult.text = "Ошибка доступа к геопозиции"
        }
    }

    private fun loadStreetView(lat: Double, lon: Double) {
        val apiKey = "ТВОЙ_API_КЛЮЧ" // Твой ключ от Google Street View
        val url = "https://maps.googleapis.com/maps/api/streetview?size=600x300&location=$lat,$lon&key=$apiKey"
        Picasso.get().load(url).into(ivLocationImage)
    }

    private fun findNearbyPhoto(currentLat: Double, currentLon: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(applicationContext).photoDao()

            // Грубый расчет рамочного квадрата для 1 км (1 градус ~ 111 км, значит 1 км ~ 0.01 градуса)
            val delta = 0.015
            val photos = dao.getPhotosInBox(
                currentLat - delta, currentLat + delta,
                currentLon - delta, currentLon + delta
            )

            // Точный поиск ближайшего фото в радиусе 1000 метров
            var closestPhoto: PhotoEntity? = null
            var minDistance = Float.MAX_VALUE

            for (photo in photos) {
                val results = FloatArray(1)
                Location.distanceBetween(currentLat, currentLon, photo.latitude, photo.longitude, results)
                val distanceInMeters = results[0]

                if (distanceInMeters <= 1000f && distanceInMeters < minDistance) {
                    minDistance = distanceInMeters
                    closestPhoto = photo
                }
            }

            withContext(Dispatchers.Main) {
                if (closestPhoto != null) {
                    tvLocationResult.text = "Найдено фото поблизости!\nРасстояние: ${minDistance.toInt()} м\nФайл: ${closestPhoto.filePath}"
                } else {
                    tvLocationResult.text = "В радиусе 1 км ваших фото с геотегом не найдено."
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            fetchLocationAndFindPhotos()
        } else {
            tvLocationResult.text = "Требуются все разрешения для работы локального радара!"
        }
    }
}
