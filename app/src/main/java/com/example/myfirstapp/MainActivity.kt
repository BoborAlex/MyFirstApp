package com.example.myfirstapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
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
import java.io.File
import java.util.concurrent.TimeUnit

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
                    // Ищем фото в радиусе 10 км с сортировкой по близости
                    findClosestPhotoInRange(location.latitude, location.longitude, 10.0)
                } else {
                    tvLocationResult.text = "Не удалось определить местоположение."
                }
            }
        } catch (e: SecurityException) {
            tvLocationResult.text = "Ошибка доступа к геопозиции"
        }
    }

    private fun findClosestPhotoInRange(currentLat: Double, currentLon: Double, radiusKm: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(applicationContext).photoDao()

            // 1 градус ~ 111 км. Считаем дельту для радиуса (например, 10 км ~ 0.1 градуса)
            val delta = radiusKm / 111.0
            val photos = dao.getPhotosInBox(
                currentLat - delta, currentLat + delta,
                currentLon - delta, currentLon + delta
            )

            // Считаем расстояние для каждой найденной фотографии
            val photosWithDistance = photos.mapNotNull { photo ->
                val results = FloatArray(1)
                Location.distanceBetween(currentLat, currentLon, photo.latitude, photo.longitude, results)
                val distanceMeters = results[0]

                if (distanceMeters <= radiusKm * 1000.0) {
                    Pair(photo, distanceMeters)
                } else {
                    null
                }
            }.sortedBy { it.second } // Сортируем по возрастанию расстояния (от самых близких к дальним)

            withContext(Dispatchers.Main) {
                if (photosWithDistance.isNotEmpty()) {
                    val (closestPhoto, distanceMeters) = photosWithDistance.first()
                    
                    val distanceText = if (distanceMeters < 1000) {
                        "${distanceMeters.toInt()} м"
                    } else {
                        String.format("%.1f км", distanceMeters / 1000.0)
                    }

                    tvLocationResult.text = "Ближайшее фото найдено!\nРасстояние: $distanceText"

                    // Выводим саму найденную фотографию из памяти телефона в ImageView
                    val imageFile = File(closestPhoto.filePath)
                    if (imageFile.exists()) {
                        Picasso.get()
                            .load(imageFile)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_dialog_alert)
                            .into(ivLocationImage)
                    }
                } else {
                    tvLocationResult.text = "В радиусе ${radiusKm.toInt()} км нет фото с геотегами.\n(Убедитесь, что на камере включено сохранение геопозиции)"
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
