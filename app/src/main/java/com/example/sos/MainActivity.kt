package com.example.sos

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.widget.Toast
import android.widget.Button
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.geometry.Point
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import androidx.core.content.ContextCompat
import com.yandex.mapkit.Animation
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import okhttp3.*
import java.io.IOException



class MainActivity : ComponentActivity() {
    lateinit var mapview: MapView
    lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapObjects: MapObjectCollection
    private lateinit var userMarker: PlacemarkMapObject
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TELEGRAM_BOT_TOKEN = "7413186571:AAGliCfbCYIVYWdPGaVjIcbEh8JJ2jZ49Ks"
    private val CHAT_ID = "711136853"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("ApiKeyYandex")
        MapKitFactory.initialize(this)
        setContentView(R.layout.fragment_main_window)
        mapview = findViewById(R.id.mapview)
        mapObjects = mapview.map.mapObjects.addCollection()
        mapview.map.move(
            CameraPosition(Point(43.682770, 40.266281), 14.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 3f), null
        )
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val sosButton = findViewById<Button>(R.id.sosButton)
        sosButton.setOnClickListener {
            getCurrentLocation()
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    this, "В разрешении на определение местоположения отказано", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener(this) { task: Task<android.location.Location?> ->
            if (task.isSuccessful) {
                val location = task.result
                if (location != null) {
                    val userLocation = Point(location.latitude, location.longitude)
                    mapview.map.move(
                        CameraPosition(userLocation, 14.0f, 0.0f, 0.0f)
                    )

                    // Добавление маркера на карте
                    if (::userMarker.isInitialized) {
                        mapObjects.remove(userMarker)
                    }
                    userMarker = mapObjects.addPlacemark(userLocation)
                    userMarker.setIcon(ImageProvider.fromResource(this, R.drawable.user_location))

                    Toast.makeText(
                        this, "Ваша позиция: (${location.latitude}, ${location.longitude})", Toast.LENGTH_LONG
                    ).show()
                    sendLocationToTelegramBot(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Включите геолокацию", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Не удается определить местоположение", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun sendLocationToTelegramBot(latitude: Double, longitude: Double) {
        val url = "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendLocation"
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("chat_id", CHAT_ID)
            .add("latitude", latitude.toString())
            .add("longitude", longitude.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity, "Не удалось отправить местоположение", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity, "Местоположение отправлено, ожидайте", Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity, "Не удалось отправить местоположение.", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    override fun onStop() {
        mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapview.onStart()
    }

}



