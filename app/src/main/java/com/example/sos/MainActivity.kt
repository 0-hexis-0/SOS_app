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
import okhttp3.*
import java.io.IOException



class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TELEGRAM_BOT_TOKEN ="7413186571:AAGliCfbCYIVYWdPGaVjIcbEh8JJ2jZ49Ks"
    private val CHAT_ID="711136853"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("com.yandex.maps.apikey")
        MapKitFactory.initialize(this)
        setContentView(R.layout.fragment_main_window)

        mapView = findViewById(R.id.mapview)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Находим кнопку sosButton по её ID
        val sosButton = findViewById<Button>(R.id.sosButton)

        // Настраиваем обработчик нажатия на кнопку
        sosButton.setOnClickListener {
            // Вызываем метод для отправки местоположения в Telegram
            getCurrentLocation()
        }

        checkLocationPermission()
    }



    private fun checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else {

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "В разрешении на определение местоположения отказано", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener(this) { task: Task<android.location.Location?> ->
            if (task.isSuccessful) {
                val location = task.result
                if (location != null) {
                    val userLocation = Point(location.latitude, location.longitude)
                    mapView.map.move(
                        CameraPosition(userLocation, 14.0f, 0.0f, 0.0f)
                    )
                    Toast.makeText(this, "Ваша позиция: (${location.latitude}, ${location.longitude})", Toast.LENGTH_LONG).show()
                    sendLocationToTelegramBot(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Включите геолокацию", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Не удается определить местоположение", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@MainActivity, "Не удалось отправить местоположение", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Местоположение отправлено, ожидайте", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Не удалось отправить местоположение", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    override fun onStop(){
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }
}



