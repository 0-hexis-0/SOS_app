package com.example.sos

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.geometry.Point
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.yandex.mapkit.Animation
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import okhttp3.*
import java.io.IOException
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {
    lateinit var mapview: MapView
    lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapObjects: MapObjectCollection
    private lateinit var userMarker: PlacemarkMapObject
    private lateinit var phoneNumberEditText: EditText
    private lateinit var sosButton: Button
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TELEGRAM_BOT_TOKEN = "7413186571:AAGliCfbCYIVYWdPGaVjIcbEh8JJ2jZ49Ks"
    private val CHAT_ID = "711136853"
    private var isPhoneNumberInputVisible = false
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("ApiKeyYandex")
        MapKitFactory.initialize(this)
        setContentView(R.layout.fragment_main_window)
        mapview = findViewById(R.id.mapview)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        sosButton = findViewById(R.id.sosButton)

        mapObjects = mapview.map.mapObjects.addCollection()
        mapview.map.move(
            CameraPosition(Point(43.682770, 40.266281), 14.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 3f), null
        )
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        sosButton.setOnClickListener {
            if (isPhoneNumberInputVisible) {
                val phoneNumber = phoneNumberEditText.text.toString()
                if (userLatitude != null && userLongitude != null && isValidPhoneNumber(phoneNumber)) {
                    sendLocationToTelegramBot(userLatitude!!, userLongitude!!, phoneNumber)
                } else {
                    Toast.makeText(this, "Пожалуйста, введите корректный номер телефона", Toast.LENGTH_SHORT).show()
                }
            } else {
                phoneNumberEditText.visibility = EditText.VISIBLE
                isPhoneNumberInputVisible = true
                getCurrentLocation()
            }
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
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    val userLocation = Point(userLatitude!!, userLongitude!!)
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
                } else {
                    Toast.makeText(this, "Включите геолокацию", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Не удается определить местоположение", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun sendLocationToTelegramBot(latitude: Double, longitude: Double, phoneNumber: String) {
        val url = "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
        val client = OkHttpClient()

        val mapUrl = "https://maps.yandex.ru/1.x/?ll=$longitude,$latitude&z=14&size=450,450&l=map&pt=$longitude,$latitude,pm2rdm"
        val message = "Телефон: $phoneNumber\nКарта: $mapUrl"

        val requestBody = FormBody.Builder()
            .add("chat_id", CHAT_ID)
            .add("text", message)
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
                        // Обнуляем и скрываем поле ввода номера телефона
                        phoneNumberEditText.setText("")
                        phoneNumberEditText.visibility = EditText.GONE
                        isPhoneNumberInputVisible = false
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

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Регулярное выражение для проверки стандартного номера телефона России
        val pattern = Pattern.compile("^((\\+7|7|8)+([0-9]){10})\$")
        return pattern.matcher(phoneNumber).matches()
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
